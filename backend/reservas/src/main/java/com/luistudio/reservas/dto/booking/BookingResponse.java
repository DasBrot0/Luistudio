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
    String attendanceStatus,
    String observation,
    String googleCalendarUrl,
    String icsUrl
) {
    public BookingResponse(
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
        String observation,
        String googleCalendarUrl,
        String icsUrl
    ) {
        this(id, userId, userEmail, roomId, roomCode, roomName, location, people, date, start, end,
            status, null, observation, googleCalendarUrl, icsUrl);
    }
}
