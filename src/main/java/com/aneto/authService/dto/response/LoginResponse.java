package com.aneto.authService.dto.response;

import java.util.List;

public record LoginResponse(
        String message,
        String token,
        String googleToken,
        String role,
        List<String> allowedModules) {
}
