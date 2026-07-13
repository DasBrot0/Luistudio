package com.luistudio.reservas.dto.admin;

import java.time.LocalDate;
import java.util.List;

public record AdminDashboardResponse(
    LocalDate from,
    LocalDate to,
    long totalReservations,
    double absenceRate,
    long absenceCount,
    long attendanceEligibleCount,
    long attendanceCount,
    long pendingAttendanceCount,
    List<RoomOccupancy> occupancyByRoom,
    List<PeakHour> peakHours,
    List<DailyOccupancy> dailyOccupancy,
    List<HeatmapCell> weeklyHeatmap,
    List<StudentRanking> topStudents
) {
    public record RoomOccupancy(
        Long roomId,
        String roomCode,
        String roomName,
        long reservedMinutes,
        long availableMinutes,
        double occupancyRate
    ) {}

    public record PeakHour(int hour, long reservedMinutes, long reservationCount) {}

    public record DailyOccupancy(
        LocalDate date,
        long reservedMinutes,
        long availableMinutes,
        double occupancyRate
    ) {}

    public record HeatmapCell(
        int dayOfWeek,
        int hour,
        long reservedMinutes,
        long availableMinutes,
        double occupancyRate
    ) {}

    public record StudentRanking(
        Long userId,
        String code,
        String fullName,
        String email,
        long reservationCount,
        long reservedMinutes,
        long absenceCount
    ) {}
}
