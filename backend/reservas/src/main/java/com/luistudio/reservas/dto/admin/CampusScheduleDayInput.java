package com.luistudio.reservas.dto.admin;

import java.time.LocalTime;

public record CampusScheduleDayInput(
    int dayOfWeek,
    LocalTime openTime,
    LocalTime closeTime,
    boolean closed
) {
}
