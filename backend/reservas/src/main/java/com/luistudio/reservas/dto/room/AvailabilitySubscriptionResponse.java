package com.luistudio.reservas.dto.room;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

public record AvailabilitySubscriptionResponse(
    Long id,
    Long roomId,
    String roomName,
    LocalDate targetDate,
    LocalTime startTime,
    LocalTime endTime,
    String status,
    OffsetDateTime createdAt
) {
}
