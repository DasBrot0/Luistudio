package com.luistudio.reservas.dto.booking;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalTime;

public record BookingUpsertRequest(
    @NotNull Long roomId,
    @NotNull LocalDate date,
    @NotNull LocalTime start,
    @NotNull LocalTime end,
    @NotNull @Min(1) Integer people,
    @NotBlank @Size(max = 120) String location,
    @Size(max = 255) String observation
) {
}
