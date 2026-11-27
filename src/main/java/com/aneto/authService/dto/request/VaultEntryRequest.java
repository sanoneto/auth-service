package com.aneto.authService.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VaultEntryRequest(
        @NotBlank(message = "O título é obrigatório.")
        @Size(max = 150)
        String title,

        @Size(max = 60)
        String category,

        @Size(max = 200)
        String accountUsername,

        @NotBlank(message = "A palavra-passe é obrigatória.")
        @Size(max = 500)
        String password,

        @Size(max = 500)
        String url,

        @Size(max = 2000)
        String notes,

        Boolean favorite
) {}