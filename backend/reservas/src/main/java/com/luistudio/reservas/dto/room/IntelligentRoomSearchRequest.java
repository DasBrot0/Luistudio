package com.luistudio.reservas.dto.room;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Duration;

public record IntelligentRoomSearchRequest(
    @NotBlank @Size(max = 500) String query,
    @NotNull LocalDate date,
    @NotNull LocalTime start,
    @NotNull LocalTime end,
    @Min(1) @Max(3) Integer limit
) {
    @AssertTrue(message = "La hora de fin debe ser mayor que la hora de inicio")
    public boolean isTimeRangeValid() {
        return start == null || end == null || end.isAfter(start);
    }

    @AssertTrue(message = "El horario debe usar un bloque válido de 30 o 60 minutos")
    public boolean isReservableSlotValid() {
        if (start == null || end == null || !end.isAfter(start)) {
            return true;
        }
        long duration = Duration.between(start, end).toMinutes();
        boolean exactSeconds = start.getSecond() == 0 && start.getNano() == 0
            && end.getSecond() == 0 && end.getNano() == 0;
        if (!exactSeconds || (duration != 30 && duration != 60)) {
            return false;
        }
        return duration == 30 ? start.getMinute() % 30 == 0 : start.getMinute() == 0;
    }
}
