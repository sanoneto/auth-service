package com.aneto.authService.service.impl;

import com.aneto.authService.models.Users;
import com.aneto.authService.models.UserRole;
import com.aneto.authService.repository.UsersRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private static final Logger log = LoggerFactory.getLogger(CustomUserDetailsService.class);
    private final UsersRepository usersRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Users user = usersRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + username));

        log.info("Carregando usuário: {}", user.getUsername());

        Collection<? extends GrantedAuthority> authorities = buildAuthorities(user);

        log.info("Role do banco: {}", user.getRole());
        log.info("Authorities configuradas: {}", authorities);

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities(authorities)
                .disabled(!user.isEnabled()) // Garante que usuários desabilitados não loguem
                .build();
    }

    /**
     * Constrói a coleção de GrantedAuthority para o usuário usando o Enum UserRole.
     * Normaliza a role (adiciona prefixo ROLE_ se necessário).
     *
     * @param user usuário carregado do repositório
     * @return lista imutável com as authorities
     * @throws UsernameNotFoundException quando role estiver ausente
     */
    private Collection<? extends GrantedAuthority> buildAuthorities(Users user) {
        UserRole role = user.getRole();

        if (role == null) {
            log.error("Role ausente para usuário: {}", user.getUsername());
            throw new UsernameNotFoundException("Usuário sem role definida: " + user.getUsername());
        }

        // Converte o nome do Enum (ex: ADMIN) para String
        String roleName = role.name().toUpperCase(Locale.ROOT);

        if (!roleName.startsWith("ROLE_")) {
            roleName = "ROLE_" + roleName;
        }

        log.debug("Authority final gerada para usuário {}: {}", user.getUsername(), roleName);
        return List.of(new SimpleGrantedAuthority(roleName));
    }
}