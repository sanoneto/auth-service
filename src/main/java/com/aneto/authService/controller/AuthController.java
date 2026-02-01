package com.aneto.authService.controller;

import com.aneto.authService.dto.request.LoginRequest;
import com.aneto.authService.dto.request.PasswordResetRequest;
import com.aneto.authService.dto.request.UserCredentialsRequest;
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
        // 1. Valida a password
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.username(), loginRequest.password())
        );

        Users usuario = authService.findPorUsername(loginRequest.username());

        // 2. SE MFA ATIVO: Não envia token, envia apenas o sinal "mfaRequired"
        if (Boolean.TRUE.equals(usuario.getMfaEnabled())) {
            return ResponseEntity.ok(Map.of(
                    "mfaRequired", true,
                    "username", usuario.getUsername()
            ));
        }

        // 3. Caso contrário, gera token normalmente
        String token = authService.saveToken(usuario);
        return ResponseEntity.ok(new LoginResponse("Logado", token, usuario.getRole()));
    }

    // =========================================================================
    // ENDPOINTS MFA (ADICIONADOS)
    // =========================================================================

    @Operation(summary = "Gera o QR Code para configurar o MFA")
    @GetMapping("/mfa-setup")
    public ResponseEntity<Map<String, String>> setupMfa(@RequestHeader("Authorization") String token) {
        // Remove "Bearer " se presente
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

    @PostMapping("/verify-mfa")
    public ResponseEntity<LoginResponse> verifyMfa(@RequestBody Map<String, String> request) {
        String username = request.get("username");
        String code = request.get("code");
        log.info( "eu entrei aqui ");
        // Valida o código através do serviço
        boolean isCodeValid = authService.verificarCodigoMfa(username, code);

        if (isCodeValid) {
            Users usuario = authService.findPorUsername(username);
            String token = authService.saveToken(usuario);
            return ResponseEntity.ok(new LoginResponse(
                    "Autenticação MFA concluída",
                    token,
                    usuario.getRole()
            ));
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
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
    // RESTANTE DOS ENDPOINTS MANTIDOS
    // =========================================================================

    @PostMapping("/register")
    public ResponseEntity<?> registrarUsers(@RequestBody @Valid UserCredentialsRequest request) {
        return ResponseEntity.ok(authService.registrarUsers(request));
    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyCode(@RequestBody UserCredentialsRequest request) {
        return ResponseEntity.ok(authService.verificarCodigo(request));
    }

    @PostMapping("/recuperar-password")
    public ResponseEntity<?> requestPasswordReset(@RequestBody PasswordResetRequest request) {
        authService.createPasswordResetTokenForUser(request.email());
        return ResponseEntity.ok("Instruções de recuperação enviadas.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody PasswordResetRequest request) {
        if (request.token() == null || request.newPassword() == null) {
            return ResponseEntity.badRequest().body("Dados obrigatórios ausentes.");
        }
        authService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok("Password redefinida com sucesso!");
    }

    @PutMapping("/users/{publicId}")
    public ResponseEntity<?> editarUtilizador(@PathVariable String publicId, @RequestBody @Valid UserCredentialsRequest request) {
        authService.atualizarUtilizador(publicId, request);
        return ResponseEntity.ok("Utilizador atualizado!");
    }

    @DeleteMapping("/users/{publicId}")
    public ResponseEntity<?> eliminarUtilizador(@PathVariable String publicId) {
        authService.eliminarUtilizador(publicId);
        return ResponseEntity.ok("Utilizador eliminado!");
    }

    @PostMapping("/google")
    public ResponseEntity<LoginResponse> googleLogin(@RequestBody Map<String, String> data) {
        return ResponseEntity.ok(authService.getLoginResponse(data));
    }

    @PostMapping("/facebook")
    public ResponseEntity<LoginResponse> facebookLogin(@RequestBody Map<String, String> data) {
        return ResponseEntity.ok(authService.processarLoginFacebook(data));
    }

    @PutMapping("/updateProfile")
    public ResponseEntity<?> updateProfile(@RequestParam String username, @RequestParam String publicUrl) {
        authService.UpdateProfile(username, publicUrl);
        return ResponseEntity.ok("Perfil atualizado!");
    }

    @PostMapping("/desvincular-telegram/{username}")
    public ResponseEntity<?> unlinkTelegram(@PathVariable String username) {
        authService.unlinkTelegram(username);
        return ResponseEntity.ok("Telegram desvinculado.");
    }

    @GetMapping("/telegram-id/{username}")
    public ResponseEntity<String> getTelegramChatId(@PathVariable String username) {
        String chatId = authService.obterTelegramChatId(username);
        return (chatId == null) ? ResponseEntity.noContent().build() : ResponseEntity.ok(chatId);
    }
}