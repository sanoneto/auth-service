package com.aneto.authService.dto.response;

import jakarta.persistence.Column;

public record ProjectResponse(String projectName,
                              @Column(nullable = false)
                              Double requiredHours) {
}
