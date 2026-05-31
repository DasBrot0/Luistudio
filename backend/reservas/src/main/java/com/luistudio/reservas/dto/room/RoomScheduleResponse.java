package com.luistudio.reservas.dto.room;

import java.time.LocalTime;

public record RoomScheduleResponse(
    int dayOfWeek,
    LocalTime openTime,
    LocalTime closeTime,
    boolean closed,
    boolean override
) {
}
