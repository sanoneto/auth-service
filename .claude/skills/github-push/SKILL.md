---
name: github-push
description: Faz commit das alterações e push do branch atual do auth-service para o GitHub (origin = github.com/sanoneto/auth-service). Usar quando o utilizador pedir para fazer push, enviar, publicar ou subir alterações para o GitHub.
---

# Push para o GitHub (auth-service)

Repositório: `https://github.com/sanoneto/auth-service.git` (remote `origin`).
Branch de trabalho habitual: `develop`. Branch principal: `main`.

Invocar este skill é autorização para fazer commit e push do branch atual.
O trabalho é feito pelo script `scripts/push-auth-service.ps1`; o teu papel é escolher os
ficheiros, escrever a mensagem de commit e interpretar o resultado.

## 1. Rever as alterações

```powershell
git status --short
git branch --show-current
git diff --stat
git log --oneline -5
```

- Se o branch atual for `main`, pára e pergunta ao utilizador para que branch quer enviar.
- Decide que ficheiros entram no commit. Exclui artefactos (`target/`, `logs/`, `.idea/workspace.xml`)
  e ficheiros com segredos reais (`.env`, `secretKeys.env`).
- Se houver alterações que não parecem fazer parte do pedido, pergunta antes de as incluir.

## 2. Escrever a mensagem de commit

Em português, curta e descritiva, no estilo do histórico. Termina com a linha de atribuição
indicada pelo sistema, se existir.

## 3. Executar o script

```powershell
& ".claude\skills\github-push\scripts\push-auth-service.ps1" `
    -Message @'
Corrige verificação de roles nos endpoints de utilizadores

Co-Authored-By: Claude <noreply@anthropic.com>
'@ `
    -Files src/main/java/com/aneto/authService/controller/AuthController.java, src/main/java/com/aneto/authService/security/SecurityConfig.java
```

Parâmetros:
| Parâmetro | Descrição |
|---|---|
| `-Message` | Mensagem de commit (obrigatória se houver alterações para commit). |
| `-Files` | Ficheiros a adicionar. Se omitido, usa só o que já está em stage. |
| `-SkipBuild` | Salta a compilação. Usar só se já tiver sido validada nesta sessão. |

Se já existirem commits locais por enviar e nada para commit, o script pode ser chamado sem
`-Message`/`-Files` — só faz o push.

O script:
1. Recusa push a partir de `main`.
2. Bloqueia ficheiros `.env`/`secretKeys.env` e linhas com possíveis segredos (chaves AWS,
   token do bot Telegram, `password=`, `jwt_secret=`, chaves privadas).
3. Compila com o JDK 21 (`C:\Program Files\Java\jdk-21`) — o JDK 23 por omissão falha
   por causa do `--enable-preview`.
4. Faz o commit.
5. Faz `fetch` e recusa o push se `origin/<branch>` tiver commits que não existem localmente.
6. Faz `git push` (com `-u` na primeira vez). Nunca usa `--force`.

## 4. Interpretar o resultado

- **`PUSH OK`**: indica ao utilizador o branch, o commit e o link. Se o branch não for `main`,
  sugere abrir um Pull Request (`gh pr create --base main`), mas só o cria se o utilizador pedir.
- **Possíveis segredos**: mostra as linhas ao utilizador e pergunta como proceder. Não contornes
  a verificação.
- **Compilação falhou**: mostra o erro; não faças push.
- **Remoto à frente**: pergunta se quer `git pull --rebase` antes de tentar de novo.
- **Erro de autenticação**: pede ao utilizador para executar `! gh auth login` e repete.

Nunca uses `--no-verify`, `--force` nem `--amend` em commits já enviados sem pedido explícito.

## Notas

- O `gateway-service` é um repositório git separado (`../gateway-service`). Este skill só envia o
  `auth-service`; se houver alterações no gateway, avisa o utilizador em vez de as enviar.
