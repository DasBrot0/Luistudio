package com.luistudio.reservas.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetConfirmInput(
    @NotBlank String token,
    @NotBlank @Size(min = 8, max = 128) String newPassword
) {
}
