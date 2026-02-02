package com.aneto.authService.dto.response;

public record ProjectResponse(String projectName,
                              Double requiredHours,
                              Double hourlyRate) {
}
