package com.aneto.authService.service;


import com.aneto.authService.dto.request.UserCredentialsRequest;
import com.aneto.authService.dto.request.UsersResponse;
import com.aneto.authService.dto.response.LoginResponse;
import com.aneto.authService.dto.response.RegistrationResponse;
import com.aneto.authService.models.Users;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface AuthService {

    // =========================================================================
    // FUNÇÃO DE LOGIN (ADICIONADA)
    // =========================================================================
    LoginResponse login(UserCredentialsRequest request);

    RegistrationResponse registrarUsers(UserCredentialsRequest request);

    Users findPorUsername(String username);

    boolean existeUsers(String username);

    void createPasswordResetTokenForUser(String email);

    String saveToken(Users users);

    void resetPassword(String token, String newPassword);

    void UpdateProfile(String username, String publicUrl);

    LoginResponse verificarCodigo(UserCredentialsRequest request);

     LoginResponse processGoogleLogin(String email, String name, String googleToken);

    void eliminarUtilizador(String publicId);

    void atualizarUtilizador(String publicId, @Valid UserCredentialsRequest request);

    LoginResponse getLoginResponse(Map<String, String> data);

    LoginResponse processarLoginFacebook(Map<String, String> data);

    void vincularTelegram(UUID publicId, String chatId);

    void unlinkTelegram(String username);

    String obterTelegramChatId(String username);

    void removerChatIdPorBloqueio(String chatId);

    void mudarStatusMfa(String username, boolean status);
    void activateMfa(String token, String code);
    Map<String, String> setupMfa(String token);

    boolean verificarCodigoMfa(String username, String code);

    List<UsersResponse> findAll();

    void atualizarPermissoesUtilizador(String publicId, List<String> modulos);
}
