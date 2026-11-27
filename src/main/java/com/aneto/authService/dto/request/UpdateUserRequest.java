package com.aneto.authService.dto.request;

import com.aneto.authService.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateUserRequest(
        @NotBlank(message = "O nome de utilizador é obrigatório")
        String username,

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "Formato de e-mail inválido")
        String email,

        UserRole role,

        String password
) {
}