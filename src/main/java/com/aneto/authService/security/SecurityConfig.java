
package com.aneto.authService.security;

import com.aneto.authService.service.impl.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;

    @Value("${app.security.cors.allowed-origins}")
    private List<String> allowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configura o AuthenticationManager que usa o CustomUserDetailsService e o PasswordEncoder.
     */
    @Bean
    public AuthenticationManager authenticationManager() {
        // Usa o DaoAuthenticationProvider com o CustomUserDetailsService e o PasswordEncoder
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return new ProviderManager(authProvider);
    }
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .cors(Customizer.withDefaults())

                // 2. Desabilita CSRF (Essencial para APIs REST Stateless)
                .csrf(AbstractHttpConfigurer::disable)

                // 3. Define a política de sessão como Stateless (Fundamental para JWT)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 5. Define regras de autorização
                .authorizeHttpRequests(authorize -> authorize
                        // Permite TODOS os requests OPTIONS (Necessário para o CORS Pre-flight)
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // Rotas ABERTAS: Login, Registo e documentação (Swagger/OpenAPI)
                        // Permite "/api/auth/**" (login, register, etc.)
                        .requestMatchers("/api/auth/**", "/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html", "/api-docs/**").permitAll()

                        // ⚠️ Nota: A rota "api/v1/projects/**" também está aqui, se for um erro de cópia, remova-a,
                        // pois a rota de projetos não deveria estar neste serviço.
                         .requestMatchers("api/v1/projects/**","/api/v1/users/**").permitAll()

                        // Qualquer outra rota neste serviço DEVE ser protegida (e só pode ser acedida com um JWT válido,
                        // embora este serviço seja primariamente o criador de tokens).
                        .anyRequest().authenticated()
                );

        return http.build();
    }
}