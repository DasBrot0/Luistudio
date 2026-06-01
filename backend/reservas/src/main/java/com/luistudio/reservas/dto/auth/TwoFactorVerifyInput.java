package com.luistudio.reservas.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TwoFactorVerifyInput(
    @NotBlank @Size(max = 2048) String provisionalToken,
    @NotBlank @Pattern(regexp = "^\\d{6}$") String code
) {
}
