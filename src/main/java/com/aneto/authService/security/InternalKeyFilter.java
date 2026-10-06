package com.aneto.authService.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

@RequiredArgsConstructor
public class InternalKeyFilter extends OncePerRequestFilter {
    private final String internalKey;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res,
                                    FilterChain chain) throws ServletException, IOException {
        String sent = req.getHeader("X-Internal-Key");
        if (internalKey != null && !internalKey.isBlank() && sent != null
                && req.getRequestURI().startsWith("/api/auth/telegram-id/")
                && MessageDigest.isEqual(
                internalKey.getBytes(StandardCharsets.UTF_8),
                sent.getBytes(StandardCharsets.UTF_8))) {
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken("internal-service", null,
                            List.of(new SimpleGrantedAuthority("ROLE_INTERNAL"))));
        }
        chain.doFilter(req, res);
    }
}