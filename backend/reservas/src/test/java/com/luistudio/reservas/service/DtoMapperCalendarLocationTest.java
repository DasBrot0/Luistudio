package com.luistudio.reservas.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.luistudio.reservas.dto.booking.BookingResponse;
import com.luistudio.reservas.model.CampusEntity;
import com.luistudio.reservas.model.PabellonEntity;
import com.luistudio.reservas.model.ReservationEntity;
import com.luistudio.reservas.model.ReservationStatus;
import com.luistudio.reservas.model.RoomEntity;
import com.luistudio.reservas.model.UserEntity;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class DtoMapperCalendarLocationTest {

    @Test
    void bookingExportsDatabaseBuildingLocationToGoogleCalendar() {
        CampusEntity campus = new CampusEntity();
        campus.setNombre("Monterrico");

        PabellonEntity building = new PabellonEntity();
        building.setNombre("Pabellón A1");
        building.setCampus(campus);
        building.setLatitude(new BigDecimal("-12.0842548"));
        building.setLongitude(new BigDecimal("-76.9729651"));

        RoomEntity room = new RoomEntity();
        room.setId(5L);
        room.setCodigo("A1-101");
        room.setNombre("Aula A1-101");
        room.setUbicacion("Piso 1, ala norte");
        room.setPabellon(building);

        UserEntity user = new UserEntity();
        user.setId(10L);
        user.setCorreo("student@example.com");

        ReservationEntity reservation = new ReservationEntity();
        reservation.setId(1L);
        reservation.setUsuario(user);
        reservation.setSala(room);
        reservation.setCantidadPersonas(2);
        reservation.setFecha(LocalDate.of(2026, 7, 13));
        reservation.setHoraInicio(LocalTime.of(9, 0));
        reservation.setHoraFin(LocalTime.of(10, 0));
        reservation.setEstado(ReservationStatus.ACTIVA);

        DtoMapper mapper = new DtoMapper(Mockito.mock(RoomCatalogTranslator.class));
        BookingResponse response = mapper.toBooking(reservation);

        String expectedLocation = "Piso 1, ala norte, Pabellón A1, Campus Monterrico, "
            + "(-12.0842548, -76.9729651)";
        assertEquals(expectedLocation, response.location());
        assertTrue(
            URLDecoder.decode(response.googleCalendarUrl(), StandardCharsets.UTF_8)
                .contains("location=" + expectedLocation),
            "Google Calendar debe recibir la ubicación jerárquica y las coordenadas guardadas en buildings"
        );
    }
}
