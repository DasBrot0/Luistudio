package com.luistudio.reservas.dto.admin;

import java.time.LocalTime;

public record CampusScheduleDayResponse(
    int dayOfWeek,
    LocalTime openTime,
    LocalTime closeTime,
    boolean closed
) {
}
