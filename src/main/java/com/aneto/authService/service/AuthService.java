package com.aneto.authService.service;


import com.aneto.authService.models.Users;

public interface AuthService {

     Users registrarUsers(Users users) ;
    /**
     * Busca um usuário pelo username.
     * Exemplo: Usado em autenticação/login.
     */
     Users findPorUsername(String username) ;

    /**
     * Verifica se um usuário existe pelo nome de usuário.
     */
     boolean existeUsers(String username);
    void createPasswordResetTokenForUser(String email);

     String saveToken(Users users);

      /**
     * Valida o token e atualiza a password do utilizador.
     * @param token O token de recuperação fornecido.
     * @param newPassword A nova password em texto claro.
     * @throws IllegalArgumentException Se o token for inválido, nulo ou expirado.
     */
     void resetPassword(String token, String newPassword) ;
}
