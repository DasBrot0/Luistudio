package com.luistudio.reservas.dto.room;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;

public record MaintenanceRequest(
    @NotNull OffsetDateTime start,
    @NotNull OffsetDateTime end,
    @NotBlank @Size(max = 255) String reason
) {
}
