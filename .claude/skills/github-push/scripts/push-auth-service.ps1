<#
.SYNOPSIS
    Faz commit e push do branch atual do auth-service para o GitHub (origin).

.DESCRIPTION
    Passos, por ordem (pára ao primeiro problema):
      1. Recusa fazer push a partir de 'main'.
      2. Adiciona os ficheiros indicados em -Files (ou usa o que já está em stage).
      3. Procura segredos nas linhas adicionadas e em ficheiros proibidos (.env, secretKeys.env).
      4. Compila com o JDK 21 (o projeto usa --enable-preview com release 21).
      5. Faz commit com a mensagem indicada.
      6. Faz fetch e recusa o push se o remoto tiver commits que não existem localmente.
      7. Faz push (com -u se o branch ainda não tiver upstream). Nunca usa --force.

.PARAMETER Message
    Mensagem de commit (pode ter várias linhas). Obrigatória se houver alterações para commit.

.PARAMETER Files
    Ficheiros a adicionar ao commit. Se omitido, usa apenas o que já estiver em stage.

.PARAMETER SkipBuild
    Salta a compilação (usar só se já tiver sido validada).

.EXAMPLE
    .\push-auth-service.ps1 -Message "Corrige roles" -Files src/main/java/.../AuthController.java
#>
param(
    [string]$Message,
    [string[]]$Files,
    [switch]$SkipBuild
)

$ErrorActionPreference = 'Stop'

function Fail([string]$msg) {
    Write-Host "ERRO: $msg" -ForegroundColor Red
    exit 1
}

function Invoke-Git {
    $output = & git @args 2>&1
    if ($LASTEXITCODE -ne 0) { Fail "git $($args -join ' ') falhou:`n$output" }
    return $output
}

# Raiz do repositório: .claude/skills/github-push/scripts -> 4 níveis acima
$repoRoot = Resolve-Path (Join-Path $PSScriptRoot '..\..\..\..')
Set-Location $repoRoot
Write-Host "Repositório: $repoRoot"

# --- 1. Branch ---------------------------------------------------------------
$branch = (Invoke-Git branch --show-current | Out-String).Trim()
if (-not $branch) { Fail "HEAD destacado (detached). Muda para um branch antes de fazer push." }
if ($branch -eq 'main') { Fail "Push direto para 'main' não é permitido. Usa outro branch (ex.: develop)." }
Write-Host "Branch: $branch"

# --- 2. Stage ----------------------------------------------------------------
if ($Files) {
    Invoke-Git add -- @Files | Out-Null
}

$staged = @(Invoke-Git diff --cached --name-only | Where-Object { $_ })
$hasCommitToMake = $staged.Count -gt 0

if ($hasCommitToMake) {
    Write-Host "Ficheiros em stage:"
    $staged | ForEach-Object { Write-Host "  $_" }

    # --- 3. Segredos ---------------------------------------------------------
    $forbidden = $staged | Where-Object { $_ -match '(^|/)(\.env(\..+)?|secretKeys\.env)$' }
    if ($forbidden) { Fail "Ficheiros com segredos em stage: $($forbidden -join ', '). Remove-os com 'git restore --staged'." }

    $addedLines = Invoke-Git diff --cached -U0 | Where-Object { $_ -match '^\+' -and $_ -notmatch '^\+\+\+' }
    $secretPatterns = @(
        'AKIA[0-9A-Z]{16}',                                                        # AWS access key
        '\b\d{8,10}:[A-Za-z0-9_-]{35}\b',                                         # Token de bot Telegram
        '(?i)(jwt_secret|secret[_-]?key|api[_-]?key|password|token)\s*[=:]\s*["'']?[A-Za-z0-9/+_\-]{16,}',
        '-----BEGIN [A-Z ]*PRIVATE KEY-----'
    )
    $hits = foreach ($line in $addedLines) {
        foreach ($p in $secretPatterns) { if ($line -match $p) { $line; break } }
    }
    if ($hits) {
        Write-Host "Possíveis segredos nas linhas adicionadas:" -ForegroundColor Yellow
        $hits | ForEach-Object { Write-Host "  $_" -ForegroundColor Yellow }
        Fail "Commit cancelado. Confirma que não são valores reais antes de continuar."
    }

    if (-not $Message) { Fail "Falta -Message para o commit." }
}

# --- 4. Build ----------------------------------------------------------------
if (-not $SkipBuild) {
    $jdk21 = 'C:\Program Files\Java\jdk-21'
    if (-not (Test-Path $jdk21)) { Fail "JDK 21 não encontrado em '$jdk21'." }
    $env:JAVA_HOME = $jdk21
    Write-Host "A compilar com o JDK 21..."
    & .\mvnw.cmd -q compile
    if ($LASTEXITCODE -ne 0) { Fail "A compilação falhou. Push cancelado." }
    Write-Host "Compilação OK."
}

# --- 5. Commit ---------------------------------------------------------------
if ($hasCommitToMake) {
    $msgFile = New-TemporaryFile
    try {
        Set-Content -Path $msgFile -Value $Message -Encoding utf8NoBOM
        Invoke-Git commit -F $msgFile | Out-Null
    } finally {
        Remove-Item $msgFile -ErrorAction SilentlyContinue
    }
    Write-Host "Commit criado: $((Invoke-Git log --oneline -1 | Out-String).Trim())"
}

# --- 6. Comparar com o remoto -------------------------------------------------
Invoke-Git fetch origin | Out-Null

& git rev-parse --verify --quiet "refs/remotes/origin/$branch" | Out-Null
$remoteExists = $LASTEXITCODE -eq 0

if ($remoteExists) {
    $behind = [int]((Invoke-Git rev-list --count "HEAD..origin/$branch" | Out-String).Trim())
    $ahead  = [int]((Invoke-Git rev-list --count "origin/$branch..HEAD" | Out-String).Trim())
    if ($behind -gt 0) { Fail "origin/$branch tem $behind commit(s) que não existem localmente. Faz 'git pull --rebase' primeiro." }
    if ($ahead -eq 0) { Write-Host "Nada para enviar: o branch já está atualizado com origin/$branch."; exit 0 }
}

# --- 7. Push -----------------------------------------------------------------
if ($remoteExists) {
    & git push origin $branch
} else {
    & git push -u origin $branch
}
if ($LASTEXITCODE -ne 0) {
    Fail "O push falhou. Se for um erro de autenticação, executa 'gh auth login' e tenta outra vez."
}

$head = (Invoke-Git log --oneline -1 | Out-String).Trim()
Write-Host ""
Write-Host "PUSH OK" -ForegroundColor Green
Write-Host "Branch: $branch"
Write-Host "Commit: $head"
Write-Host "Link:   https://github.com/sanoneto/auth-service/tree/$branch"
