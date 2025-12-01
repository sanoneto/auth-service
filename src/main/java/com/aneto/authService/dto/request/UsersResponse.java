package com.aneto.authService.dto.request;

import jakarta.persistence.Column;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record UsersResponse(
        @Column(unique = true, nullable = false, updatable = false)
        UUID publicId, // Não final, pois é gerado no @PrePersist

        @Column(unique = true, nullable = false)
        @NotBlank(message = "Username não pode ser vazio")
        String username,

        @Column(unique = true, nullable = false)
        @Email(message = "email inválido")
        String email,

        @Column(nullable = false)
        @NotBlank(message = "Role não pode ser vazia")
        String role

) {
}
