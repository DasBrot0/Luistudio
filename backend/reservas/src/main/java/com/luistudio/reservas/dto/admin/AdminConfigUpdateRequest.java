package com.luistudio.reservas.dto.admin;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.AssertTrue;

public record AdminConfigUpdateRequest(
    @Min(1) int maxActiveBookings,
    @Min(30) int maxDurationMinutes
) {
    @AssertTrue(message = "La duración de reserva debe ser de 30 o 60 minutos")
    public boolean isDurationValid() {
        return maxDurationMinutes == 30 || maxDurationMinutes == 60;
    }
}
