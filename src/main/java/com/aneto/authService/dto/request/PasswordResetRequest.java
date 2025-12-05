package com.aneto.authService.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetRequest(
        @NotBlank(message = "O e-mail não pode estar vazio.")
        @Email(message = "O formato do e-mail é inválido.")
        // Opcional: Adicionar um limite de tamanho
        @Size(max = 100, message = "O e-mail deve ter no máximo 100 caracteres.")
        String email,
        String token,
        String newPassword
) {
}