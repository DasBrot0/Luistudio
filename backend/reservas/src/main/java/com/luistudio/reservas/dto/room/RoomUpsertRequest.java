package com.luistudio.reservas.dto.room;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RoomUpsertRequest(
    @NotBlank String name,
    @NotBlank String location,
    @NotNull @Min(1) Integer capacity,
    String pabellonCode
) {
}
