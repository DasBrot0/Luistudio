package com.luistudio.reservas.service;

import com.luistudio.reservas.dto.admin.AdminDashboardResponse;
import com.luistudio.reservas.exception.BusinessException;
import com.luistudio.reservas.model.CampusScheduleEntity;
import com.luistudio.reservas.model.ReservationEntity;
import com.luistudio.reservas.model.ReservationStatus;
import com.luistudio.reservas.model.RoomEntity;
import com.luistudio.reservas.model.RoomScheduleEntity;
import com.luistudio.reservas.model.RoomState;
import com.luistudio.reservas.model.UserEntity;
import com.luistudio.reservas.repository.ReservationRepository;
import com.luistudio.reservas.repository.CampusScheduleRepository;
import com.luistudio.reservas.repository.RoomRepository;
import com.luistudio.reservas.repository.RoomScheduleRepository;
import com.luistudio.reservas.util.AppTime;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminDashboardService {
    private static final int MAX_RANGE_DAYS = 366;
    private static final int ATTENDANCE_TOLERANCE_MINUTES = 15;

    private final ReservationRepository reservationRepository;
    private final RoomRepository roomRepository;
    private final RoomScheduleRepository roomScheduleRepository;
    private final CampusScheduleRepository campusScheduleRepository;

    public AdminDashboardService(
        ReservationRepository reservationRepository,
        RoomRepository roomRepository,
        RoomScheduleRepository roomScheduleRepository,
        CampusScheduleRepository campusScheduleRepository
    ) {
        this.reservationRepository = reservationRepository;
        this.roomRepository = roomRepository;
        this.roomScheduleRepository = roomScheduleRepository;
        this.campusScheduleRepository = campusScheduleRepository;
    }

    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard(LocalDate from, LocalDate to) {
        validateRange(from, to);
        List<ReservationEntity> reservations = reservationRepository.findForDashboard(from, to).stream()
            .filter(item -> item.getEstado() != ReservationStatus.CANCELADA)
            .toList();
        List<RoomEntity> rooms = roomRepository.findByEstadoNot(RoomState.INACTIVA);
        Map<Long, List<RoomScheduleEntity>> schedulesByRoom = roomScheduleRepository
            .findBySalaIdIn(rooms.stream().map(RoomEntity::getId).toList())
            .stream()
            .collect(Collectors.groupingBy(schedule -> schedule.getSala().getId()));
        List<Long> campusIds = rooms.stream()
            .filter(room -> room.getPabellon() != null && room.getPabellon().getCampus() != null)
            .map(room -> room.getPabellon().getCampus().getId())
            .distinct()
            .toList();
        Map<Long, List<CampusScheduleEntity>> schedulesByCampus = campusIds.isEmpty()
            ? Map.of()
            : campusScheduleRepository.findByCampus_IdIn(campusIds).stream()
                .collect(Collectors.groupingBy(schedule -> schedule.getCampus().getId()));

        List<AdminDashboardResponse.RoomOccupancy> occupancy = rooms.stream()
            .map(room -> occupancy(room, reservations, schedulesByRoom.getOrDefault(room.getId(), List.of()),
                campusSchedules(room, schedulesByCampus), from, to))
            .sorted(Comparator.comparingDouble(AdminDashboardResponse.RoomOccupancy::occupancyRate).reversed()
                .thenComparing(AdminDashboardResponse.RoomOccupancy::roomCode))
            .toList();

        AttendanceSummary attendance = attendanceSummary(reservations);
        return new AdminDashboardResponse(
            from,
            to,
            reservations.size(),
            percentage(attendance.absences(), attendance.eligible()),
            attendance.absences(),
            attendance.eligible(),
            attendance.eligible() - attendance.absences(),
            Math.max(0, reservations.size() - attendance.eligible()),
            occupancy,
            peakHours(reservations),
            dailyOccupancy(reservations, rooms, schedulesByRoom, schedulesByCampus, from, to),
            weeklyHeatmap(reservations, rooms, schedulesByRoom, schedulesByCampus, from, to),
            topStudents(reservations)
        );
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from == null || to == null || from.isAfter(to)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "El rango de fechas no es válido");
        }
        if (Duration.between(from.atStartOfDay(), to.plusDays(1).atStartOfDay()).toDays() > MAX_RANGE_DAYS) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "El rango máximo permitido es de 366 días");
        }
    }

    private AdminDashboardResponse.RoomOccupancy occupancy(
        RoomEntity room,
        List<ReservationEntity> reservations,
        List<RoomScheduleEntity> schedules,
        List<CampusScheduleEntity> campusSchedules,
        LocalDate from,
        LocalDate to
    ) {
        long reserved = reservations.stream()
            .filter(item -> item.getSala().getId().equals(room.getId()))
            .mapToLong(this::durationMinutes)
            .sum();
        long available = from.datesUntil(to.plusDays(1))
            .mapToLong(date -> scheduleMinutes(resolveSchedule(schedules, campusSchedules, toDatabaseDay(date.getDayOfWeek()))))
            .sum();
        return new AdminDashboardResponse.RoomOccupancy(
            room.getId(), room.getCodigo(), room.getNombre(), reserved, available, percentage(reserved, available)
        );
    }

    private List<AdminDashboardResponse.PeakHour> peakHours(List<ReservationEntity> reservations) {
        Map<Integer, HourAccumulator> hours = new HashMap<>();
        for (ReservationEntity reservation : reservations) {
            int firstHour = reservation.getHoraInicio().getHour();
            int lastHour = reservation.getHoraFin().minusNanos(1).getHour();
            for (int hour = firstHour; hour <= lastHour; hour++) {
                LocalTime bucketStart = LocalTime.of(hour, 0);
                LocalTime bucketEnd = bucketStart.plusHours(1);
                LocalTime overlapStart = reservation.getHoraInicio().isAfter(bucketStart) ? reservation.getHoraInicio() : bucketStart;
                LocalTime overlapEnd = reservation.getHoraFin().isBefore(bucketEnd) ? reservation.getHoraFin() : bucketEnd;
                long minutes = Math.max(0, Duration.between(overlapStart, overlapEnd).toMinutes());
                if (minutes > 0) hours.computeIfAbsent(hour, ignored -> new HourAccumulator()).add(minutes);
            }
        }
        return hours.entrySet().stream()
            .map(entry -> new AdminDashboardResponse.PeakHour(
                entry.getKey(), entry.getValue().reservedMinutes, entry.getValue().reservationCount
            ))
            .sorted(Comparator.comparingLong(AdminDashboardResponse.PeakHour::reservedMinutes).reversed()
                .thenComparingInt(AdminDashboardResponse.PeakHour::hour))
            .limit(5)
            .toList();
    }

    private List<AdminDashboardResponse.StudentRanking> topStudents(List<ReservationEntity> reservations) {
        Map<Long, List<ReservationEntity>> byUser = reservations.stream()
            .filter(item -> item.getUsuario() != null && item.getUsuario().getRol() != null)
            .filter(item -> "ESTUDIANTE".equalsIgnoreCase(item.getUsuario().getRol().getNombre()))
            .collect(Collectors.groupingBy(item -> item.getUsuario().getId()));
        List<AdminDashboardResponse.StudentRanking> result = new ArrayList<>();
        byUser.values().forEach(items -> {
            UserEntity user = items.getFirst().getUsuario();
            result.add(new AdminDashboardResponse.StudentRanking(
                user.getId(), user.getCodigo(), (user.getNombres() + " " + user.getApellidos()).trim(), user.getCorreo(),
                items.size(), items.stream().mapToLong(this::durationMinutes).sum(),
                items.stream().filter(item -> "INASISTIO".equals(item.getAttendanceStatus())).count()
            ));
        });
        return result.stream()
            .sorted(Comparator.comparingLong(AdminDashboardResponse.StudentRanking::reservationCount).reversed()
                .thenComparing(Comparator.comparingLong(AdminDashboardResponse.StudentRanking::reservedMinutes).reversed())
                .thenComparing(AdminDashboardResponse.StudentRanking::code))
            .limit(10)
            .toList();
    }

    private AttendanceSummary attendanceSummary(List<ReservationEntity> reservations) {
        LocalDate today = AppTime.today();
        LocalTime cutoff = AppTime.nowTime().minusMinutes(ATTENDANCE_TOLERANCE_MINUTES);
        List<ReservationEntity> eligible = reservations.stream()
            .filter(item -> item.getFecha().isBefore(today)
                || (item.getFecha().isEqual(today) && !item.getHoraInicio().isAfter(cutoff)))
            .toList();
        long absences = eligible.stream().filter(item -> "INASISTIO".equals(item.getAttendanceStatus())).count();
        return new AttendanceSummary(absences, eligible.size());
    }

    private List<AdminDashboardResponse.DailyOccupancy> dailyOccupancy(
        List<ReservationEntity> reservations,
        List<RoomEntity> rooms,
        Map<Long, List<RoomScheduleEntity>> schedulesByRoom,
        Map<Long, List<CampusScheduleEntity>> schedulesByCampus,
        LocalDate from,
        LocalDate to
    ) {
        return from.datesUntil(to.plusDays(1)).map(date -> {
            long reserved = reservations.stream()
                .filter(item -> item.getFecha().equals(date))
                .mapToLong(this::durationMinutes)
                .sum();
            long available = rooms.stream().mapToLong(room -> {
                ScheduleWindow schedule = resolveSchedule(
                    schedulesByRoom.getOrDefault(room.getId(), List.of()),
                    campusSchedules(room, schedulesByCampus),
                    toDatabaseDay(date.getDayOfWeek())
                );
                return scheduleMinutes(schedule);
            }).sum();
            return new AdminDashboardResponse.DailyOccupancy(date, reserved, available, percentage(reserved, available));
        }).toList();
    }

    private List<AdminDashboardResponse.HeatmapCell> weeklyHeatmap(
        List<ReservationEntity> reservations,
        List<RoomEntity> rooms,
        Map<Long, List<RoomScheduleEntity>> schedulesByRoom,
        Map<Long, List<CampusScheduleEntity>> schedulesByCampus,
        LocalDate from,
        LocalDate to
    ) {
        Map<String, Long> reserved = new HashMap<>();
        Map<String, Long> available = new HashMap<>();
        for (ReservationEntity reservation : reservations) {
            accumulateHours(reservation.getFecha().getDayOfWeek().getValue(), reservation.getHoraInicio(), reservation.getHoraFin(),
                (key, minutes) -> reserved.merge(key, minutes, Long::sum));
        }
        from.datesUntil(to.plusDays(1)).forEach(date -> rooms.forEach(room -> {
            ScheduleWindow schedule = resolveSchedule(
                schedulesByRoom.getOrDefault(room.getId(), List.of()),
                campusSchedules(room, schedulesByCampus),
                toDatabaseDay(date.getDayOfWeek())
            );
            if (schedule != null && !schedule.closed() && schedule.open() != null && schedule.close() != null) {
                accumulateHours(date.getDayOfWeek().getValue(), schedule.open(), schedule.close(),
                    (key, minutes) -> available.merge(key, minutes, Long::sum));
            }
        }));
        return available.entrySet().stream().map(entry -> {
            String[] parts = entry.getKey().split("-");
            int day = Integer.parseInt(parts[0]);
            int hour = Integer.parseInt(parts[1]);
            long reservedMinutes = reserved.getOrDefault(entry.getKey(), 0L);
            return new AdminDashboardResponse.HeatmapCell(day, hour, reservedMinutes, entry.getValue(), percentage(reservedMinutes, entry.getValue()));
        }).sorted(Comparator.comparingInt(AdminDashboardResponse.HeatmapCell::hour)
            .thenComparingInt(AdminDashboardResponse.HeatmapCell::dayOfWeek)).toList();
    }

    private void accumulateHours(int dayOfWeek, LocalTime start, LocalTime end, java.util.function.BiConsumer<String, Long> consumer) {
        int firstHour = start.getHour();
        int lastHour = end.minusNanos(1).getHour();
        for (int hour = firstHour; hour <= lastHour; hour++) {
            LocalTime bucketStart = LocalTime.of(hour, 0);
            LocalTime bucketEnd = bucketStart.plusHours(1);
            LocalTime overlapStart = start.isAfter(bucketStart) ? start : bucketStart;
            LocalTime overlapEnd = end.isBefore(bucketEnd) ? end : bucketEnd;
            long minutes = Math.max(0, Duration.between(overlapStart, overlapEnd).toMinutes());
            if (minutes > 0) consumer.accept(dayOfWeek + "-" + hour, minutes);
        }
    }

    private long durationMinutes(ReservationEntity reservation) {
        return Duration.between(reservation.getHoraInicio(), reservation.getHoraFin()).toMinutes();
    }

    private List<CampusScheduleEntity> campusSchedules(
        RoomEntity room,
        Map<Long, List<CampusScheduleEntity>> schedulesByCampus
    ) {
        if (room.getPabellon() == null || room.getPabellon().getCampus() == null) return List.of();
        return schedulesByCampus.getOrDefault(room.getPabellon().getCampus().getId(), List.of());
    }

    private ScheduleWindow resolveSchedule(
        List<RoomScheduleEntity> roomSchedules,
        List<CampusScheduleEntity> campusSchedules,
        int dayOfWeek
    ) {
        return roomSchedules.stream()
            .filter(item -> item.getDiaSemana().equals(dayOfWeek))
            .findFirst()
            .map(item -> new ScheduleWindow(Boolean.TRUE.equals(item.getCerrado()), item.getHoraApertura(), item.getHoraCierre()))
            .orElseGet(() -> campusSchedules.stream()
                .filter(item -> item.getDiaSemana().equals(dayOfWeek))
                .findFirst()
                .map(item -> new ScheduleWindow(Boolean.TRUE.equals(item.getCerrado()), item.getHoraApertura(), item.getHoraCierre()))
                .orElse(null));
    }

    private long scheduleMinutes(ScheduleWindow schedule) {
        if (schedule == null || schedule.closed() || schedule.open() == null || schedule.close() == null) return 0;
        return Math.max(0, Duration.between(schedule.open(), schedule.close()).toMinutes());
    }

    private int toDatabaseDay(DayOfWeek day) {
        return day.getValue();
    }

    private double percentage(long numerator, long denominator) {
        if (denominator <= 0) return 0;
        return Math.round((numerator * 10000.0) / denominator) / 100.0;
    }

    private record AttendanceSummary(long absences, long eligible) {}
    private record ScheduleWindow(boolean closed, LocalTime open, LocalTime close) {}

    private static final class HourAccumulator {
        private long reservedMinutes;
        private long reservationCount;

        void add(long minutes) {
            reservedMinutes += minutes;
            reservationCount++;
        }
    }
}
