package com.luistudio.reservas.dto.room;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record MaintenanceRequest(
    @NotNull OffsetDateTime start,
    @NotNull OffsetDateTime end,
    @NotBlank String reason
) {
}
