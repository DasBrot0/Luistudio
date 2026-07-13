package com.luistudio.reservas.dto.room;

import com.luistudio.reservas.model.RoomState;
import com.luistudio.reservas.model.RoomNoiseLevel;
import com.luistudio.reservas.model.RoomType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.Set;

public record RoomUpsertRequest(
    @NotBlank @Size(max = 120) String name,
    @NotBlank @Size(max = 120) String campus,
    @NotBlank @Size(max = 120) String location,
    @NotNull @Min(1) Integer capacity,
    @NotNull @Min(1) Integer maxPeople,
    @Min(1) Integer minPeople,
    @NotNull Boolean minPeopleRequired,
    List<@Valid RoomScheduleInput> schedule,
    @Size(max = 20, message = "El código de pabellón debe tener 20 caracteres como máximo") String pabellonCode,
    RoomState status,
    RoomNoiseLevel noiseLevel,
    Boolean supportsConcentration,
    RoomType roomType,
    Set<@Size(max = 50) String> equipment
) {
}
