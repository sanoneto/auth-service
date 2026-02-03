package com.aneto.authService.controller;

import com.aneto.authService.dto.request.LoginRequest;
import com.aneto.authService.dto.request.PasswordResetRequest;
import com.aneto.authService.dto.request.UserCredentialsRequest;
import com.aneto.authService.dto.request.UsersResponse;
import com.aneto.authService.dto.response.LoginResponse;
import com.aneto.authService.models.Users;
import com.aneto.authService.repository.UsersRepository;
import com.aneto.authService.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final AuthService authService;
    private final UsersRepository usersRepository;

    @Operation(summary = "Autentica um usuário")
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequest loginRequest) {
        // Realiza a autenticação via Spring Security
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.username(), loginRequest.password())
        );

        Users usuario = authService.findPorUsername(loginRequest.username());

        // Verifica se o MFA está ativo para este utilizador
        if (Boolean.TRUE.equals(usuario.getMfaEnabled())) {
            log.info("MFA requerido para o utilizador: {}", usuario.getUsername());
            return ResponseEntity.ok(Map.of(
                    "mfaRequired", true,
                    "username", usuario.getUsername()
            ));
        }

        String token = authService.saveToken(usuario);

        // O usuario.getRole() agora retorna o Enum UserRole
        return ResponseEntity.ok(new LoginResponse(
                "Logado",
                token,
                null,
                usuario.getRole(),          // Agora envia o Enum corretamente
                usuario.getAllowedModules()
        ));
    }

    @Operation(summary = "Verifica o código MFA e finaliza o login")
    @PostMapping("/verify-mfa")
    public ResponseEntity<LoginResponse> verifyMfa(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String code = request.get("code");

        boolean isCodeValid = authService.verificarCodigoMfa(username, code);

        if (isCodeValid) {
            Users usuario = authService.findPorUsername(username);
            String token = authService.saveToken(usuario);

            return ResponseEntity.ok(new LoginResponse(
                    "Logado",
                    token,
                    null,
                    usuario.getRole(),
                    usuario.getAllowedModules()
            ));
        } else {
            log.warn("Tentativa de login MFA falhou para o utilizador: {}", username);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

    @Operation(summary = "Atualiza módulos permitidos do utilizador")
    @PutMapping("/users/{publicId}/permissions")
    public ResponseEntity<?> atualizarPermissoes(
            @PathVariable String publicId,
            @RequestBody Map<String, List<String>> request) {

        List<String> modulos = request.get("allowedModules");
        authService.atualizarPermissoesUtilizador(publicId, modulos);
        return ResponseEntity.ok("Permissões atualizadas!");
    }

    // =========================================================================
    // ENDPOINTS MFA
    // =========================================================================

    @Operation(summary = "Gera o QR Code para configurar o MFA")
    @GetMapping("/mfa-setup")
    public ResponseEntity<Map<String, String>> setupMfa(@RequestHeader("Authorization") String token) {
        String jwt = token.startsWith("Bearer ") ? token.substring(7) : token;
        return ResponseEntity.ok(authService.setupMfa(jwt));
    }

    @Operation(summary = "Ativa o MFA após validar o primeiro código")
    @PostMapping("/mfa-activate")
    public ResponseEntity<?> activateMfa(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, String> body) {

        String jwt = token.startsWith("Bearer ") ? token.substring(7) : token;
        String code = body.get("code");

        authService.activateMfa(jwt, code);
        return ResponseEntity.ok("MFA ativado com sucesso!");
    }

    @Operation(summary = "Altera o status do MFA (Ligar/Desligar)")
    @PostMapping("/mfa-status")
    public ResponseEntity<?> mudarStatusMfa(
            @RequestParam String username,
            @RequestParam boolean status) {
        authService.mudarStatusMfa(username, status);
        return ResponseEntity.ok("Status do MFA atualizado.");
    }

    // =========================================================================
    // GESTÃO DE UTILIZADORES
    // =========================================================================

    @Operation(summary = "Regista um novo utilizador no sistema")
    @PostMapping("/register")
    public ResponseEntity<?> registrarUsers(@RequestBody @Valid UserCredentialsRequest request) {
        return ResponseEntity.ok(authService.registrarUsers(request));
    }

    @Operation(summary = "Verifica o código de ativação da conta")
    @PostMapping("/verify")
    public ResponseEntity<?> verifyCode(@RequestBody UserCredentialsRequest request) {
        return ResponseEntity.ok(authService.verificarCodigo(request));
    }

    @Operation(summary = "Solicita a recuperação de password via e-mail")
    @PostMapping("/recuperar-password")
    public ResponseEntity<?> requestPasswordReset(@RequestBody PasswordResetRequest request) {
        authService.createPasswordResetTokenForUser(request.email());
        return ResponseEntity.ok("Instruções de recuperação enviadas.");
    }

    @Operation(summary = "Redefine a password usando o token enviado")
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody PasswordResetRequest request) {
        if (request.token() == null || request.newPassword() == null) {
            return ResponseEntity.badRequest().body("Dados obrigatórios ausentes.");
        }
        authService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok("Password redefinida com sucesso!");
    }

    @Operation(summary = "Edita os dados de um utilizador existente")
    @PutMapping("/users/{publicId}")
    public ResponseEntity<?> editarUtilizador(@PathVariable String publicId, @RequestBody @Valid UserCredentialsRequest request) {
        authService.atualizarUtilizador(publicId, request);
        return ResponseEntity.ok("Utilizador atualizado!");
    }

    @Operation(summary = "Elimina um utilizador permanentemente")
    @DeleteMapping("/users/{publicId}")
    public ResponseEntity<?> eliminarUtilizador(@PathVariable String publicId) {
        authService.eliminarUtilizador(publicId);
        return ResponseEntity.ok("Utilizador eliminado!");
    }

    @Operation(summary = "Lista todos os utilizadores registados")
    @GetMapping("/users")
    public ResponseEntity<List<UsersResponse>> getListUsers() {
        return ResponseEntity.ok(authService.findAll());
    }

    // =========================================================================
    // INTEGRAÇÕES EXTERNAS
    // =========================================================================

    @Operation(summary = "Login/Registo via Google OAuth2")
    @PostMapping("/google")
    public ResponseEntity<LoginResponse> googleLogin(@RequestBody Map<String, String> data) {
        return ResponseEntity.ok(authService.getLoginResponse(data));
    }

    @Operation(summary = "Login/Registo via Facebook")
    @PostMapping("/facebook")
    public ResponseEntity<LoginResponse> facebookLogin(@RequestBody Map<String, String> data) {
        return ResponseEntity.ok(authService.processarLoginFacebook(data));
    }

    @Operation(summary = "Atualiza o link da foto de perfil")
    @PutMapping("/updateProfile")
    public ResponseEntity<?> updateProfile(@RequestParam String username, @RequestParam String publicUrl) {
        authService.UpdateProfile(username, publicUrl);
        return ResponseEntity.ok("Perfil atualizado!");
    }

    @Operation(summary = "Remove o vínculo da conta com o Telegram")
    @PostMapping("/desvincular-telegram/{username}")
    public ResponseEntity<?> unlinkTelegram(@PathVariable String username) {
        authService.unlinkTelegram(username);
        return ResponseEntity.ok("Telegram desvinculado.");
    }

    @Operation(summary = "Obtém o Chat ID do Telegram do utilizador")
    @GetMapping("/telegram-id/{username}")
    public ResponseEntity<String> getTelegramChatId(@PathVariable String username) {
        String chatId = authService.obterTelegramChatId(username);
        return (chatId == null) ? ResponseEntity.noContent().build() : ResponseEntity.ok(chatId);
    }
}