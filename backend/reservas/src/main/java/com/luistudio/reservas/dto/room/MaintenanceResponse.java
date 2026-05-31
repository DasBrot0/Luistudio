package com.luistudio.reservas.dto.room;

import java.time.OffsetDateTime;

public record MaintenanceResponse(
    Long id,
    OffsetDateTime start,
    OffsetDateTime end,
    String reason,
    String status
) {
}
