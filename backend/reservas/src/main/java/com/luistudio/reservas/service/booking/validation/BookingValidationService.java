package com.luistudio.reservas.service.booking.validation;

import com.luistudio.reservas.dto.booking.BookingUpsertRequest;
import com.luistudio.reservas.exception.BusinessException;
import com.luistudio.reservas.model.RoomEntity;
import com.luistudio.reservas.model.UserEntity;
import com.luistudio.reservas.repository.ReservationRepository;
import com.luistudio.reservas.service.RoomScheduleService;
import com.luistudio.reservas.service.RoomService;
import com.luistudio.reservas.service.SystemConfigService;
import com.luistudio.reservas.util.AppTime;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class BookingValidationService {

    private final RoomScheduleService roomScheduleService;
    private final RoomService roomService;
    private final ReservationRepository reservationRepository;
    private final SystemConfigService systemConfigService;

    public BookingValidationService(
        RoomScheduleService roomScheduleService,
        RoomService roomService,
        ReservationRepository reservationRepository,
        SystemConfigService systemConfigService
    ) {
        this.roomScheduleService = roomScheduleService;
        this.roomService = roomService;
        this.reservationRepository = reservationRepository;
        this.systemConfigService = systemConfigService;
    }

    public void validate(
        UserEntity user,
        RoomEntity room,
        BookingUpsertRequest request,
        Long excludeBookingId
    ) {
        validateEndTimeAfterStart(request);
        validateCapacity(room, request);
        validateBookingWindowAndSlot(room, request);
        validateMaxDuration(room, request);
        validateRoomAvailability(room, request, excludeBookingId);
        validateMaxActiveBookings(user, excludeBookingId);
    }

    private void validateEndTimeAfterStart(BookingUpsertRequest request) {
        if (!request.end().isAfter(request.start())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "La hora fin debe ser mayor a la hora inicio");
        }
    }

    private void validateCapacity(RoomEntity room, BookingUpsertRequest request) {
        int people = request.people();
        int roomCapacity = room.getCapacidad();
        int roomMax = room.getMaximoPersonas() == null ? roomCapacity : room.getMaximoPersonas();
        int roomMin = room.getMinimoPersonas() == null ? 1 : room.getMinimoPersonas();
        boolean minRequired = Boolean.TRUE.equals(room.getMinimoPersonasObligatorio());

        if (people > roomCapacity || people > roomMax) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "La cantidad de personas supera el máximo permitido para la sala");
        }
        if (minRequired && people < roomMin) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "La reserva requiere al menos " + roomMin + " personas para esta sala");
        }
    }

    private void validateBookingWindowAndSlot(RoomEntity room, BookingUpsertRequest request) {
        LocalDateTime now = AppTime.nowDateTime();
        LocalDate today = now.toLocalDate();
        LocalDate requestDate = request.date();

        if (requestDate.isBefore(today)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Solo puedes reservar en horas y fechas futuras");
        }

        LocalDate monday = today.minusDays(today.getDayOfWeek().getValue() - DayOfWeek.MONDAY.getValue());
        LocalDate endCurrentWeek = monday.plusDays(6);
        boolean weekendToday = today.getDayOfWeek() == DayOfWeek.SATURDAY || today.getDayOfWeek() == DayOfWeek.SUNDAY;
        LocalDate maxAllowedDate = weekendToday ? endCurrentWeek.plusDays(7) : endCurrentWeek;
        if (requestDate.isAfter(maxAllowedDate)) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                weekendToday
                    ? "Solo puedes reservar hasta la siguiente semana durante fin de semana"
                    : "Solo puedes reservar dentro de la semana actual"
            );
        }

        if (requestDate.isEqual(today) && !request.start().isAfter(now.toLocalTime())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Solo puedes reservar en horarios posteriores a la hora actual");
        }

        int slotMinutes = roomScheduleService.getCampusSlotMinutes(room.getCampus());
        int durationMinutes = (int) Duration.between(request.start(), request.end()).toMinutes();
        if (durationMinutes != slotMinutes) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "La reserva debe ocupar exactamente un bloque de " + slotMinutes + " minutos en este campus"
            );
        }

        RoomScheduleService.EffectiveSchedule schedule = roomScheduleService.getEffectiveScheduleForRoomDay(room, requestDate);
        if (schedule.closed() || schedule.openTime() == null || schedule.closeTime() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "La sala no atiende en esa fecha");
        }

        LocalTime start = request.start();
        LocalTime end = request.end();
        if (start.isBefore(schedule.openTime()) || end.isAfter(schedule.closeTime())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "El horario no está dentro del rango disponible de la sala");
        }

        long fromOpenToStart = Duration.between(schedule.openTime(), start).toMinutes();
        long fromOpenToEnd = Duration.between(schedule.openTime(), end).toMinutes();
        if (fromOpenToStart < 0 || fromOpenToEnd < 0 || fromOpenToStart % slotMinutes != 0 || fromOpenToEnd % slotMinutes != 0) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "El horario debe alinearse con bloques de " + slotMinutes + " minutos desde la apertura del día"
            );
        }
    }

    private void validateMaxDuration(RoomEntity room, BookingUpsertRequest request) {
        int durationMinutes = (int) Duration.between(request.start(), request.end()).toMinutes();
        int maxDuration = roomScheduleService.getCampusSlotMinutes(room.getCampus());
        if (durationMinutes > maxDuration) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "La duración máxima permitida para esta sala/campus es " + maxDuration + " minutos"
            );
        }
    }

    private void validateRoomAvailability(
        RoomEntity room,
        BookingUpsertRequest request,
        Long excludeBookingId
    ) {
        boolean available = roomService.isRoomAvailable(
            room,
            request.date(),
            request.start(),
            request.end(),
            excludeBookingId
        );
        if (!available) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "La sala no está disponible para el horario seleccionado");
        }
    }

    private void validateMaxActiveBookings(UserEntity user, Long excludeBookingId) {
        if (excludeBookingId != null) {
            return;
        }

        int maxAllowed = systemConfigService.getMaxActiveBookings();
        long activeCount = reservationRepository.countCurrentActiveForUser(user, AppTime.today(), AppTime.nowTime());
        if (activeCount >= maxAllowed) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "Alcanzaste el límite de reservas activas (" + maxAllowed + ")"
            );
        }
    }
}
