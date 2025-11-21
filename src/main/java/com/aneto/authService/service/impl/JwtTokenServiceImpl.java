// language: java
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
@Transactional
@RequiredArgsConstructor
public class JwtTokenServiceImpl implements JwtTokenService {

    private final JwtTokenRepository jwtTokenRepository;
    private final UsersRepository usersRepository;

    @Override
    public void saveToken(String token, String username, Instant issuedAt, Instant expiresAt) {
        Users users = usersRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalStateException("Usuario não encontrado: " + username));
        JwtToken jwtToken = new JwtToken();
        jwtToken.setToken(token);
        jwtToken.setIssuedAt(issuedAt);
        jwtToken.setExpiresAt(expiresAt);
        jwtToken.setRevoked(false);
        jwtToken.setUsers(users);
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

}