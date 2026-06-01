package com.luistudio.reservas.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetRequestInput(@NotBlank @Email @Size(max = 160) String email) {
}
