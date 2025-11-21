package com.aneto.authService.dto.response;

public record ErrorResponse(
        String message,
        int status
) {
}