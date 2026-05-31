package com.luistudio.reservas.dto.admin;

import java.util.List;

public record CampusScheduleResponse(
    String campus,
    String campusLabel,
    int slotMinutes,
    List<CampusScheduleDayResponse> days,
    List<String> warnings
) {
}
