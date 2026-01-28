package com.aneto.authService.service.impl;

import com.aneto.authService.models.JwtToken;
import com.aneto.authService.models.Users;
import com.aneto.authService.repository.JwtTokenRepository;
import com.aneto.authService.repository.UsersRepository;
import com.aneto.authService.service.JwtTokenService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class JwtTokenServiceImpl implements JwtTokenService {

    private final JwtTokenRepository jwtTokenRepository;
    private final UsersRepository usersRepository;

    @Override
    @Transactional // Garante que o delete e o save ocorrem na mesma transação
    public void saveToken(String token, String username, Instant issuedAt, Instant expiresAt) {
        Users user = usersRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Usuário não encontrado"));

        // 1. LIMPEZA: Remove todos os tokens antigos do utilizador.
        // Isso resolve o erro dos 71 resultados e garante "Um token por utilizador".
        jwtTokenRepository.deleteByUsersId(user.getId());

        // Para garantir que o JPA processa o delete antes do novo insert se houver conflitos de ID
        jwtTokenRepository.flush();

        // 2. CRIAÇÃO: Cria um novo registo limpo.
        JwtToken jwtToken = new JwtToken();
        jwtToken.setToken(token);
        jwtToken.setIssuedAt(issuedAt);
        jwtToken.setExpiresAt(expiresAt);
        jwtToken.setRevoked(false);
        jwtToken.setUsers(user);

        jwtTokenRepository.save(jwtToken);
    }

    @Override
    public Optional<JwtToken> findByToken(String token) {
        return jwtTokenRepository.findByToken(token);
    }

    @Override
    public Optional<JwtToken> getReusableTokenForUser(String username) {
        Instant now = Instant.now();
        return jwtTokenRepository
                .findTopByUsers_UsernameAndRevokedFalseOrderByExpiresAtDesc(username)
                .filter(t -> t.getExpiresAt().isAfter(now));
    }

    @Override
    public Optional<JwtToken> validateToken(String token) {
        return jwtTokenRepository.findByToken(token)
                .filter(t -> !t.isRevoked())
                .filter(t -> t.getExpiresAt().isAfter(Instant.now()));
    }
}