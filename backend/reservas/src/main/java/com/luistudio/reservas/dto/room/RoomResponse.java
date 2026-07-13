package com.luistudio.reservas.dto.room;

import java.util.List;
import java.util.Set;

public record RoomResponse(
    Long id,
    String code,
    String name,
    String resourceLabel,
    String campus,
    String campusLabel,
    String venue,
    String venueLabel,
    Integer capacity,
    String location,
    Integer minPeople,
    Boolean minPeopleRequired,
    Integer maxPeople,
    Integer slotMinutes,
    List<RoomScheduleResponse> schedule,
    String status,
    String pabellonCode,
    String noiseLevel,
    Boolean supportsConcentration,
    String roomType,
    Set<String> equipment
) {
}
