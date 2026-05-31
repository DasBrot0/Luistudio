package com.luistudio.reservas.dto.admin;

import com.luistudio.reservas.dto.room.RoomResponse;
import java.util.List;

public record CampusMapResponse(List<PabellonMapItem> pabellones) {

    public record PabellonMapItem(String codigo, String nombre, List<RoomMapItem> salas) {
    }

    public record RoomMapItem(
        Long id,
        String codigo,
        String nombre,
        String estado,
        String ubicacion,
        Integer capacidad
    ) {
    }
}
