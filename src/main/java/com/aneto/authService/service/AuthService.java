package com.aneto.authService.service;


import com.aneto.authService.dto.request.UserCredentialsRequest;
import com.aneto.authService.dto.response.LoginResponse;
import com.aneto.authService.dto.response.RegistrationResponse;
import com.aneto.authService.models.Users;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;

public interface AuthService {

    RegistrationResponse registrarUsers(UserCredentialsRequest request);

    Users findPorUsername(String username);

    boolean existeUsers(String username);

    void createPasswordResetTokenForUser(String email);

    String saveToken(Users users);

    void resetPassword(String token, String newPassword);

    void UpdateProfile(String username, String publicUrl);

    LoginResponse verificarCodigo(UserCredentialsRequest request);
    LoginResponse processGoogleLogin(String email, String name);

    void eliminarUtilizador(String publicId);

    void atualizarUtilizador(String publicId, @Valid UserCredentialsRequest request);
}
