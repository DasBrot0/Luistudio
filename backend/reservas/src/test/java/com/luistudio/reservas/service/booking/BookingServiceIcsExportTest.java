package com.luistudio.reservas.service.booking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import com.luistudio.reservas.exception.BusinessException;
import com.luistudio.reservas.exception.NotFoundException;
import com.luistudio.reservas.model.ReservationEntity;
import com.luistudio.reservas.model.CampusEntity;
import com.luistudio.reservas.model.PabellonEntity;
import com.luistudio.reservas.model.ReservationStatus;
import com.luistudio.reservas.model.RoleEntity;
import com.luistudio.reservas.model.RoomEntity;
import com.luistudio.reservas.model.UserEntity;
import com.luistudio.reservas.repository.ReservationRepository;
import com.luistudio.reservas.service.AuditService;
import com.luistudio.reservas.service.AvailabilitySubscriptionService;
import com.luistudio.reservas.service.BookingService;
import com.luistudio.reservas.service.DtoMapper;
import com.luistudio.reservas.service.EmailOutboxService;
import com.luistudio.reservas.service.RoomService;
import com.luistudio.reservas.service.UserService;
import com.luistudio.reservas.service.booking.validation.BookingValidationService;
import com.luistudio.reservas.service.email.EmailTemplateService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

/**
 * Pruebas Unitarias — BookingService.getStudentIcsContent(Long, Long)
 *
 * Método evaluado:
 *   BookingService.getStudentIcsContent(Long bookingId, Long studentUserId)
 *
 * Flujo interno del método:
 *   1. Carga el UserEntity por studentUserId.
 *   2. Verifica que el rol sea "ESTUDIANTE" — lanza BusinessException(FORBIDDEN) si no.
 *   3. Carga la ReservationEntity por bookingId.
 *   4. Verifica que reservation.usuario.id == studentUserId — lanza BusinessException(FORBIDDEN) si no.
 *   5. Verifica que el estado sea ACTIVA — lanza BusinessException(BAD_REQUEST) si no.
 *   6. Genera y retorna el contenido ICS.
 *
 * Casos cubiertos:
 *   UT-01  Estudiante propietario de una reserva activa exporta ICS correctamente.
 *   UT-02  Usuario con rol diferente a ESTUDIANTE recibe error FORBIDDEN.
 *   UT-03  Estudiante intenta exportar una reserva ajena y recibe error FORBIDDEN.
 *   UT-04  Estudiante intenta exportar una reserva CANCELADA y recibe error BAD_REQUEST.
 */
@ExtendWith(MockitoExtension.class)
class BookingServiceIcsExportTest {

    // -----------------------------------------------------------------------
    // Constantes de prueba
    // -----------------------------------------------------------------------

    private static final Long STUDENT_ID   = 10L;
    private static final Long OTHER_ID     = 99L;
    private static final Long BOOKING_ID   = 1L;
    private static final LocalDate DATE    = LocalDate.of(2025, 8, 11);
    private static final LocalTime START   = LocalTime.of(9, 0);
    private static final LocalTime END     = LocalTime.of(10, 0);

    // -----------------------------------------------------------------------
    // Mocks — todos los colaboradores de BookingService
    // -----------------------------------------------------------------------

    @Mock private ReservationRepository      reservationRepository;
    @Mock private RoomService                roomService;
    @Mock private UserService                userService;
    @Mock private EmailOutboxService         emailOutboxService;
    @Mock private AuditService               auditService;
    @Mock private DtoMapper                  dtoMapper;
    @Mock private BookingValidationService   bookingValidationService;
    @Mock private EmailTemplateService       emailTemplateService;
    @Mock private AvailabilitySubscriptionService availabilitySubscriptionService;

    @InjectMocks
    private BookingService sut;

    // -----------------------------------------------------------------------
    // Objetos base reutilizables
    // -----------------------------------------------------------------------

    private UserEntity studentUser;
    private RoleEntity studentRole;
    private RoomEntity room;
    private ReservationEntity activeReservation;

    @BeforeEach
    void setUp() {
        // Rol ESTUDIANTE
        studentRole = new RoleEntity();
        studentRole.setId(2L);
        studentRole.setNombre("ESTUDIANTE");

        // Usuario estudiante propietario de la reserva
        studentUser = new UserEntity();
        studentUser.setId(STUDENT_ID);
        studentUser.setNombres("Luis");
        studentUser.setApellidos("García");
        studentUser.setCorreo("luis@estudiante.edu");
        studentUser.setRol(studentRole);

        // Sala de prueba
        room = new RoomEntity();
        room.setId(5L);
        room.setCodigo("A-101");
        room.setNombre("Aula A-101");
        room.setUbicacion("Piso 1, ala norte");

        CampusEntity campus = new CampusEntity();
        campus.setNombre("Monterrico");
        PabellonEntity building = new PabellonEntity();
        building.setNombre("Pabellón A1");
        building.setCampus(campus);
        building.setLatitude(new BigDecimal("-12.0842548"));
        building.setLongitude(new BigDecimal("-76.9729651"));
        room.setPabellon(building);

        // Reserva activa que pertenece al estudiante
        activeReservation = new ReservationEntity();
        activeReservation.setId(BOOKING_ID);
        activeReservation.setUsuario(studentUser);
        activeReservation.setSala(room);
        activeReservation.setFecha(DATE);
        activeReservation.setHoraInicio(START);
        activeReservation.setHoraFin(END);
        activeReservation.setEstado(ReservationStatus.ACTIVA);
    }

    // -----------------------------------------------------------------------
    // UT-01: Estudiante propietario, reserva ACTIVA → ICS generado correctamente
    // -----------------------------------------------------------------------

    /**
     * Camino feliz: todas las condiciones de autorización y estado se cumplen.
     * Se verifica que el string retornado contenga las marcas estándar de iCalendar
     * (BEGIN:VCALENDAR, BEGIN:VEVENT, DTSTART, SUMMARY con el nombre de la sala).
     */
    @Test
    @DisplayName("UT-01: Estudiante propietario de reserva activa exporta ICS correctamente")
    void ut01_studentOwnerOfActiveBooking_returnsValidIcs() {
        when(userService.getById(STUDENT_ID)).thenReturn(studentUser);
        when(reservationRepository.findById(BOOKING_ID)).thenReturn(Optional.of(activeReservation));

        String ics = sut.getStudentIcsContent(BOOKING_ID, STUDENT_ID);

        assertTrue(ics.contains("BEGIN:VCALENDAR"),
            "El ICS debe comenzar con BEGIN:VCALENDAR");
        assertTrue(ics.contains("BEGIN:VEVENT"),
            "El ICS debe contener un bloque VEVENT");
        assertTrue(ics.contains("DTSTART:"),
            "El ICS debe incluir la fecha/hora de inicio");
        assertTrue(ics.contains("DTEND:"),
            "El ICS debe incluir la fecha/hora de fin");
        assertTrue(ics.contains("UID:booking-1@luistudio"),
            "El evento debe conservar un UID estable para la reserva");
        assertTrue(ics.contains("DTSTART:20250811T140000Z"),
            "La hora de Lima debe convertirse correctamente a UTC");
        assertTrue(ics.contains("DTEND:20250811T150000Z"),
            "La hora final debe convertirse correctamente a UTC");
        assertTrue(ics.contains("Aula A-101"),
            "El SUMMARY del ICS debe incluir el nombre de la sala");
        assertTrue(ics.contains("LOCATION:Piso 1\\, ala norte\\, Pabellón A1\\, Campus Monterrico\\, (-12.0842548\\, -76.9729651)"),
            "La ubicación debe provenir de la sala, pabellón, campus y coordenadas reales");
        assertTrue(ics.contains("GEO:-12.0842548;-76.9729651"),
            "El ICS debe incluir las coordenadas del pabellón usadas por el mapa");
        assertTrue(ics.contains("END:VCALENDAR"),
            "El ICS debe cerrar con END:VCALENDAR");
        assertTrue(ics.endsWith("\r\n"),
            "El documento iCalendar debe usar terminadores CRLF");
    }

    // -----------------------------------------------------------------------
    // UT-02: Usuario con rol diferente a ESTUDIANTE → FORBIDDEN
    // -----------------------------------------------------------------------

    /**
     * Un usuario ADMIN intenta exportar un ICS de reserva.
     * Se espera BusinessException con status 403 FORBIDDEN.
     * El repositorio de reservas NO debe ser consultado.
     */
    @Test
    @DisplayName("UT-02: Usuario con rol ADMIN recibe FORBIDDEN al intentar exportar ICS")
    void ut02_nonStudentRole_throwsForbidden() {
        RoleEntity adminRole = new RoleEntity();
        adminRole.setId(1L);
        adminRole.setNombre("ADMIN");

        UserEntity adminUser = new UserEntity();
        adminUser.setId(STUDENT_ID);
        adminUser.setRol(adminRole);

        when(userService.getById(STUDENT_ID)).thenReturn(adminUser);

        BusinessException ex = assertThrows(BusinessException.class,
            () -> sut.getStudentIcsContent(BOOKING_ID, STUDENT_ID));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus(),
            "El status HTTP debe ser 403 FORBIDDEN");
        assertEquals("Solo estudiantes pueden exportar reservas", ex.getMessage(),
            "El mensaje de error debe indicar que solo estudiantes pueden exportar");
    }

    // -----------------------------------------------------------------------
    // UT-03: Estudiante intenta exportar una reserva ajena → FORBIDDEN
    // -----------------------------------------------------------------------

    /**
     * El estudiante (id=10) intenta exportar la reserva que pertenece a otro usuario (id=99).
     * Se espera BusinessException con status 403 FORBIDDEN.
     */
    @Test
    @DisplayName("UT-03: Estudiante intenta exportar una reserva ajena y recibe FORBIDDEN")
    void ut03_studentExportsOthersBooking_throwsForbidden() {
        // Otro usuario (el propietario real de la reserva)
        UserEntity otherStudent = new UserEntity();
        otherStudent.setId(OTHER_ID);
        otherStudent.setRol(studentRole);

        // La reserva pertenece a otherStudent, NO a studentUser (id=10)
        ReservationEntity foreignReservation = new ReservationEntity();
        foreignReservation.setId(BOOKING_ID);
        foreignReservation.setUsuario(otherStudent);   // propietario ≠ solicitante
        foreignReservation.setSala(room);
        foreignReservation.setFecha(DATE);
        foreignReservation.setHoraInicio(START);
        foreignReservation.setHoraFin(END);
        foreignReservation.setEstado(ReservationStatus.ACTIVA);

        when(userService.getById(STUDENT_ID)).thenReturn(studentUser);
        when(reservationRepository.findById(BOOKING_ID)).thenReturn(Optional.of(foreignReservation));

        BusinessException ex = assertThrows(BusinessException.class,
            () -> sut.getStudentIcsContent(BOOKING_ID, STUDENT_ID));

        assertEquals(HttpStatus.FORBIDDEN, ex.getStatus(),
            "El status HTTP debe ser 403 FORBIDDEN");
        assertEquals("Solo puedes exportar tus propias reservas", ex.getMessage(),
            "El mensaje debe indicar que solo se puede exportar la propia reserva");
    }

    // -----------------------------------------------------------------------
    // UT-04: Estudiante intenta exportar una reserva CANCELADA → BAD_REQUEST
    // -----------------------------------------------------------------------

    /**
     * La reserva pertenece al estudiante pero su estado es CANCELADA.
     * Se espera BusinessException con status 400 BAD_REQUEST.
     */
    @Test
    @DisplayName("UT-04: Estudiante intenta exportar reserva cancelada y recibe BAD_REQUEST")
    void ut04_studentExportsCancelledBooking_throwsBadRequest() {
        ReservationEntity cancelledReservation = new ReservationEntity();
        cancelledReservation.setId(BOOKING_ID);
        cancelledReservation.setUsuario(studentUser);
        cancelledReservation.setSala(room);
        cancelledReservation.setFecha(DATE);
        cancelledReservation.setHoraInicio(START);
        cancelledReservation.setHoraFin(END);
        cancelledReservation.setEstado(ReservationStatus.CANCELADA);   // ← estado no permitido

        when(userService.getById(STUDENT_ID)).thenReturn(studentUser);
        when(reservationRepository.findById(BOOKING_ID)).thenReturn(Optional.of(cancelledReservation));

        BusinessException ex = assertThrows(BusinessException.class,
            () -> sut.getStudentIcsContent(BOOKING_ID, STUDENT_ID));

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus(),
            "El status HTTP debe ser 400 BAD_REQUEST");
        assertEquals("Solo se pueden exportar reservas confirmadas", ex.getMessage(),
            "El mensaje debe indicar que solo se exportan reservas confirmadas");
    }
}
