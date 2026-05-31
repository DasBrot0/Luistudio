package com.luistudio.reservas.dto.user;

import jakarta.validation.constraints.NotBlank;

public record UserStatusUpdateRequest(@NotBlank String status) {
}
