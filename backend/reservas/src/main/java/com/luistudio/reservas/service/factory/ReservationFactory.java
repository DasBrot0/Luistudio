package com.luistudio.reservas.service.factory;

import com.luistudio.reservas.dto.booking.BookingUpsertRequest;
import com.luistudio.reservas.model.ReservationEntity;
import com.luistudio.reservas.model.ReservationStatus;
import com.luistudio.reservas.model.RoomEntity;
import com.luistudio.reservas.model.UserEntity;
import org.springframework.stereotype.Component;

@Component
public class ReservationFactory {

    public ReservationEntity createActiveReservation(UserEntity user, RoomEntity room, BookingUpsertRequest request) {
        ReservationEntity booking = new ReservationEntity();
        booking.setUsuario(user);
        booking.setSala(room);
        booking.setFecha(request.date());
        booking.setHoraInicio(request.start());
        booking.setHoraFin(request.end());
        booking.setCantidadPersonas(request.people());
        booking.setObservacion(request.observation());
        booking.setEstado(ReservationStatus.ACTIVA);
        return booking;
    }
}
