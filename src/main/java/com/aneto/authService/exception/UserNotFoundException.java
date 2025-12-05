package com.aneto.authService.exception;

public class UserNotFoundException extends Exception {

    // Construtor padrão
    public UserNotFoundException() {
        super("O utilizador especificado não foi encontrado.");
    }

    // Construtor que aceita uma mensagem personalizada
    public UserNotFoundException(String message) {
        super(message);
    }

    // Construtor que aceita uma mensagem e a causa original
    public UserNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}