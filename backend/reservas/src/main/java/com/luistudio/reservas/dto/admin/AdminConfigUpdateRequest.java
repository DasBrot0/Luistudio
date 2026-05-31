package com.luistudio.reservas.dto.admin;

import jakarta.validation.constraints.Min;

public record AdminConfigUpdateRequest(
    @Min(1) int maxActiveBookings,
    @Min(30) int maxDurationMinutes
) {
}
