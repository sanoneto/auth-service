package com.aneto.authService.controller;

import com.aneto.authService.dto.request.LoginRequest;
import com.aneto.authService.dto.request.PasswordResetRequest;
import com.aneto.authService.dto.request.UserCredentialsRequest;
import com.aneto.authService.dto.response.LoginResponse;
import com.aneto.authService.models.Users;
import com.aneto.authService.queue.EmailProducer;
import com.aneto.authService.repository.UsersRepository;
import com.aneto.authService.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final AuthService authService;
    private final UsersRepository usersRepository;
    private final EmailProducer emailProducer;

    private static final String X_USER_ID = "X-User-Id";

    @Operation(
            summary = "Autentica um usuário e emite um token JWT",
            description = "Valida credenciais e retorna um JWT válido."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Login bem-sucedido"),
            @ApiResponse(responseCode = "401", description = "Credenciais inválidas")
    })
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody @Valid LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.username(), loginRequest.password())
        );

        Users usuario = authService.findPorUsername(loginRequest.username());
        String token = authService.saveToken(usuario);

        // 🔑 Obtemos o googleToken que está guardado no perfil do usuário
        String googleToken = usuario.getGoogleToken();

        return ResponseEntity.ok(new LoginResponse(
                usuario.getUsername() + " logado com sucesso",
                token,
                googleToken // Enviado para o Frontend
        ));
    }

    @Operation(
            summary = "Regista um novo usuário e emite um token de sessão",
            description = "Regista um novo usuário e automaticamente faz o login, emitindo um token JWT."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuário registrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Username já existe ou dados inválidos")
    })
    @PostMapping("/register")
    public ResponseEntity<?> registrarUsers(@RequestBody @Valid UserCredentialsRequest request) {
        // Sem try-catch! O GlobalExceptionHandler trata tudo por trás das cenas.
        return ResponseEntity.ok(authService.registrarUsers(request));

    }

    @PostMapping("/verify")
    public ResponseEntity<?> verifyCode(@RequestBody UserCredentialsRequest request) {
        // 1. O retorno de findByEmail é Optional. Use .isPresent() ou .orElse(null)
        return ResponseEntity.ok(authService.verificarCodigo(request));
    }

    @PostMapping("/recuperar-password")
    public ResponseEntity<?> requestPasswordReset(@RequestBody PasswordResetRequest request) {
        String email = request.email();
        // Chama o serviço que faz toda a lógica
        authService.createPasswordResetTokenForUser(email);
        // Retorna uma mensagem genérica de sucesso para evitar vazamento de informações
        return ResponseEntity.ok("Instruções de recuperação enviadas com sucesso, se o email existir.");
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody PasswordResetRequest request) {
        // Validação básica do corpo da requisição (pode ser aprimorada com @Valid)
        if (request.token() == null || request.newPassword() == null) {
            return ResponseEntity.badRequest().body("Token e nova password são obrigatórios.");
        }
        authService.resetPassword(request.token(), request.newPassword());
        // Sucesso - Retorna 200 OK ou 204 No Content
        return ResponseEntity.ok("Password redefinida com sucesso!");
    }

    @Operation(
            summary = "Edita os dados de um utilizador",
            description = "Permite atualizar o username, email ou cargo de um utilizador via publicId."
    )
    @PutMapping("/users/{publicId}")
    public ResponseEntity<?> editarUtilizador(
            @PathVariable String publicId,
            @RequestBody @Valid UserCredentialsRequest request) {

        // O serviço tratará a lógica de procurar por publicId e atualizar
        authService.atualizarUtilizador(publicId, request);
        return ResponseEntity.ok("Utilizador atualizado com sucesso!");
    }

    @Operation(
            summary = "Elimina um utilizador do sistema",
            description = "Remove permanentemente o utilizador através do seu publicId."
    )
    @DeleteMapping("/users/{publicId}")
    public ResponseEntity<?> eliminarUtilizador(@PathVariable String publicId) {
        authService.eliminarUtilizador(publicId);
        return ResponseEntity.ok("Utilizador eliminado com sucesso!");
    }

    @PostMapping("/google")
    public ResponseEntity<LoginResponse> googleLogin(@RequestBody Map<String, String> data) {
        LoginResponse response = authService.getLoginResponse(data);

        return ResponseEntity.ok(response);
    }


    @PutMapping("/updateProfile")
    public ResponseEntity<?> UpdateProfile(@RequestParam String username, @RequestParam String publicUrl) {
        // Validação básica do corpo da requisição (pode ser aprimorada com @Valid)
        authService.UpdateProfile(username, publicUrl);
        // Sucesso - Retorna 200 OK ou 204 No Content
        return ResponseEntity.ok("Url adiciona com sucesso!");
    }
}