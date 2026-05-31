package com.luistudio.reservas.service;

import com.luistudio.reservas.dto.auth.AuthUserResponse;
import com.luistudio.reservas.dto.booking.BookingResponse;
import com.luistudio.reservas.dto.room.MaintenanceResponse;
import com.luistudio.reservas.dto.room.RoomResponse;
import com.luistudio.reservas.dto.room.RoomScheduleResponse;
import com.luistudio.reservas.dto.user.UserResponse;
import com.luistudio.reservas.model.MaintenanceEntity;
import com.luistudio.reservas.model.ReservationEntity;
import com.luistudio.reservas.model.RoomEntity;
import com.luistudio.reservas.model.UserEntity;
import com.luistudio.reservas.util.CalendarUtils;
import org.springframework.stereotype.Component;

@Component
public class DtoMapper {

    private final RoomCatalogTranslator roomCatalogTranslator;

    public DtoMapper(RoomCatalogTranslator roomCatalogTranslator) {
        this.roomCatalogTranslator = roomCatalogTranslator;
    }

    public AuthUserResponse toAuthUser(UserEntity user) {
        return new AuthUserResponse(
            user.getId(),
            user.getCodigo(),
            user.getNombres(),
            user.getApellidos(),
            user.getCorreo(),
            user.getRol().getNombre(),
            user.getEstado().name()
        );
    }

    public RoomResponse toRoom(RoomEntity room) {
        return new RoomResponse(
            room.getId(),
            room.getCodigo(),
            room.getNombre(),
            roomCatalogTranslator.resourceToEs(room.getNombre()),
            room.getCampus(),
            roomCatalogTranslator.campusToEs(room.getCampus()),
            room.getVenue(),
            roomCatalogTranslator.venueToEs(room.getVenue()),
            room.getCapacidad(),
            room.getUbicacion(),
            room.getMinimoPersonas(),
            room.getMinimoPersonasObligatorio(),
            room.getMaximoPersonas(),
            60,
            java.util.List.<RoomScheduleResponse>of(),
            room.getEstado().name(),
            room.getPabellon().getCodigo()
        );
    }

    public BookingResponse toBooking(ReservationEntity reservation) {
        String title = "Reserva - " + reservation.getSala().getNombre();
        String description = "Reserva Luistudio ID " + reservation.getId();
        String location = reservation.getSala().getUbicacion();

        return new BookingResponse(
            reservation.getId(),
            reservation.getUsuario().getId(),
            reservation.getUsuario().getCorreo(),
            reservation.getSala().getId(),
            reservation.getSala().getCodigo(),
            reservation.getSala().getNombre(),
            reservation.getSala().getUbicacion(),
            reservation.getCantidadPersonas(),
            reservation.getFecha(),
            reservation.getHoraInicio(),
            reservation.getHoraFin(),
            reservation.getEstado().name(),
            reservation.getObservacion(),
            CalendarUtils.googleCalendarLink(
                title,
                description,
                location,
                reservation.getFecha(),
                reservation.getHoraInicio(),
                reservation.getHoraFin()
            ),
            "/api/bookings/" + reservation.getId() + "/ics"
        );
    }

    public UserResponse toUser(UserEntity user) {
        return new UserResponse(
            user.getId(),
            user.getCodigo(),
            user.getCorreo(),
            user.getNombres(),
            user.getApellidos(),
            user.getEstado().name(),
            user.getRol().getNombre()
        );
    }

    public MaintenanceResponse toMaintenance(MaintenanceEntity maintenance) {
        return new MaintenanceResponse(
            maintenance.getId(),
            maintenance.getInicio(),
            maintenance.getFin(),
            maintenance.getMotivo(),
            maintenance.getEstado().name()
        );
    }
}
