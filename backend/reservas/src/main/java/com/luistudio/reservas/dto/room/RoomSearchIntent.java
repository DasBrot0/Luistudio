package com.luistudio.reservas.dto.room;

import com.luistudio.reservas.model.RoomNoiseLevel;
import com.luistudio.reservas.model.RoomType;
import java.util.Set;

/** Contrato validado que la IA puede producir; no contiene ninguna decisión de sala. */
public record RoomSearchIntent(
    RoomType roomType,
    int minimumCapacity,
    RoomNoiseLevel maximumNoise,
    boolean requiresConcentration,
    Set<String> requiredEquipment
) {
}
