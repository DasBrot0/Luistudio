package com.luistudio.reservas.dto.admin;

import java.util.List;

public record CampusScheduleListResponse(
    List<CampusScheduleResponse> campuses
) {
}
