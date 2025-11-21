package com.aneto.authService.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

@Component
public class JwtTokenUtil {
    private static final Logger log = LoggerFactory.getLogger(JwtTokenUtil.class);

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    private SecretKey finalKey; // A chave secreta final e uniforme
    private static final String ROLES_CLAIM = "roles";

    /**
     * Inicializa a chave secreta APÓS a injeção do valor de '${jwt.secret}'.
     * Garante a derivação uniforme para todos os serviços.
     */
    @PostConstruct
    public void init() {
        if (secret == null || secret.length() < 32) {
            log.error("Chave secreta JWT ausente ou muito curta!");
            throw new IllegalStateException("Chave secreta inválida. Mínimo 32 caracteres.");
        }
        // CORRIGIDO: Usa a string secreta diretamente, sem MessageDigest
        this.finalKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        log.info("JwtTokenUtil inicializado com sucesso.");
    }

    public String generateToken(String username, List<String> roles) {
        // Validação básica para garantir que a chave foi inicializada
        if (finalKey == null) {
            throw new IllegalStateException("Chave JWT não inicializada.");
        }

        return Jwts.builder()
                .setSubject(username)
                .claim(ROLES_CLAIM, roles)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(finalKey)
                .compact();
    }
    // Novo método exposto para obter a expiração em milissegundos
    public long getExpirationMillis() {
        return expiration != null ? expiration : 0L;
    }
}