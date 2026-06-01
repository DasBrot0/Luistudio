package com.luistudio.reservas.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserStatusUpdateRequest(@NotBlank @Size(max = 20) String status) {
}
