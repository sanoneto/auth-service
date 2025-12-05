package com.aneto.authService.service;



import com.aneto.authService.models.JwtToken;
import com.aneto.authService.models.Users;

import java.time.Instant;
import java.util.Optional;

public interface JwtTokenService {
    void saveToken(String token, String username, Instant issuedAt, Instant expiresAt);

    Optional<JwtToken> findByToken(String token);


    // (opcionalmente) utilitário para checar expirado
    default boolean isExpired(JwtToken jwtToken) {
        return jwtToken == null || jwtToken.getExpiresAt().isBefore(Instant.now());
    }

    Optional<JwtToken> getReusableTokenForUser(String username);


    // Busca e valida um token.
    Optional<JwtToken> validateToken(String token);
}