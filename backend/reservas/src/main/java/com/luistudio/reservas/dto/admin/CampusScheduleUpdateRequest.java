package com.luistudio.reservas.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record CampusScheduleUpdateRequest(
    @NotBlank String campus,
    @NotNull Integer slotMinutes,
    @NotEmpty List<CampusScheduleDayInput> days
) {
}
