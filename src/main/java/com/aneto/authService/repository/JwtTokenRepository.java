package com.aneto.authService.repository;


import com.aneto.authService.models.JwtToken;
import com.aneto.authService.models.Users;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JwtTokenRepository extends JpaRepository<JwtToken, Long> {
    Optional<JwtToken> findTopByUsers_UsernameAndRevokedFalseOrderByExpiresAtDesc(String username);

    Optional<JwtToken> findByToken(String token);


    // Método para buscar um token por usuário (para garantir que só haja um por usuário, se necessário)
    Optional<JwtToken> findByUsers(Users user);
}