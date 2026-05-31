package com.luistudio.reservas.dto.booking;

import java.time.LocalDate;
import java.time.LocalTime;

public record BookingResponse(
    Long id,
    Long userId,
    String userEmail,
    Long roomId,
    String roomCode,
    String roomName,
    String location,
    Integer people,
    LocalDate date,
    LocalTime start,
    LocalTime end,
    String status,
    String googleCalendarUrl,
    String icsUrl
) {
}
