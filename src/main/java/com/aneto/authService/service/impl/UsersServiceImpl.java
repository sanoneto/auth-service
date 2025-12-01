package com.aneto.authService.service.impl;


import com.aneto.authService.dto.request.UsersResponse;
import com.aneto.authService.mapper.RequestMapper;
import com.aneto.authService.models.Users;

import com.aneto.authService.repository.UsersRepository;
import com.aneto.authService.service.UsersService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UsersServiceImpl implements UsersService {

    private final UsersRepository usersRepository; // Repositório dos usuários.
    private final PasswordEncoder passwordEncoder;     // Para a codificação da senha.
    private final RequestMapper requestMapper;

    @Override
    public Users registrarUsers(Users users) {
        users.setPassword(passwordEncoder.encode(users.getPassword()));
        return usersRepository.save(users);
    }

    @Override
    public Users findPorUsername(String username) throws UsernameNotFoundException {
        return usersRepository.findByUsername(username)
                // Se o utilizador não for encontrado na base de dados, esta exceção é lançada
                .orElseThrow(() -> new UsernameNotFoundException("Utilizador não encontrado com o nome: " + username));
    }

    @Override
    public boolean existeUsers(String username) {
        return usersRepository.findByUsername(username).isPresent();
    }

    public List<UsersResponse> findAll() {
                List <Users> userlist= usersRepository.findAll();
        return requestMapper.UsersResponse(userlist);
    }
}
