package com.aneto.authService.dto.response;

import com.aneto.authService.models.UserRole;

import java.util.List;

public record LoginResponse(
        String message,
        String token,
        String publicId,
        String googleToken,
        String role,         // Alterado para UserRole
        List<String> allowedModules,
        String email,
        String numeroSocio
) {}