package com.aneto.authService.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record VaultEntryResponse(
        UUID publicId,
        String title,
        String category,
        String accountUsername,
        String password,
        String url,
        String notes,
        boolean favorite,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}