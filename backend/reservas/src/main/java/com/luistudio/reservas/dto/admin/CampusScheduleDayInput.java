package com.luistudio.reservas.dto.admin;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalTime;

public record CampusScheduleDayInput(
    @Min(1) @Max(7) int dayOfWeek,
    LocalTime openTime,
    LocalTime closeTime,
    boolean closed
) {
}
