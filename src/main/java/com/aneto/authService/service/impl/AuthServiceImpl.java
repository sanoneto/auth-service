package com.aneto.authService.service.impl;

import com.aneto.authService.dto.request.UserCredentialsRequest;
import com.aneto.authService.dto.request.UsersResponse;
import com.aneto.authService.dto.response.LoginResponse;
import com.aneto.authService.dto.response.RegistrationResponse;
import com.aneto.authService.mapper.RequestMapper;
import com.aneto.authService.models.JwtToken;
import com.aneto.authService.models.Users;
import com.aneto.authService.queue.EmailProducer;
import com.aneto.authService.repository.JwtTokenRepository;
import com.aneto.authService.repository.ProjectsRepository;
import com.aneto.authService.repository.UsersRepository;
import com.aneto.authService.security.JwtTokenUtil;
import com.aneto.authService.service.AuthService;
import com.aneto.authService.service.JwtTokenService;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.warrenstrange.googleauth.GoogleAuthenticator;
import com.warrenstrange.googleauth.GoogleAuthenticatorKey;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final UsersRepository usersRepository;
    private final PasswordEncoder passwordEncoder;
    private final RequestMapper requestMapper;
    private final JwtTokenUtil jwtTokenUtil;
    private final EmailProducer emailProducer;
    private final JwtTokenService jwtTokenService;
    private final JwtTokenRepository tokenRepository;
    private final ProjectsRepository projectsRepository;
    private final RestTemplate restTemplate;

    @Value("${url.front-end}")
    String FRONTEND_BASE_URL;

    @Value("${codes.especialista}")
    private String CODEESPECIALISTA;

    @Value("${codes.admin}")
    private String CODEADMIN;

    @Value("${google.client-id}")
    private String googleClientId;

    @Override
    public LoginResponse login(UserCredentialsRequest request) {
        log.info("Tentativa de login para o utilizador: {}", request.username());

        Users user = usersRepository.findByUsername(request.username())
                .orElseThrow(() -> new BadCredentialsException("Utilizador ou password incorretos."));

        if (!user.isEnabled()) {
            throw new RuntimeException("Esta conta ainda não foi ativada. Verifique o seu e-mail.");
        }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Utilizador ou password incorretos.");
        }

        String token = saveToken(user);
        return new LoginResponse("Login efetuado com sucesso!", token, user.getRole());
    }

    @Override
    @Transactional
    public RegistrationResponse registrarUsers(UserCredentialsRequest request) {
        if (existeUsers(request.username())) {
            throw new RuntimeException("O nome de utilizador já está em uso.");
        }

        String roleSolicitada = request.role().toUpperCase();
        if (roleSolicitada.equals("ADMIN") && !CODEADMIN.equals(request.inviteCode())) {
            throw new SecurityException("Código de autorização inválido para ADMINISTRADOR.");
        }
        if (roleSolicitada.equals("ESPECIALISTA") && !CODEESPECIALISTA.equals(request.inviteCode())) {
            throw new SecurityException("Código de autorização inválido para ESPECIALISTA.");
        }

        Users users = requestMapper.mapToLogin(request);
        users.setPassword(passwordEncoder.encode(users.getPassword()));

        String code = String.format("%06d", new java.security.SecureRandom().nextInt(999999));
        users.setVerificationCode(code);
        users.setEnabled(false);

        usersRepository.save(users);
        emailProducer.publishEmailRequest(
                users.getUsername(),
                users.getEmail(),
                "Ativação de Conta",
                "<h3>" + code + "</h3>",
                null
        );

        return new RegistrationResponse("Registo realizado. Verifique o seu e-mail.", users.getUsername());
    }

    @Override
    public LoginResponse verificarCodigo(UserCredentialsRequest request) {
        Users user = usersRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("Utilizador não encontrado."));

        if (user.getVerificationCode() == null || !user.getVerificationCode().equals(request.code())) {
            throw new SecurityException("Código de verificação inválido.");
        }

        user.setEnabled(true);
        user.setVerificationCode(null);
        usersRepository.save(user);

        String token = saveToken(user);
        return new LoginResponse("Conta ativada!", token, user.getRole());
    }

    @Override
    @Transactional
    public LoginResponse processGoogleLogin(String email, String name) {
        Users user = usersRepository.findByEmail(email)
                .orElseGet(() -> {
                    Users newUser = new Users();
                    newUser.setEmail(email);
                    newUser.setUsername(email.split("@")[0]);
                    newUser.setRole("USER");
                    newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
                    newUser.setEnabled(true);
                    return usersRepository.save(newUser);
                });

        String systemToken = saveToken(user);
        return new LoginResponse("Login efetuado com sucesso!", systemToken, user.getRole());
    }

    @Override
    @Transactional
    public void eliminarUtilizador(String publicId) {
        Users usuario = usersRepository.findByPublicId(UUID.fromString(publicId))
                .orElseThrow(() -> new RuntimeException("Utilizador não encontrado"));
        usersRepository.delete(usuario);
    }

    @Override
    @Transactional
    public void atualizarUtilizador(String publicId, UserCredentialsRequest request) {
        Users usuario = usersRepository.findByPublicId(UUID.fromString(publicId))
                .orElseThrow(() -> new RuntimeException("Utilizador não encontrado"));
        if (request.username() != null) usuario.setUsername(request.username());
        if (request.email() != null) usuario.setEmail(request.email());
        usersRepository.save(usuario);
    }

    @Override
    @Transactional
    public LoginResponse getLoginResponse(Map<String, String> data) {
        String googleToken = data.get("token");
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(googleClientId))
                .build();

        try {
            GoogleIdToken idToken = verifier.verify(googleToken);
            if (idToken == null) throw new BadCredentialsException("Token inválido.");
            GoogleIdToken.Payload payload = idToken.getPayload();
            return processGoogleLogin(payload.getEmail(), (String) payload.get("name"));
        } catch (Exception e) {
            throw new BadCredentialsException("Erro ao validar Google Token.");
        }
    }

    @Override
    @Transactional
    public LoginResponse processarLoginFacebook(Map<String, String> data) {
        String accessToken = data.get("accessToken");
        String fbUrl = UriComponentsBuilder.fromUriString("https://graph.facebook.com/me")
                .queryParam("fields", "id,email")
                .queryParam("access_token", accessToken).toUriString();
        try {
            Map<String, Object> fbProfile = restTemplate.getForObject(fbUrl, Map.class);
            String email = (String) fbProfile.get("email");
            return processGoogleLogin(email, email.split("@")[0]);
        } catch (Exception e) {
            throw new RuntimeException("Erro Facebook login.");
        }
    }

    @Override
    public Users findPorUsername(String username) throws UsernameNotFoundException {
        return usersRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Não encontrado."));
    }

    @Override
    public boolean existeUsers(String username) {
        return usersRepository.findByUsername(username).isPresent();
    }

    public List<UsersResponse> findAll() {
        return requestMapper.UsersResponse(usersRepository.findAll());
    }

    @Override
    public void createPasswordResetTokenForUser(String email) {
        Users user = usersRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) return;
        String token = saveToken(user);
        emailProducer.publishEmailRequest(user.getUsername(), user.getEmail(), "Reset Password", "Link: ", FRONTEND_BASE_URL + "/reset-password?token=" + token);
    }

    @Override
    @Transactional
    public String saveToken(Users users) {
        String role = users.getRole();
        List<String> roles = List.of(role != null ? role : "USER");
        String token = jwtTokenUtil.generateToken(users.getUsername(), roles);
        jwtTokenService.saveToken(token, users.getUsername(), Instant.now(), Instant.now().plusMillis(jwtTokenUtil.getExpirationMillis()));
        return token;
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        JwtToken jwtToken = jwtTokenService.findByToken(token).orElseThrow();
        Users user = jwtToken.getUsers();
        user.setPassword(passwordEncoder.encode(newPassword));
        usersRepository.save(user);
        tokenRepository.delete(jwtToken);
    }

    @Override
    public void UpdateProfile(String username, String publicUrl) {
        usersRepository.findByUsername(username).ifPresent(u -> {
            u.setProfile_picture_url(publicUrl);
            usersRepository.save(u);
        });
    }

    @Override
    @Transactional
    public void mudarStatusMfa(String username, boolean status) {
        Users user = usersRepository.findByUsername(username).orElseThrow();
        user.setMfaEnabled(status);
        usersRepository.save(user);
    }

    // =========================================================================
    // CORREÇÃO: setupMfa e activateMfa
    // =========================================================================
    @Override
    @Transactional
    public Map<String, String> setupMfa(String token) {
        // CORREÇÃO: Certifique-se que o seu JwtTokenUtil tem o método extractUsername ou similar
        String username = jwtTokenUtil.extractUsername(token);

        Users user = usersRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utilizador não encontrado."));

        if (user.getMfaSecret() == null || user.getMfaSecret().isEmpty()) {
            // CORREÇÃO: Sintaxe correta para a biblioteca googleauth
            GoogleAuthenticator gAuth = new GoogleAuthenticator();
            final GoogleAuthenticatorKey key = gAuth.createCredentials();
            user.setMfaSecret(key.getKey());
            usersRepository.save(user);
        }

        String appName = "PROACT";
        String qrCodeUrl = String.format(
                "https://api.qrserver.com/v1/create-qr-code/?data=otpauth://totp/%s:%s?secret=%s&issuer=%s&size=200x200",
                appName, user.getEmail(), user.getMfaSecret(), appName
        );

        Map<String, String> response = new HashMap<>();
        response.put("qrCodeUrl", qrCodeUrl);
        response.put("secret", user.getMfaSecret());
        return response;
    }

    @Override
    public boolean verificarCodigoMfa(String username, String code) {
        // 1. Procurar o utilizador na base de dados
        Users usuario = usersRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utilizador não encontrado"));

        // 2. Verificar se o segredo existe
        String secret = usuario.getMfaSecret();
        if (secret == null || secret.isEmpty()) {
            throw new RuntimeException("MFA não está configurado para este utilizador");
        }

        // 3. Validar o código de 6 dígitos usando a biblioteca GoogleAuthenticator
        GoogleAuthenticator gAuth = new GoogleAuthenticator();

        try {
            int codeInt = Integer.parseInt(code); // Converte o código String para Int
            return gAuth.authorize(secret, codeInt);
        } catch (NumberFormatException e) {
            return false; // Se não for um número válido, falha logo
        }
    }

    @Override
    @Transactional
    public void activateMfa(String token, String code) {
        String username = jwtTokenUtil.extractUsername(token);
        Users user = usersRepository.findByUsername(username).orElseThrow();

        if (verifyTotpCode(user.getMfaSecret(), code)) {
            user.setMfaEnabled(true);
            usersRepository.save(user);
        } else {
            throw new SecurityException("Código inválido.");
        }
    }

    private boolean verifyTotpCode(String secret, String code) {
        try {
            GoogleAuthenticator gAuth = new GoogleAuthenticator();
            return gAuth.authorize(secret, Integer.parseInt(code));
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    @Transactional
    public void vincularTelegram(UUID publicId, String chatId) {
        usersRepository.findByTelegramChatId(chatId).ifPresent(userExistente -> {
            if (!userExistente.getPublicId().equals(publicId)) {
                userExistente.setTelegramChatId(null);
                usersRepository.saveAndFlush(userExistente);
            }
        });

        Users user = usersRepository.findByPublicId(publicId)
                .orElseThrow(() -> new RuntimeException("Utilizador não encontrado."));

        user.setTelegramChatId(chatId);
        user.setUpdatedAt(LocalDateTime.now());
        usersRepository.saveAndFlush(user);
    }

    @Override
    @Transactional
    public void unlinkTelegram(String username) {
        Users user = usersRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utilizador não encontrado."));
        user.setTelegramChatId(null);
        usersRepository.save(user);
    }

    @Override
    public String obterTelegramChatId(String username) {
        return usersRepository.findByUsername(username)
                .map(Users::getTelegramChatId)
                .orElse(null);
    }

    @Override
    @Transactional
    public void removerChatIdPorBloqueio(String chatId) {
        usersRepository.findByTelegramChatId(chatId).ifPresent(user -> {
            user.setTelegramChatId(null);
            usersRepository.save(user);
        });
    }
}