package com.aneto.authService.security;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenUtil jwtTokenUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                String username = jwtTokenUtil.extractUsername(token);
                List<?> roles = jwtTokenUtil.extractClaim(token, c -> c.get("roles", List.class));

                List<GrantedAuthority> authorities = (roles == null ? List.<Object>of() : roles)
                        .stream()
                        .map(Object::toString)
                        .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                        .<GrantedAuthority>map(SimpleGrantedAuthority::new)
                        .toList();

                var auth = new UsernamePasswordAuthenticationToken(username, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(auth);

            } catch (JwtException | IllegalArgumentException e) {
                SecurityContextHolder.clearContext(); // token inválido ou expirado: fica anónimo
            }
        }
        chain.doFilter(request, response);
    }
}