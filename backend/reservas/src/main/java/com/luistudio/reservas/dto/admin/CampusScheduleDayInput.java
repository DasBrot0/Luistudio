package com.luistudio.reservas.dto.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

public record CampusScheduleDayInput(
    @Min(1) @Max(7) int dayOfWeek,
    @NotNull LocalTime openTime,
    @NotNull LocalTime closeTime,
    boolean closed
) {
}
