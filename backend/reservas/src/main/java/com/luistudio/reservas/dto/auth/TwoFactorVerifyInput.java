package com.luistudio.reservas.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record TwoFactorVerifyInput(
    @NotBlank String provisionalToken,
    @NotBlank String code
) {
}
