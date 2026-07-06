package com.luistudio.reservas.dto.room;

import java.time.LocalDate;
import java.time.LocalTime;

public record AvailabilitySubscriptionRequest(LocalDate targetDate, LocalTime startTime, LocalTime endTime) {
}
