package com.aneto.authService.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.io.Serializable;


public record EmailRequest(
        @NotBlank
        @Size(max = 100)
        String name,

        @NotBlank
        @Email
        String email,
        String subject,
        @NotBlank
        @Size(max = 2000)
        String message,
        String resetLink
) implements Serializable { // Adicione 'implements Serializable'
}
