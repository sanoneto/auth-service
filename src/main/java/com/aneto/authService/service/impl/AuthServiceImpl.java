package com.aneto.authService.service.impl;

import com.aneto.authService.dto.request.UserCredentialsRequest;
import com.aneto.authService.dto.request.UsersResponse;
import com.aneto.authService.dto.response.LoginResponse;
import com.aneto.authService.dto.response.RegistrationResponse;
import com.aneto.authService.exception.ConflictException;
import com.aneto.authService.exception.InvalidTokenException;
import com.aneto.authService.exception.ResourceNotFoundException;
import com.aneto.authService.mapper.RequestMapper;
import com.aneto.authService.models.PasswordResetToken;
import com.aneto.authService.models.SocioUtils;
import com.aneto.authService.models.UserRole;
import com.aneto.authService.models.Users;
import com.aneto.authService.queue.EmailProducer;
import com.aneto.authService.repository.JwtTokenRepository;
import com.aneto.authService.repository.PasswordResetTokenRepository;
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
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
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
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Value("${password-reset.expiration-minutes:30}")
    private long resetTokenExpirationMinutes;

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
            throw new DisabledException("Esta conta ainda não foi ativada. Verifique o seu e-mail.");
        }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new BadCredentialsException("Utilizador ou password incorretos.");
        }

        String token = saveToken(user);
        String message = "Login efetuado com sucesso!";
        return requestMapper.mapToUserResponse(user, token, message);
    }

    @Override
    @Transactional
    public RegistrationResponse registrarUsers(UserCredentialsRequest request) {
        if (existeUsers(request.username())) {
            throw new ConflictException("O nome de utilizador já está em uso.");
        }

        // Validação de códigos de convite baseada no Enum
        UserRole roleSolicitada = request.role();
        if (roleSolicitada == UserRole.ADMIN && !CODEADMIN.equals(request.inviteCode())) {
            throw new SecurityException("Código de autorização inválido para ADMINISTRADOR.");
        }

        // Exemplo de como tratar roles que não estão no Enum original se necessário
        if (roleSolicitada.name().equals("ESPECIALISTA") && !CODEESPECIALISTA.equals(request.inviteCode())) {
            throw new SecurityException("Código de autorização inválido para ESPECIALISTA.");
        }

        Users users = requestMapper.mapToLogin(request);
        users.setPassword(passwordEncoder.encode(users.getPassword()));

        String vCode = String.format("%06d", new java.security.SecureRandom().nextInt(999999));
        users.setVerificationCode(vCode);
        users.setEnabled(false);

        // Persiste e gera o número de sócio a partir do ID atribuído pela DB
        users = guardarComNumeroSocio(users);

        // Notificação
        enviarEmailBoasVindas(users, users.getNumeroSocio(), vCode);

        return new RegistrationResponse("Registo realizado. Verifique o seu e-mail.", users.getUsername());
    }

    /**
     * Guarda um novo utilizador e atribui-lhe o número de sócio com base no ID gerado pela DB.
     * O ID (IDENTITY) é único e atómico, evitando colisões em registos concorrentes.
     * Deve ser chamado dentro de uma transação.
     */
    private Users guardarComNumeroSocio(Users users) {
        // Valor temporário único apenas para satisfazer a restrição NOT NULL/UNIQUE no primeiro INSERT
        users.setNumeroSocio("PENDENTE-" + UUID.randomUUID());
        Users saved = usersRepository.saveAndFlush(users);

        saved.setNumeroSocio(SocioUtils.gerarNumero(saved.getId()));
        return usersRepository.save(saved);
    }

    // Método auxiliar para manter o código principal limpo
    private void enviarEmailBoasVindas(Users u, String socioNum, String code) {
        String html = String.format(
                "<div style='font-family: Arial, sans-serif; color: #333;'> " +
                        "<h2>Bem-vindo! O seu registo foi concluído.</h2>" +
                        "<p>O seu <b>Número de Sócio Oficial</b> é: <span style='color: #007bff; font-size: 18px;'>%s</span></p>" +
                        "<p>Utilize o código abaixo para ativar a sua conta:</p>" +
                        "<div style='background: #f4f4f4; padding: 15px; text-align: center; font-size: 24px; letter-spacing: 5px; font-weight: bold;'>" +
                        "%s</div>" +
                        "</div>", socioNum, code);

        emailProducer.publishEmailRequest(u.getUsername(), u.getEmail(), "Bem-vindo Sócio!", html, null);
    }

    @Override
    public LoginResponse verificarCodigo(UserCredentialsRequest request) {
        Users user = usersRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("Utilizador não encontrado."));

        if (user.getVerificationCode() == null || !user.getVerificationCode().equals(request.code())) {
            throw new SecurityException("Código de verificação inválido.");
        }

        user.setEnabled(true);
        user.setVerificationCode(null);
        usersRepository.save(user);

        String token = saveToken(user);
        String message = "Login efetuado com sucesso!";
        return requestMapper.mapToUserResponse(user, token, message);
    }

    @Override
    @Transactional
    public LoginResponse processGoogleLogin(String email, String name, String googleToken) {
        Users user = usersRepository.findByEmail(email)
                .orElseGet(() -> {
                    Users newUser = new Users();
                    newUser.setEmail(email);
                    newUser.setUsername(email.split("@")[0]);
                    newUser.setRole(UserRole.USER); // Definindo Enum padrão
                    newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
                    newUser.setEnabled(true);
                    return guardarComNumeroSocio(newUser);
                });

        String token = saveToken(user);
        String message = "Login efetuado com sucesso!";
        return requestMapper.mapToUserResponse(user, token, message);
    }

    public LoginResponse processGoogleLogin(String email, String name) {
        return processGoogleLogin(email, name, null);
    }

    @Override
    @Transactional
    public void eliminarUtilizador(String publicId) {
        UUID uuid;
        try {
            uuid = UUID.fromString(publicId);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("ID Público em formato inválido");
        }
        Users usuario = usersRepository.findByPublicId(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Utilizador não encontrado"));

        usersRepository.delete(usuario);
    }

    @Override
    @Transactional
    public void atualizarUtilizador(String publicId, UserCredentialsRequest request) {
        Users usuario = usersRepository.findByPublicId(UUID.fromString(publicId))
                .orElseThrow(() -> new ResourceNotFoundException("Utilizador não encontrado"));

        if (request.username() != null) usuario.setUsername(request.username());
        if (request.email() != null) usuario.setEmail(request.email());
        if (request.role() != null) usuario.setRole(request.role()); // Atualiza o Enum

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
            return processGoogleLogin(payload.getEmail(), (String) payload.get("name"), googleToken);
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
            return processGoogleLogin(email, email.split("@")[0], accessToken);
        } catch (Exception e) {
            throw new BadCredentialsException("Erro ao validar login do Facebook.");
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

    @Override
    public List<UsersResponse> findAll() {
        return requestMapper.mapToUserResponseList(usersRepository.findAll());
    }

    @Override
    @Transactional
    public void atualizarPermissoesUtilizador(String publicId, List<String> novosModulos) {
        UUID uuid = UUID.fromString(publicId);
        Users usuario = usersRepository.findByPublicId(uuid)
                .orElseThrow(() -> new ResourceNotFoundException("Utilizador não encontrado"));

        usuario.setAllowedModules(novosModulos);
        usersRepository.saveAndFlush(usuario);

        log.info("Módulos persistidos para {}: {}", usuario.getUsername(), novosModulos);
    }

    @Override
    @Transactional
    public void createPasswordResetTokenForUser(String email) {
        Users user = usersRepository.findByEmailIgnoreCase(email).orElse(null);
        if (user == null) return;

        // Um único link ativo por utilizador: pedidos novos invalidam os anteriores
        passwordResetTokenRepository.deleteByUsersId(user.getId());

        // Token opaco e aleatório (não é um JWT, não serve para autenticar)
        byte[] randomBytes = new byte[32];
        SECURE_RANDOM.nextBytes(randomBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

        PasswordResetToken resetToken = new PasswordResetToken();
        resetToken.setTokenHash(sha256(token));
        resetToken.setExpiresAt(Instant.now().plus(Duration.ofMinutes(resetTokenExpirationMinutes)));
        resetToken.setUsers(user);
        passwordResetTokenRepository.save(resetToken);

        emailProducer.publishEmailRequest(user.getUsername(), user.getEmail(), "Reset Password", "Link: ", FRONTEND_BASE_URL + "/reset-password?token=" + token);
    }

    @Override
    @Transactional
    public String saveToken(Users users) {
        // Converte o Enum Role para String para o JWT
        String roleName = users.getRole() != null ? users.getRole().name() : "USER";
        List<String> roles = List.of(roleName);

        String token = jwtTokenUtil.generateToken(users.getUsername(), roles);
        jwtTokenService.saveToken(token, users.getUsername(), Instant.now(), Instant.now().plusMillis(jwtTokenUtil.getExpirationMillis()));
        return token;
    }

    @Override
    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(sha256(token))
                .orElseThrow(() -> new InvalidTokenException("Link de recuperação inválido ou já utilizado."));

        if (resetToken.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidTokenException("Link de recuperação expirado. Peça um novo.");
        }

        Users user = resetToken.getUsers();
        user.setPassword(passwordEncoder.encode(newPassword));
        usersRepository.save(user);

        // Uso único; e termina a sessão ativa, já que a password mudou
        passwordResetTokenRepository.delete(resetToken);
        tokenRepository.deleteByUsersId(user.getId());
    }

    private static String sha256(String value) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 não disponível", e);
        }
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
        Users user = usersRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utilizador não encontrado."));
        user.setMfaEnabled(status);
        usersRepository.save(user);
    }

    @Override
    @Transactional
    public Map<String, String> setupMfa(String username) {
        Users user = usersRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utilizador não encontrado."));

        if (user.getMfaSecret() == null || user.getMfaSecret().isEmpty()) {
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
        Users usuario = usersRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utilizador não encontrado"));

        String secret = usuario.getMfaSecret();
        if (secret == null || secret.isEmpty()) {
            throw new ConflictException("MFA não está configurado para este utilizador");
        }

        GoogleAuthenticator gAuth = new GoogleAuthenticator();

        try {
            int codeInt = Integer.parseInt(code);
            return gAuth.authorize(secret, codeInt);
        } catch (NumberFormatException e) {
            return false;
        }
    }

    @Override
    @Transactional
    public void activateMfa(String username, String code) {
        Users user = usersRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utilizador não encontrado."));

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
                .orElseThrow(() -> new ResourceNotFoundException("Utilizador não encontrado."));

        user.setTelegramChatId(chatId);
        user.setUpdatedAt(LocalDateTime.now());
        usersRepository.saveAndFlush(user);
    }

    @Override
    @Transactional
    public void unlinkTelegram(String username) {
        Users user = usersRepository.findByUsername(username)
                .orElseThrow(() -> new ResourceNotFoundException("Utilizador não encontrado."));
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