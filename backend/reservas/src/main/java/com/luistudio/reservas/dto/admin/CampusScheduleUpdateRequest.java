package com.luistudio.reservas.dto.admin;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Min;
import java.util.List;

public record CampusScheduleUpdateRequest(
    @NotBlank @Size(max = 120) String campus,
    @NotNull @Min(5) @Max(240) Integer slotMinutes,
    @NotEmpty List<@Valid CampusScheduleDayInput> days
) {
}
