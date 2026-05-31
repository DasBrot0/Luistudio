package com.luistudio.reservas.service.booking.rule;

import com.luistudio.reservas.exception.BusinessException;
import com.luistudio.reservas.service.RoomScheduleService;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@Order(25)
public class BookingWindowAndSlotRule implements BookingValidationRule {

    private final RoomScheduleService roomScheduleService;

    public BookingWindowAndSlotRule(RoomScheduleService roomScheduleService) {
        this.roomScheduleService = roomScheduleService;
    }

    @Override
    public void validate(BookingRuleContext context) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        LocalDate requestDate = context.request().date();

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

        if (requestDate.isEqual(today) && !context.request().start().isAfter(now.toLocalTime())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Solo puedes reservar en horarios posteriores a la hora actual");
        }

        int slotMinutes = roomScheduleService.getCampusSlotMinutes(context.room().getCampus());
        int durationMinutes = (int) Duration.between(context.request().start(), context.request().end()).toMinutes();
        if (durationMinutes != slotMinutes) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "La reserva debe ocupar exactamente un bloque de " + slotMinutes + " minutos en este campus"
            );
        }

        RoomScheduleService.EffectiveSchedule schedule = roomScheduleService.getEffectiveScheduleForRoomDay(context.room(), requestDate);
        if (schedule.closed() || schedule.openTime() == null || schedule.closeTime() == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "La sala no atiende en esa fecha");
        }

        LocalTime start = context.request().start();
        LocalTime end = context.request().end();
        if (start.isBefore(schedule.openTime()) || end.isAfter(schedule.closeTime())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "El horario no esta dentro del rango disponible de la sala");
        }

        long fromOpenToStart = Duration.between(schedule.openTime(), start).toMinutes();
        long fromOpenToEnd = Duration.between(schedule.openTime(), end).toMinutes();
        if (fromOpenToStart < 0 || fromOpenToEnd < 0 || fromOpenToStart % slotMinutes != 0 || fromOpenToEnd % slotMinutes != 0) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "El horario debe alinearse con bloques de " + slotMinutes + " minutos desde la apertura del dia"
            );
        }
    }
}
