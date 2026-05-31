package com.luistudio.reservas.dto.room;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record RoomUpsertRequest(
    @NotBlank String name,
    @NotBlank String campus,
    @NotBlank String location,
    @NotNull @Min(1) Integer capacity,
    @NotNull @Min(1) Integer maxPeople,
    @Min(1) Integer minPeople,
    @NotNull Boolean minPeopleRequired,
    List<RoomScheduleInput> schedule,
    String pabellonCode
) {
}
