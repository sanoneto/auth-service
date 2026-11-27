package com.aneto.authService.service;

import com.aneto.authService.dto.request.UpdateUserRequest;
import com.aneto.authService.dto.request.UserCredentialsRequest;
import com.aneto.authService.dto.request.UsersResponse;
import com.aneto.authService.dto.response.LoginResponse;
import com.aneto.authService.dto.response.RegistrationResponse;
import com.aneto.authService.models.Users;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface AuthService {

    LoginResponse login(UserCredentialsRequest request);

    RegistrationResponse registrarUsers(UserCredentialsRequest request);

    LoginResponse verificarCodigo(UserCredentialsRequest request);

    LoginResponse processGoogleLogin(String email, String name, String googleToken);

    void eliminarUtilizador(String publicId);

    void atualizarUtilizador(String publicId, UpdateUserRequest request);

    LoginResponse getLoginResponse(Map<String, String> data);

    LoginResponse processarLoginFacebook(Map<String, String> data);

    Users findPorUsername(String username) throws UsernameNotFoundException;

    boolean existeUsers(String username);

    List<UsersResponse> findAll();

    void atualizarPermissoesUtilizador(String publicId, List<String> novosModulos);

    void createPasswordResetTokenForUser(String email);

    String saveToken(Users users);

    void resetPassword(String token, String newPassword);

    void UpdateProfile(String username, String publicUrl);

    void mudarStatusMfa(String username, boolean status);

    Map<String, String> setupMfa(String username);

    boolean verificarCodigoMfa(String username, String code);

    void activateMfa(String username, String code);

    void vincularTelegram(UUID publicId, String chatId);

    void unlinkTelegram(String username);

    String obterTelegramChatId(String username);

    void removerChatIdPorBloqueio(String chatId);
}