package com.luistudio.reservas.dto.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.AssertTrue;
import java.util.List;

public record CampusScheduleUpdateRequest(
    @NotBlank @Size(max = 120) String campus,
    @NotNull Integer slotMinutes,
    @NotEmpty List<@Valid CampusScheduleDayInput> days
) {
    @AssertTrue(message = "La duración por reserva debe ser de 30 o 60 minutos")
    public boolean isSlotMinutesValid() {
        return slotMinutes == null || slotMinutes == 30 || slotMinutes == 60;
    }
}
