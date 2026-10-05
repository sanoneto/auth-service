package com.aneto.authService.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * Constrói o contexto de segurança a partir dos headers injetados pelo Gateway (JwtAuthFilter),
 * que já validou o JWT e remove o header Authorization antes de reencaminhar.
 * Corre depois do JwtAuthenticationFilter e só atua se este não tiver autenticado o pedido.
 * Mesmo padrão usado no registo-horas-service e no email-service.
 */
@Component
public class GatewayAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(GatewayAuthFilter.class);
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ROLES_HEADER = "X-User-Roles";

    /**
     * Ignora rotas do Actuator e Documentação para não barrar o Health Check.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getServletPath();
        return path.startsWith("/actuator") ||
                path.startsWith("/swagger-ui") ||
                path.startsWith("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Um Bearer válido (JwtAuthenticationFilter) tem prioridade sobre os headers
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            filterChain.doFilter(request, response);
            return;
        }

        String userId = request.getHeader(USER_ID_HEADER);
        String rolesString = request.getHeader(USER_ROLES_HEADER);

        if (userId != null && !userId.isBlank() && rolesString != null && !rolesString.isBlank()) {
            Collection<SimpleGrantedAuthority> authorities = Arrays.stream(rolesString.split(","))
                    .map(String::trim)
                    .filter(role -> !role.isEmpty())
                    .map(role -> role.toUpperCase(Locale.ROOT))
                    .map(role -> new SimpleGrantedAuthority(role.startsWith("ROLE_") ? role : "ROLE_" + role))
                    .collect(Collectors.toList());

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userId, null, authorities);

            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("Autenticação configurada para o user: {} {}", userId, authorities);
        } else {
            // Normal nas rotas públicas (login, registo, ...); as rotas protegidas são barradas pelos @PreAuthorize
            log.debug("Pedido sem headers do Gateway: {}", request.getRequestURI());
        }

        filterChain.doFilter(request, response);
    }
}
