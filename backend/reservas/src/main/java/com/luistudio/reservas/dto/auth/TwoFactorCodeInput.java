package com.luistudio.reservas.dto.auth;

import jakarta.validation.constraints.NotBlank;

public record TwoFactorCodeInput(@NotBlank String code) {
}
