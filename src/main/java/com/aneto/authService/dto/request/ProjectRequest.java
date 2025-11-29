package com.aneto.authService.dto.request;

import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;

public record ProjectRequest(
        @NotBlank(message = "Username não pode ser vazio")
        String username,
        @NotBlank(message = "Username não pode ser vazio")
        String projectName,
        @Column(nullable = false)
        Double requiredHours
) {
}
