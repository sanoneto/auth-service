package com.aneto.authService.service;


import com.aneto.authService.dto.request.UserCredentialsRequest;
import com.aneto.authService.dto.response.LoginResponse;
import com.aneto.authService.models.Users;
import org.springframework.http.ResponseEntity;

public interface AuthService {

    String registrarUsers(UserCredentialsRequest request);

    Users findPorUsername(String username);

    boolean existeUsers(String username);

    void createPasswordResetTokenForUser(String email);

    String saveToken(Users users);

    void resetPassword(String token, String newPassword);

    void UpdateProfile(String username, String publicUrl);

    LoginResponse verificarCodigo(UserCredentialsRequest request);
}
