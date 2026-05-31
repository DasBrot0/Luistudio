package com.luistudio.reservas.dto.room;

import java.time.LocalTime;

public record RoomScheduleInput(
    int dayOfWeek,
    LocalTime openTime,
    LocalTime closeTime,
    boolean closed
) {
}
