package com.luistudio.reservas.service.factory;

import com.luistudio.reservas.model.PabellonEntity;
import com.luistudio.reservas.model.RoomEntity;
import com.luistudio.reservas.model.RoomState;
import org.springframework.stereotype.Component;

@Component
public class RoomFactory {

    public RoomEntity createAvailableRoom(
        String name,
        Integer capacity,
        String location,
        PabellonEntity pabellon,
        String roomCode
    ) {
        RoomEntity room = new RoomEntity();
        room.setNombre(name.trim());
        room.setCapacidad(capacity);
        room.setUbicacion(location.trim());
        room.setEstado(RoomState.DISPONIBLE);
        room.setPabellon(pabellon);
        room.setCodigo(roomCode);
        return room;
    }
}
