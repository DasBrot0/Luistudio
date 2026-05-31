package com.luistudio.reservas.service.factory;

import com.luistudio.reservas.model.PabellonEntity;
import com.luistudio.reservas.model.RoomEntity;
import com.luistudio.reservas.model.RoomState;
import org.springframework.stereotype.Component;

@Component
public class RoomFactory {

    public RoomEntity createAvailableRoom(
        String name,
        String campus,
        String venue,
        Integer capacity,
        Integer minPeople,
        Boolean minPeopleRequired,
        Integer maxPeople,
        String location,
        PabellonEntity pabellon,
        String roomCode
    ) {
        RoomEntity room = new RoomEntity();
        room.setNombre(name.trim());
        room.setCampus(campus.trim());
        room.setVenue(venue.trim());
        room.setCapacidad(capacity);
        room.setMinimoPersonas(minPeople);
        room.setMinimoPersonasObligatorio(minPeopleRequired);
        room.setMaximoPersonas(maxPeople);
        room.setUbicacion(location.trim());
        room.setEstado(RoomState.DISPONIBLE);
        room.setPabellon(pabellon);
        room.setCodigo(roomCode);
        return room;
    }
}
