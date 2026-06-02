package com.luistudio.reservas.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TwoFactorVerifyInput(
    @NotBlank @Pattern(regexp = "^\\d{6}$") String code,
    boolean rememberMe
) {
}
