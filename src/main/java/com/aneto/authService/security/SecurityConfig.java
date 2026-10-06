package com.aneto.authService.security;

import com.aneto.authService.service.impl.CustomUserDetailsService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final CustomUserDetailsService customUserDetailsService;
    private final JwtTokenUtil jwtTokenUtil;
    private final GatewayAuthFilter gatewayAuthFilter;

    // Chave partilhada entre serviços internos (nunca no código; vem da variável INTERNAL_API_KEY).
    // Sem "final": o @RequiredArgsConstructor só gera construtor para campos final.
    @Value("${internal.api-key:}")
    private String internalKey;

    @PostConstruct
    void avisarSeChaveInternaEmFalta() {
        if (internalKey == null || internalKey.isBlank()) {
            log.warn("internal.api-key vazia: chamadas internas com X-Internal-Key serão recusadas (401).");
        }
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(authProvider);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(Customizer.withDefaults())
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                // Rotas públicas (as mesmas do gateway)
                                "/api/auth/login", "/api/auth/register", "/api/auth/verify",
                                "/api/auth/verify-mfa", "/api/auth/google", "/api/auth/facebook",
                                "/api/auth/recuperar-password", "/api/auth/reset-password",
                                // Healthcheck do Coolify
                                "/actuator/health/**",
                                // Swagger: só fica acessível se springdoc estiver ligado
                                // (SPRINGDOC_ENABLED=true em local; desligado por omissão em produção)
                                "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html"
                        ).permitAll()
                        // Inclui /api/auth/telegram-id/**: exige JWT, gateway ou X-Internal-Key válida
                        .anyRequest().authenticated()
                )
                // Cada filtro é registado uma única vez
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenUtil),
                        UsernamePasswordAuthenticationFilter.class)
                .addFilterBefore(new InternalKeyFilter(internalKey), JwtAuthenticationFilter.class)
                // Fallback: pedidos do Gateway sem Authorization, com X-User-Id / X-User-Roles
                .addFilterAfter(gatewayAuthFilter, JwtAuthenticationFilter.class);

        return http.build();
    }
}