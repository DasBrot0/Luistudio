package com.luistudio.reservas.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AttendanceStatusUpdateRequest(
    @NotBlank
    @Pattern(regexp = "ASISTIO|INASISTIO", message = "El estado debe ser ASISTIO o INASISTIO")
    String status
) {
}
