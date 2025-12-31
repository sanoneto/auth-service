package com.aneto.authService.service.impl;


import com.aneto.authService.dto.request.UserCredentialsRequest;
import com.aneto.authService.dto.request.UsersResponse;
import com.aneto.authService.dto.response.LoginResponse;
import com.aneto.authService.mapper.RequestMapper;
import com.aneto.authService.models.JwtToken;
import com.aneto.authService.models.Users;
import com.aneto.authService.queue.EmailProducer;
import com.aneto.authService.repository.JwtTokenRepository;
import com.aneto.authService.repository.UsersRepository;
import com.aneto.authService.security.JwtTokenUtil;
import com.aneto.authService.service.AuthService;
import com.aneto.authService.service.JwtTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;

@Service
public class AuthServiceImpl implements AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthServiceImpl.class);
    private final UsersRepository usersRepository; // Repositório dos usuários.
    private final PasswordEncoder passwordEncoder;     // Para a codificação da senha.
    private final RequestMapper requestMapper;
    private final JwtTokenUtil jwtTokenUtil;
    private final EmailProducer emailProducer;
    private final JwtTokenService jwtTokenService;
    private final JwtTokenRepository tokenRepository;

    public AuthServiceImpl(UsersRepository usersRepository, RequestMapper requestMapper,
                           PasswordEncoder passwordEncoder, EmailProducer emailProducer, JwtTokenUtil jwtTokenUtil, JwtTokenService jwtTokenService, JwtTokenRepository tokenRepository) {
        this.usersRepository = usersRepository;
        this.requestMapper = requestMapper;
        this.passwordEncoder = passwordEncoder;
        this.emailProducer = emailProducer;
        this.jwtTokenUtil = jwtTokenUtil;
        this.jwtTokenService = jwtTokenService;
        this.tokenRepository = tokenRepository;
    }

    @Value("${url.front-end}")
    String FRONTEND_BASE_URL;

    @Value("${codes.especialista}")
    private String CODEESPECIALISTA;

    @Value("${codes.admin}")
    private String CODEADMIN;

    @Override
    public String registrarUsers(UserCredentialsRequest request) {
        // 1. Validação de existência
        if (existeUsers(request.username())) {
            throw new RuntimeException("O nome de utilizador já está em uso.");
        }
        // 2. Validação de Códigos
        String roleSolicitada = request.role().toUpperCase();
        if (roleSolicitada.equals("ADMIN") && !CODEADMIN.equals(request.inviteCode())) {
            throw new SecurityException("Código inválido para ADMINISTRADOR.");
        }
        if (roleSolicitada.equals("ESPECIALISTA") && !CODEESPECIALISTA.equals(request.inviteCode())) {
            throw new SecurityException("Código inválido para ESPECIALISTA.");
        }
        // 3. Persistência
        Users users = requestMapper.mapToLogin(request);
        users.setPassword(passwordEncoder.encode(users.getPassword()));

        // Gerar código aleatório de 6 dígitos
        String code = String.format("%06d", new Random().nextInt(999999));
        users.setVerificationCode(code);
        users.setEnabled(false);
        usersRepository.save(users);

        // Enviar e-mail com o CÓDIGO e não apenas boas-vindas
        String message = "O seu código de ativação é: " + code;
        log.info("O seu código de ativação é: :{}", code);
        emailProducer.sendRegistrationEmail(users.getUsername(), users.getEmail(), "Código de Verificação", message, null);
        return "Registo realizado. Verifique o seu e-mail para ativar a conta.";
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
        user.setVerificationCode(null);
        usersRepository.save(user);

        // 4. GERA O TOKEN AQUI!
        // O utilizador valida o e-mail e já fica logado.
        String token = saveToken(user);

        return new LoginResponse("Conta ativada com sucesso!", token);
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

        // Se o usuário não for encontrado, não faz nada para evitar indicar
        // se o email está ou não registado.
        if (users == null) {
            return;
        }
        // 1. Geração e salva o token
        String token = saveToken(users);
        // URL do seu front-end React
        String resetLink = FRONTEND_BASE_URL + "/reset-password?token=" + token;

        String message = "Olá, \n\n"
                + "Você solicitou a redefinição da sua password. Por favor, clique no link abaixo para continuar:\n\n"
                + resetLink + "\n\n"
                + "Este link expira em 1 hora.\n\n"
                + "Se você não solicitou esta alteração, ignore este e-mail.\n\n"
                + "Atenciosamente,\n"
                + "Sua Equipe de Suporte.";
        String subject = "Recuperação de Password - Sistema de Registo de Horas";

        log.info("foi enviado para a fila o e-mail :{}", users.getEmail());
        // 4. Envio do Email
        emailProducer.sendRegistrationEmail(users.getUsername(), users.getEmail(), subject, message, resetLink);
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
                });

    }

}
