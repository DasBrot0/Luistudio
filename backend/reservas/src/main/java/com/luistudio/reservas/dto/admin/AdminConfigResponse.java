package com.luistudio.reservas.dto.admin;

public record AdminConfigResponse(
    int maxActiveBookings,
    int maxDurationMinutes
) {
}
