package com.aneto.authService.exception;

public class InvalidTokenException extends Throwable {
    public InvalidTokenException(String message) {
        super(message);
    }
}
