package com.luistudio.reservas.dto.room;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

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
}
