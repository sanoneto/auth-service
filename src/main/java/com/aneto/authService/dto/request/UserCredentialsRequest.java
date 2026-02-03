package com.aneto.authService.dto.request;

import com.aneto.authService.models.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

@Schema(description = "Request para criar Usuário")
public record UserCredentialsRequest(

        @Schema(
                description = "Username necessário",
                example = "bento",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "O username é obrigatório")
        @Size(min = 2, max = 50, message = "O username deve ter entre 2 e 50 caracteres")
        @Pattern(regexp = "^[A-Za-zÀ-ÿ\\s]+$", message = "O username deve conter apenas letras")
        String username,

        @Schema(
                description = "Senha do usuário",
                example = "sdr1233",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 8, max = 100, message = "A senha deve ter entre 8 e 100 caracteres")
        String password,

        @Schema(
                description = "O seu cargo/role no sistema",
                example = "ADMIN",
                implementation = UserRole.class,
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotNull(message = "O role é obrigatório (ex: ADMIN, ESTAGIARIO)")
        UserRole role,

        @Schema(
                description = "O seu e-mail institucional ou pessoal",
                example = "usuario@aneto.com",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "O e-mail deve ter um formato válido.")
        String email,

        @Schema(description = "Código de convite opcional", example = "INV-2026")
        String inviteCode,

        @Schema(description = "Código de verificação ou MFA", example = "123456")
        String code
) {
}