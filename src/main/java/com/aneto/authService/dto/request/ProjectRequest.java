package com.aneto.authService.dto.request;

import jakarta.persistence.Column;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotBlank;
import jakarta.persistence.Column;

public record ProjectRequest(
        @NotBlank(message = "Username não pode ser vazio")
        String username,

        @NotBlank(message = "Project Name não pode ser vazio")
        String projectName,

        Double requiredHours,

        @Column(nullable = false)
        Double hourlyRate,

        String color // Adiciona este campo
) {
    /**
     * Construtor Compacto para validação e valores padrão.
     * Ele não precisa de parênteses com os parâmetros.
     */
    public ProjectRequest {
        // Se o hourlyRate for nulo, define como 1.1
        if (hourlyRate == null) {
            hourlyRate = 1.0;
        }
    }
}