package com.aneto.authService.dto.response;

import com.aneto.authService.models.UserRole;

import java.util.List;

public record LoginResponse(
        String message,
        String token,
        String googleToken,
        UserRole role,         // Alterado para UserRole
        List<String> allowedModules
) {}