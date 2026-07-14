package com.luistudio.reservas.dto.admin;

import java.time.LocalDate;
import java.time.LocalTime;

public record AdminAttendanceResponse(
    Long bookingId,
    Long userId,
    String studentCode,
    String studentName,
    String studentEmail,
    Long roomId,
    String roomCode,
    String roomName,
    String campus,
    String pavilionCode,
    String pavilionName,
    String location,
    LocalDate date,
    LocalTime startTime,
    LocalTime endTime,
    String bookingStatus,
    String attendanceStatus
) {
}
