package com.aneto.authService.controller;

import com.aneto.authService.dto.request.LoginRequest;
import com.aneto.authService.dto.request.ProjectRequest;
import com.aneto.authService.dto.request.UserCredentialsRequest;
import com.aneto.authService.dto.response.LoginResponse;
import com.aneto.authService.dto.response.ProjectResponse;
import com.aneto.authService.mapper.RequestMapper; // Assumido
import com.aneto.authService.models.Users; // Assumido
import com.aneto.authService.queue.EmailProducer;
import com.aneto.authService.security.JwtTokenUtil;
import com.aneto.authService.service.JwtTokenService; // Assumido
import com.aneto.authService.service.UsersService;
import com.aneto.authService.service.impl.ProjectorsServiceImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenUtil jwtTokenUtil;
    private final UsersService usersService;
    private final JwtTokenService jwtTokenService;
    private final RequestMapper requestMapper; // Assumido
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

        // 1. Autenticação via Spring Security
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.username(), loginRequest.password())
        );

        // 2. Busca o usuário para obter roles
        Users usuario = usersService.findPorUsername(loginRequest.username());

        // 3. Normaliza a role para o token (remove 'ROLE_' e usa UPPERCASE)
        String role = usuario.getRole();
        List<String> rolesForToken = (role == null || role.isBlank())
                ? List.of()
                : List.of(role.startsWith("ROLE_") ? role.substring(5) : role);

        // 4. Gera o token
        String token = jwtTokenUtil.generateToken(usuario.getUsername(), rolesForToken);

        // 5. Salva o token no banco (para revogação)
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusMillis(jwtTokenUtil.getExpirationMillis());
        jwtTokenService.saveToken(token, usuario.getUsername(), issuedAt, expiresAt);

        return ResponseEntity.ok(new LoginResponse(usuario.getUsername() + " logado com sucesso", token));
    }

    @Operation(
            summary = "Regista um novo usuário e emite um token de sessão",
            description = "Regista um novo usuário e automaticamente faz o login, emitindo um token JWT."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Usuário registrado com sucesso"),
            @ApiResponse(responseCode = "400", description = "Username já existe ou dados inválidos")
    })
    @PostMapping("/registar")
    public ResponseEntity<?> registrarUsers(@RequestBody @Valid UserCredentialsRequest userCredentialsRequest) {
        if (usersService.existeUsers(userCredentialsRequest.username())) {
            return ResponseEntity.badRequest().body("Username já existe!");
        }

        // Mapeia e registra o usuário (o service deve codificar a password)
        Users users = requestMapper.mapToLogin(userCredentialsRequest);
        usersService.registrarUsers(users);

        emailProducer.sendRegistrationEmail(userCredentialsRequest.username(), userCredentialsRequest.email());
        // Gera e salva o token para o novo usuário

        String role = users.getRole();
        List<String> rolesForToken = (role == null || role.isBlank())
                ? List.of()
                : List.of(role.startsWith("ROLE_") ? role.substring(5) : role);
        String token = jwtTokenUtil.generateToken(users.getUsername(), rolesForToken);

        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusMillis(jwtTokenUtil.getExpirationMillis());
        jwtTokenService.saveToken(token, users.getUsername(), issuedAt, expiresAt);

        return ResponseEntity.ok(new LoginResponse(users.getUsername() + " registrado com sucesso", token));
    }

}