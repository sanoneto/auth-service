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
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
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
    private final UsersRepository usersRepository; // Repositório dos usuários.
    private final PasswordEncoder passwordEncoder;     // Para a codificação da senha.
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
    @Transactional
    public RegistrationResponse registrarUsers(UserCredentialsRequest request) {
        // 1. Validação de existência
        if (existeUsers(request.username())) {
            throw new RuntimeException("O nome de utilizador já está em uso.");
        }

        // 2. Validação de Códigos de Convite (Invite Codes)
        String roleSolicitada = request.role().toUpperCase();
        if (roleSolicitada.equals("ADMIN") && !CODEADMIN.equals(request.inviteCode())) {
            throw new SecurityException("Código de autorização inválido para ADMINISTRADOR.");
        }
        if (roleSolicitada.equals("ESPECIALISTA") && !CODEESPECIALISTA.equals(request.inviteCode())) {
            throw new SecurityException("Código de autorização inválido para ESPECIALISTA.");
        }

        Users users = requestMapper.mapToLogin(request);
        users.setPassword(passwordEncoder.encode(users.getPassword()));

        // 4. Gerar código de verificação de 6 dígitos
        String code = String.format("%06d", new java.security.SecureRandom().nextInt(999999));
        users.setVerificationCode(code);
        users.setEnabled(false); // Conta desativada até validar o código

        usersRepository.save(users);
        String subject = "Ativação de Conta - Código de Verificação";
        String htmlMessage = "Obrigado por te registares! Para ativar a tua conta, utiliza o seguinte código:";

        // IMPORTANTE: Passamos o 'code' no campo de mensagem
        // O EmailServiceImpl vai colocar isto dentro do template HTML
        emailProducer.publishEmailRequest(
                users.getUsername(),
                users.getEmail(),
                subject,
                "<h3>" + code + "</h3>", // Destaque para o código
                null
        );

        log.info("Utilizador {} registado. Código de ativação gerado.", users.getUsername());
        return new RegistrationResponse("Registo realizado. Verifique o seu e-mail para ativar a conta.", users.getUsername());
    }

    @Override
    public LoginResponse verificarCodigo(UserCredentialsRequest request) {
        // 1. Procura o utilizador pelo e-mail
        Users user = usersRepository.findByEmail(request.email())
                .orElseThrow(() -> new RuntimeException("Utilizador não encontrado."));

        // 2. Verifica se o código é o mesmo que guardamos no registo
        if (user.getVerificationCode() == null || !user.getVerificationCode().equals(request.code())) {
            throw new SecurityException("Código de verificação inválido ou expirado.");
        }

        // 3. Ativa a conta e limpa o código para não ser reutilizado
        user.setEnabled(true);
        user.setVerificationCode(null); // coloca o codigo a null
        usersRepository.save(user);

        // 4. GERA O TOKEN AQUI!
        // O utilizador valida o e-mail e já fica logado.
        String token = saveToken(user);

        return new LoginResponse("Conta ativada com sucesso!", token, null);
    }

    @Override
    @Transactional
    public LoginResponse processGoogleLogin(String email, String name) {
        Users user = usersRepository.findByEmail(email)
                .orElseGet(() -> {
                    log.info("Criando novo utilizador via Google Login: {}", email);
                    Users newUser = new Users();
                    newUser.setEmail(email);
                    String username = email.split("@")[0];
                    newUser.setUsername(username);
                    newUser.setUsername(email); // Pode ajustar para extrair parte do e-mail se preferir
                    newUser.setRole("USER");
                    // Senha aleatória forte para conta social
                    newUser.setPassword(passwordEncoder.encode(Base64.getEncoder().encodeToString(new byte[16])));
                    newUser.setEnabled(true); // Google já verificou o e-mail
                    return usersRepository.save(newUser);
                });

        // Se o usuário existia mas estava desativado, ativamos (opcional)
        if (!user.isEnabled()) {
            user.setEnabled(true);
            usersRepository.save(user);
        }

        String systemToken = saveToken(user);
        return new LoginResponse("Login efetuado com sucesso!", systemToken, null);
    }

    @Override
    @Transactional
    public void eliminarUtilizador(String publicId) {
        // 1. Localiza o utilizador pelo publicId
        Users usuario = usersRepository.findByPublicId(UUID.fromString(publicId))
                .orElseThrow(() -> new RuntimeException("Utilizador não encontrado"));
        // Apaga todos os tokens associados ao ID do utilizador
        tokenRepository.deleteByUsersId(usuario.getId());
        // Apaga todos os projetos associados ao ID do utilizador
        projectsRepository.deleteByUsersId(usuario.getId());

        // 3. Agora que os filhos morreram, podemos apagar o pai
        usersRepository.delete(usuario);

        log.info("Limpeza completa: Tokens, Projetos e Utilizador {} removidos.", usuario.getUsername());
    }

    @Override
    @Transactional
    public void atualizarUtilizador(String publicId, UserCredentialsRequest request) {
        Users usuario = usersRepository.findByPublicId(UUID.fromString(publicId))
                .orElseThrow(() -> new RuntimeException("Utilizador não encontrado com o ID: " + publicId));

        // Atualização seletiva: só altera se o campo não for nulo no request
        if (request.username() != null && !request.username().isBlank()) {
            usuario.setUsername(request.username());
        }
        if (request.email() != null && !request.email().isBlank()) {
            usuario.setEmail(request.email());
        }
        if (request.role() != null && !request.role().isBlank()) {
            usuario.setRole(request.role());
        }

        usersRepository.save(usuario);
    }

    @Override
    @Transactional
    public LoginResponse getLoginResponse(Map<String, String> data) {
        String googleToken = data.get("token");

        if (googleToken == null || googleToken.isBlank()) {
            throw new BadCredentialsException("Token do Google não fornecido no corpo da requisição.");
        }
        // 1. Valida o token com o Google
        // Nota: O ideal é mover este 'verifier' para um @Bean de configuração
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), new GsonFactory())
                .setAudience(Collections.singletonList(googleClientId))
                .build();

        // Removemos o try-catch manual, o GlobalExceptionHandler cuida das exceções!
        GoogleIdToken idToken;
        try {
            idToken = verifier.verify(googleToken);
        } catch (Exception e) {
            // Lançamos uma exceção personalizada ou BadCredentials para o Handler capturar
            throw new BadCredentialsException("Token do Google inválido ou expirado.");
        }

        if (idToken == null) {
            throw new BadCredentialsException("Não foi possível validar o token com o Google.");
        }

        GoogleIdToken.Payload payload = idToken.getPayload();
        String email = payload.getEmail();
        String name = (String) payload.get("name");

        // 2. Lógica de Login/Registo
        LoginResponse response = processGoogleLogin(email, name);

        // 3. Enviar e-mail de "Bem-vindo" via Fila (RabbitMQ -> Resend)
        // Usamos o novo nome do método que criámos no EmailProducer
        emailProducer.publishEmailRequest(
                name,
                email,
                "Bem-vindo ao Sistema Sanoneto",
                "Estamos felizes por teres feito login com o Google!",
                null
        );
        return response;
    }

    @Override
    @Transactional
    public LoginResponse processarLoginFacebook(Map<String, String> data) {
        String accessToken = data.get("accessToken");

        // 1. Validar o token com a Graph API da Meta (apenas ID e Email, como no Google)
        String fbUrl = UriComponentsBuilder.fromUriString("https://graph.facebook.com/me")
                .queryParam("fields", "id,email")
                .queryParam("access_token", accessToken)
                .toUriString();
        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> fbProfile = restTemplate.getForObject(fbUrl, Map.class);

            if (fbProfile == null || !fbProfile.containsKey("id")) {
                throw new RuntimeException("Token do Facebook inválido ou expirado.");
            }

            String email = (String) fbProfile.get("email");
            String facebookId = (String) fbProfile.get("id");

            // 2. Lógica idêntica ao Google: Procura ou cria o utilizador
            Users user = usersRepository.findByEmail(email)
                    .orElseGet(() -> {
                        log.info("Criando novo utilizador via Facebook Login: {}", email);
                        Users newUser = new Users();
                        newUser.setEmail(email);
                        String username = email.split("@")[0];
                        newUser.setUsername(username);
                        newUser.setFacebookId(facebookId); // Identificador único do FB
                        newUser.setRole("USER");
                        // Usa a mesma lógica de senha segura do Google
                        newUser.setPassword(passwordEncoder.encode(Base64.getEncoder().encodeToString(new byte[16])));

                        newUser.setEnabled(true); // Facebook já verificou o e-mail
                        return usersRepository.save(newUser);
                    });

            // 3. Mesma verificação de ativação do Google
            if (!user.isEnabled()) {
                user.setEnabled(true);
                usersRepository.save(user);
            }

            // 4. Se o utilizador já existia (ex: vindo do Google), mas agora entrou via FB,
            // podes atualizar o facebookId se estiver vazio (opcional)
            if (user.getFacebookId() == null) {
                user.setFacebookId(facebookId);
                usersRepository.save(user);
            }

            String systemToken = saveToken(user);
            return new LoginResponse("Login efetuado com sucesso!", systemToken, null);

        } catch (Exception e) {
            log.error("Erro na autenticação Facebook: {}", e.getMessage());
            throw new RuntimeException("Falha ao processar login social.");
        }
    }

    @Override
    public Users findPorUsername(String username) throws UsernameNotFoundException {
        return usersRepository.findByUsername(username)
                // Se o utilizador não for encontrado na base de dados, esta exceção é lançada
                .orElseThrow(() -> new UsernameNotFoundException("Utilizador não encontrado com o nome: " + username));
    }

    @Override
    public boolean existeUsers(String username) {
        return usersRepository.findByUsername(username).isPresent();
    }

    public List<UsersResponse> findAll() {
        List<Users> userlist = usersRepository.findAll();
        return requestMapper.UsersResponse(userlist);
    }

    @Override
    public void createPasswordResetTokenForUser(String email) {
        Users users = usersRepository.findByEmailIgnoreCase(email)
                .orElse(null);

        // Proteção contra enumeração de emails
        if (users == null) {
            log.warn("Tentativa de recuperação de senha para email inexistente: {}", email);
            return;
        }

        String token = saveToken(users);
        String resetLink = "%s/reset-password?token=%s".formatted(FRONTEND_BASE_URL, token);
        String subject = "Recuperação de Password - Sanoneto System";
        String message = "Recebemos um pedido para redefinir a sua password. Clique no botão abaixo para prosseguir. Este link é válido por 1 hora.";
        log.info("Enviando solicitação de reset de senha para a fila: {}", users.getEmail());

        // 4. Envio via Producer (usando o novo nome do método)
        emailProducer.publishEmailRequest(
                users.getUsername(),
                users.getEmail(),
                subject,
                message,
                resetLink // O EmailServiceImpl usará isto para criar o botão HTML
        );
    }

    @Override
    public String saveToken(Users users) {
        String role = users.getRole();
        List<String> rolesForToken = (role == null || role.isBlank())
                ? List.of()
                : List.of(role.startsWith("ROLE_") ? role.substring(5) : role);
        String token = jwtTokenUtil.generateToken(users.getUsername(), rolesForToken);

        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusMillis(jwtTokenUtil.getExpirationMillis());
        jwtTokenService.saveToken(token, users.getUsername(), issuedAt, expiresAt);
        return token;
    }

    @Override
    public void resetPassword(String token, String newPassword) {
        // 1. Validar o Token
        Optional<JwtToken> jwtTokenOptional = jwtTokenService.findByToken(token);

        JwtToken jwtToken = jwtTokenOptional
                // Lança uma RuntimeException se o Optional estiver vazio
                .orElseThrow(() -> new NoSuchElementException("Token inválido ou inexistente."));

        // 2. Verificar se o token expirou
        if (jwtToken.getExpiresAt().isBefore(new Date().toInstant())) {
            // Se expirou, deve ser deletado imediatamente e lançar exceção
            tokenRepository.delete(jwtToken);
        }
        // 3. Atualizar a Password
        Users users = jwtToken.getUsers();

        // Codificar a nova password
        String encodedPassword = passwordEncoder.encode(newPassword);
        users.setPassword(encodedPassword);

        // Salvar a nova password do utilizador
        usersRepository.save(users);

        // 4. Invalidar o Token
        // Deletar o token da base de dados imediatamente após o uso para prevenir repetição
        tokenRepository.delete(jwtToken);
    }

    @Override
    public void UpdateProfile(String username, String publicUrl) {
        usersRepository.findByUsername(username)
                .ifPresent(user -> {
                    // 2. O objeto 'user' dentro deste bloco JÁ é um Users.
                    user.setProfile_picture_url(publicUrl);
                    // 3. O save() deve ser chamado com o objeto Users, não com o Optional.
                    usersRepository.save(user);
                    log.info("A adicionado o link {} da foto do perfil {}", username, publicUrl);
                });

    }

    @Override
    @Transactional
    public void vincularTelegram(UUID publicId, String chatId) {
        log.info("Iniciando vínculo para PublicID: {} com ChatID: {}", publicId, chatId);

        // 1. Limpeza de duplicados: Verifica se este Telegram já pertence a outra conta
        usersRepository.findByTelegramChatId(chatId).ifPresent(userExistente -> {
            // Se o ChatID já existe em outro PublicID, removemos do antigo para evitar o erro de Unique Constraint
            if (!userExistente.getPublicId().equals(publicId)) {
                log.warn("O ChatID {} já estava vinculado ao user {}. Removendo vínculo antigo...",
                        chatId, userExistente.getUsername());

                userExistente.setTelegramChatId(null);
                usersRepository.saveAndFlush(userExistente);
            }
        });

        // 2. Procura o usuário que enviou o comando /start pelo PublicID
        Users user = usersRepository.findByPublicId(publicId)
                .orElseThrow(() -> new RuntimeException("Utilizador não encontrado com PublicID: " + publicId));

        // 3. Define o novo vínculo (Corrigido o erro de sintaxe aqui)
        user.setTelegramChatId(chatId);
        user.setUpdatedAt(LocalDateTime.now());

        try {
            usersRepository.saveAndFlush(user);
            log.info("✅ Telegram vinculado com sucesso ao utilizador: {}", user.getUsername());
        } catch (DataIntegrityViolationException e) {
            log.error("❌ Erro de integridade: {}", e.getMessage());
            throw new RuntimeException("Este Telegram já está vinculado e não pôde ser movido.");
        }
    }

    @Override
    @Transactional
    public void unlinkTelegram(String username) {
        Users user = usersRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Utilizador não encontrado: " + username));

        user.setTelegramChatId(null);
        usersRepository.save(user);
    }

    @Override
    public String obterTelegramChatId(String username) {
        Users usuario = usersRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado: " + username));

        // Assume-se que o campo no teu modelo Users se chama telegramChatId
        return usuario.getTelegramChatId();
    }
}
