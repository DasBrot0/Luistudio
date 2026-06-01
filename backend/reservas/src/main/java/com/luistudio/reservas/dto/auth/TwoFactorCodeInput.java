package com.luistudio.reservas.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record TwoFactorCodeInput(@NotBlank @Pattern(regexp = "^\\d{6}$") String code) {
}
