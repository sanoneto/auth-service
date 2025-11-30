package com.aneto.authService.service;


import com.aneto.authService.models.Users;


import java.util.Optional;

public interface UsersService {

    public Users registrarUsers(Users users) ;
    /**
     * Busca um usuário pelo username.
     * Exemplo: Usado em autenticação/login.
     */
    public Users findPorUsername(String username) ;

    /**
     * Verifica se um usuário existe pelo nome de usuário.
     */
    public boolean existeUsers(String username);
}
