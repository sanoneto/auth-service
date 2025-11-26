package com.aneto.authService.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "Request para create User")
public record UserCredentialsRequest(

        @Schema(
                description = "username necessario",
                example = "bento",
                required = true

        )
        @NotBlank(message = "O primeiro nome é obrigatório")
        @Size(min = 2, max = 50, message = "Nome deve ter entre 2 e 50 caracteres")
        @Pattern(regexp = "^[A-Za-zÀ-ÿ\\s]+$", message = "Nome deve conter apenas letras")
        String username,

        @Schema(
                description = "Senha do usuário",
                example = "sdr1233",
                required = true
        )
        @NotBlank(message = "Senha é obrigatória")
        @Size(min = 8, max = 100, message = "Senha deve ter entre 8 e 100 caracteres")
        String password,

        @Schema(
                description = "o teu role ",
                example = "ADMIN ou ESTAGIARIO",
                required = true
        )
        @NotBlank(message = "O primeiro nome é obrigatório")
        @Size(min = 2, max = 50, message = "Nome deve ter entre 2 e 50 caracteres")
        @NotEmpty String role,

        @Schema(
                description = "o teu email ",
                example = "ADMIN ou ESTAGIARIO",
                required = true
        )
        @NotBlank(message = "O e-mail é obrigatório.")
        // (Ex: deve conter um '@' e um '.' no domínio)
        @Email(message = "O e-mail deve ter um formato válido.")
        @NotEmpty String email,

        @Schema(
                description = "Horas necessárias para o usuário",
                example = "400.0",
                required = true
        )
        @NotNull(message = "Required hours cannot be null")
        @Min(value = 1, message = "Required hours must be at least 1")
        Double requiredHours) {


}
