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
import java.util.function.Function;

@Component
public class JwtTokenUtil {
    private static final Logger log = LoggerFactory.getLogger(JwtTokenUtil.class);

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    private SecretKey finalKey;
    private static final String ROLES_CLAIM = "roles";

    @PostConstruct
    public void init() {
        if (secret == null || secret.length() < 32) {
            log.error("Chave secreta JWT ausente ou muito curta!");
            throw new IllegalStateException("Chave secreta inválida. Mínimo 32 caracteres.");
        }
        this.finalKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        log.info("JwtTokenUtil inicializado com sucesso.");
    }

    public String generateToken(String username, List<String> roles) {
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

    // --- MÉTODOS ADICIONADOS PARA RESOLVER O ERRO NO AUTHSERVICE ---

    /**
     * Extrai o nome de utilizador (Subject) do token.
     */
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Extrai a data de expiração do token.
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Método genérico para extrair qualquer Claim.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(finalKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public long getExpirationMillis() {
        return expiration != null ? expiration : 0L;
    }
}