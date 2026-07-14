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
    String icsUrl,
    Integer roomUnitNumber,
    String roomUnitLabel
) {
    public BookingResponse(
        Long id, Long userId, String userEmail, Long roomId, String roomCode, String roomName,
        String location, Integer people, LocalDate date, LocalTime start, LocalTime end, String status,
        String attendanceStatus, String observation, String googleCalendarUrl, String icsUrl
    ) {
        this(id, userId, userEmail, roomId, roomCode, roomName, location, people, date, start, end,
            status, attendanceStatus, observation, googleCalendarUrl, icsUrl, null, null);
    }

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
            status, null, observation, googleCalendarUrl, icsUrl, null, null);
    }
}
