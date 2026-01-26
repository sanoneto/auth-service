package com.aneto.authService.repository;


import com.aneto.authService.models.JwtToken;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface JwtTokenRepository extends JpaRepository<JwtToken, Long> {

    Optional<JwtToken> findByUsersId(Long userId);

    Optional<JwtToken> findByToken(String token);

    @Modifying
    @Transactional
    @Query("DELETE FROM JwtToken t WHERE t.users.id = :userId")
    void deleteByUsersId(Long userId);

    Optional<JwtToken> findTopByUsers_UsernameAndRevokedFalseOrderByExpiresAtDesc(String username);
}