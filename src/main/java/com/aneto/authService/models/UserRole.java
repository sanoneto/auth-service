package com.aneto.authService.models;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Papéis/Roles disponíveis no sistema")
public enum UserRole {
    ADMIN,
    ESTAGIARIO,
    USER,
    GESTOR;

    // Opcional: Método para converter String para Enum de forma segura
    public static UserRole fromString(String value) {
        for (UserRole role : UserRole.values()) {
            if (role.name().equalsIgnoreCase(value)) {
                return role;
            }
        }
        throw new IllegalArgumentException("Role inválido: " + value);
    }
}