package com.luistudio.reservas.dto.room;

import java.util.Set;

/** Datos públicos de una sala activa que ayudan a interpretar su afinidad semántica. */
public record RoomSearchCandidate(
    Long id,
    String code,
    String name,
    String description,
    String campus,
    String venue,
    String location,
    Double latitude,
    Double longitude,
    Integer capacity,
    String noiseLevel,
    Boolean supportsConcentration,
    String roomType,
    Set<String> equipment,
    Set<String> allowedActivities,
    Set<String> nearbyServices,
    Set<String> accessibilityFeatures
) {
}
