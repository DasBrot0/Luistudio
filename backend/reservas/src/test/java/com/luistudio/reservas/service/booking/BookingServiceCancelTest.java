package com.luistudio.reservas.service.booking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.luistudio.reservas.dto.booking.BookingResponse;
import com.luistudio.reservas.exception.BusinessException;
import com.luistudio.reservas.model.ReservationEntity;
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
import com.luistudio.reservas.util.AppTime;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

/**
 * Pruebas Unitarias — BookingService.cancelBooking(Long, Long, boolean)
 *
 * Flujo interno del método:
 *   1. Carga la ReservationEntity por bookingId.
 *   2. Si estado == CANCELADA  → retorna DTO sin guardar ni notificar. (skip path)
 *   3. Si fecha+horaFin <= ahora → lanza BusinessException(BAD_REQUEST). (past path)
 *   4. Cambia estado a CANCELADA, persiste, encola email, notifica suscriptores.
 *
 * Casos cubiertos:
 *   CX-01  Reserva activa, actor=estudiante (adminCancel=false)  → se cancela correctamente.
 *   CX-02  Reserva activa, actor=admin     (adminCancel=true)   → razón incluye "administrador".
 *   CX-03  Reserva ya CANCELADA             → se salta sin guardar ni notificar.
 *   CX-04  Reserva cuya horaFin ya pasó    → BusinessException(BAD_REQUEST).
 *   CX-05  Cancelación exitosa             → se encola exactamente 1 notificación con tipo correcto.
 *
 * Estrategia de mocks:
 *   - AppTime.nowDateTime() se fija con MockedStatic para controlar si la reserva
 *     está en el futuro (happy path) o en el pasado (excepción).
 *   - reservationRepository.findById() y .save() se mockean directamente.
 *   - emailOutboxService y availabilitySubscriptionService se verifican con verify().
 */
@ExtendWith(MockitoExtension.class)
class BookingServiceCancelTest {

    // -----------------------------------------------------------------------
    // Constantes de tiempo
    // -----------------------------------------------------------------------

    /** "Ahora" fijo: 2025-09-15 a las 08:00 (horario Lima). */
    private static final LocalDateTime NOW = LocalDateTime.of(2025, 9, 15, 8, 0);

    /** Reserva FUTURA: fecha posterior a NOW, la cancelación debe proceder. */
    private static final LocalDate  FUTURE_DATE  = LocalDate.of(2025, 9, 16);
    private static final LocalTime  FUTURE_START = LocalTime.of(9, 0);
    private static final LocalTime  FUTURE_END   = LocalTime.of(10, 0);

    /** Reserva PASADA: fecha anterior a NOW, la cancelación debe rechazarse. */
    private static final LocalDate  PAST_DATE  = LocalDate.of(2025, 9, 14);
    private static final LocalTime  PAST_START = LocalTime.of(9, 0);
    private static final LocalTime  PAST_END   = LocalTime.of(10, 0);

    private static final Long BOOKING_ID   = 1L;
    private static final Long STUDENT_ID   = 10L;
    private static final Long ADMIN_ID     = 1L;

    // -----------------------------------------------------------------------
    // Mocks
    // -----------------------------------------------------------------------

    @Mock private ReservationRepository           reservationRepository;
    @Mock private RoomService                     roomService;
    @Mock private UserService                     userService;
    @Mock private EmailOutboxService              emailOutboxService;
    @Mock private AuditService                    auditService;
    @Mock private DtoMapper                       dtoMapper;
    @Mock private BookingValidationService        bookingValidationService;
    @Mock private EmailTemplateService            emailTemplateService;
    @Mock private AvailabilitySubscriptionService availabilitySubscriptionService;

    @InjectMocks
    private BookingService sut;

    // -----------------------------------------------------------------------
    // Objetos base
    // -----------------------------------------------------------------------

    private UserEntity studentUser;
    private RoomEntity room;
    private BookingResponse stubbedDto;

    @BeforeEach
    void setUp() {
        RoleEntity studentRole = new RoleEntity();
        studentRole.setId(2L);
        studentRole.setNombre("ESTUDIANTE");

        studentUser = new UserEntity();
        studentUser.setId(STUDENT_ID);
        studentUser.setNombres("Luis");
        studentUser.setApellidos("García");
        studentUser.setCorreo("luis@test.edu");
        studentUser.setRol(studentRole);

        room = new RoomEntity();
        room.setId(5L);
        room.setCodigo("A-101");
        room.setNombre("Aula A-101");
        room.setUbicacion("Pabellón A");

        stubbedDto = new BookingResponse(
            BOOKING_ID, STUDENT_ID, "luis@test.edu",
            5L, "A-101", "Aula A-101", "Pabellón A",
            3, FUTURE_DATE, FUTURE_START, FUTURE_END,
            "CANCELADA", null,
            "https://calendar.google.com/stub",
            "/api/bookings/1/ics"
        );
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Construye una reserva en estado dado con fechas futuras. */
    private ReservationEntity buildReservation(ReservationStatus status) {
        return buildReservation(status, FUTURE_DATE, FUTURE_START, FUTURE_END);
    }

    private ReservationEntity buildReservation(
        ReservationStatus status,
        LocalDate date, LocalTime start, LocalTime end
    ) {
        ReservationEntity r = new ReservationEntity();
        r.setId(BOOKING_ID);
        r.setUsuario(studentUser);
        r.setSala(room);
        r.setFecha(date);
        r.setHoraInicio(start);
        r.setHoraFin(end);
        r.setEstado(status);
        r.setCantidadPersonas(3);
        return r;
    }

    // -----------------------------------------------------------------------
    // CX-01: Reserva activa, actor estudiante → se cancela y retorna DTO correcto
    // -----------------------------------------------------------------------

    /**
     * Camino feliz con adminCancel=false.
     * Verifica:
     *   - El estado persisitido es CANCELADA.
     *   - updatedBy es el ID del estudiante.
     *   - Se llama reservationRepository.save() exactamente una vez.
     *   - Se retorna el DTO mapeado.
     */
    @Test
    @DisplayName("CX-01: Reserva activa cancelada por estudiante cambia estado a CANCELADA")
    void cx01_activeBooking_studentCancels_becomesCANCELADA() {
        try (MockedStatic<AppTime> appTime = mockStatic(AppTime.class)) {
            appTime.when(AppTime::nowDateTime).thenReturn(NOW);

            ReservationEntity booking = buildReservation(ReservationStatus.ACTIVA);
            when(reservationRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
            when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(emailTemplateService.bookingStatus(any(), anyString(), anyString(), anyString()))
                .thenReturn("<html>cancelada</html>");
            when(dtoMapper.toBooking(any())).thenReturn(stubbedDto);

            BookingResponse result = sut.cancelBooking(BOOKING_ID, STUDENT_ID, false);

            // Estado persistido
            ArgumentCaptor<ReservationEntity> saved = ArgumentCaptor.forClass(ReservationEntity.class);
            verify(reservationRepository).save(saved.capture());
            assertEquals(ReservationStatus.CANCELADA, saved.getValue().getEstado(),
                "El estado debe cambiarse a CANCELADA");
            assertEquals(STUDENT_ID, saved.getValue().getUpdatedBy(),
                "updatedBy debe ser el ID del estudiante");

            // Retorno correcto
            assertNotNull(result, "El método debe retornar un BookingResponse");
            assertEquals("CANCELADA", result.status());
        }
    }

    // -----------------------------------------------------------------------
    // CX-02: Reserva activa, actor admin → razón contiene "administrador"
    // -----------------------------------------------------------------------

    /**
     * Verifica que cuando adminCancel=true la razón pasada al template
     * incluya la palabra "administrador".
     */
    @Test
    @DisplayName("CX-02: Reserva activa cancelada por admin incluye razón 'administrador' en la notificación")
    void cx02_activeBooking_adminCancels_reasonMentionsAdmin() {
        try (MockedStatic<AppTime> appTime = mockStatic(AppTime.class)) {
            appTime.when(AppTime::nowDateTime).thenReturn(NOW);

            ReservationEntity booking = buildReservation(ReservationStatus.ACTIVA);
            when(reservationRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
            when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(dtoMapper.toBooking(any())).thenReturn(stubbedDto);

            // Capturar el "detail" (4.º argumento) que se le pasa al template
            ArgumentCaptor<String> detailCaptor = ArgumentCaptor.forClass(String.class);
            when(emailTemplateService.bookingStatus(
                any(), anyString(), anyString(), detailCaptor.capture()))
                .thenReturn("<html>cancelada admin</html>");

            sut.cancelBooking(BOOKING_ID, ADMIN_ID, true);

            String detail = detailCaptor.getValue();
            assertNotNull(detail, "El detail de la notificación no debe ser nulo");
            org.junit.jupiter.api.Assertions.assertTrue(
                detail.contains("administrador"),
                "El detalle debe mencionar 'administrador' cuando adminCancel=true, pero fue: " + detail
            );
        }
    }

    // -----------------------------------------------------------------------
    // CX-03: Reserva ya CANCELADA → skip path: sin save ni notificación
    // -----------------------------------------------------------------------

    /**
     * Cuando la reserva ya está cancelada el método retorna el DTO directamente
     * sin guardar ni encolar ningún email ni notificar suscriptores.
     */
    @Test
    @DisplayName("CX-03: Reserva ya CANCELADA se salta sin guardar ni notificar")
    void cx03_alreadyCancelledBooking_skipsWithoutSideEffects() {
        // AppTime NO se mockea: el código ni siquiera llega a llamarlo en el skip path
        ReservationEntity booking = buildReservation(ReservationStatus.CANCELADA);
        when(reservationRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
        when(dtoMapper.toBooking(booking)).thenReturn(stubbedDto);

        BookingResponse result = sut.cancelBooking(BOOKING_ID, STUDENT_ID, false);

        // No debe persistirse ni notificarse nada
        verify(reservationRepository, never()).save(any());
        verify(emailOutboxService, never()).enqueue(any(), anyString(), anyString(), anyString());
        verify(availabilitySubscriptionService, never()).notifySubscribers(any(), any(), any(), any());

        assertNotNull(result, "Debe retornar el DTO aunque la reserva ya estuviera cancelada");
    }

    // -----------------------------------------------------------------------
    // CX-04: Reserva cuya horaFin ya pasó → BusinessException BAD_REQUEST
    // -----------------------------------------------------------------------

    /**
     * NOW = 2025-09-15 08:00.
     * La reserva tiene fecha 2025-09-14 con horaFin 10:00.
     * Condición: 2025-09-14T10:00 <= 2025-09-15T08:00  → no isAfter → lanza excepción.
     */
    @Test
    @DisplayName("CX-04: Reserva ya finalizada lanza BusinessException BAD_REQUEST")
    void cx04_pastReservation_throwsBadRequest() {
        try (MockedStatic<AppTime> appTime = mockStatic(AppTime.class)) {
            appTime.when(AppTime::nowDateTime).thenReturn(NOW);

            ReservationEntity booking = buildReservation(
                ReservationStatus.ACTIVA, PAST_DATE, PAST_START, PAST_END);
            when(reservationRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));

            BusinessException ex = assertThrows(BusinessException.class,
                () -> sut.cancelBooking(BOOKING_ID, STUDENT_ID, false));

            assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus(),
                "El status debe ser 400 BAD_REQUEST");
            assertEquals("No se puede cancelar una reserva que ya finalizo", ex.getMessage());

            // No debe haberse persistido nada
            verify(reservationRepository, never()).save(any());
        }
    }

    // -----------------------------------------------------------------------
    // CX-05: Cancelación exitosa → exactamente 1 email encolado con tipo correcto
    // -----------------------------------------------------------------------

    /**
     * Verifica que:
     *   - emailOutboxService.enqueue() se llama exactamente una vez.
     *   - El payload contiene "BOOKING_CANCELLATION".
     *   - availabilitySubscriptionService.notifySubscribers() se invoca una vez.
     */
    @Test
    @DisplayName("CX-05: Cancelación exitosa encola exactamente 1 notificación BOOKING_CANCELLATION")
    void cx05_successfulCancellation_enqueuesExactlyOneNotification() {
        try (MockedStatic<AppTime> appTime = mockStatic(AppTime.class)) {
            appTime.when(AppTime::nowDateTime).thenReturn(NOW);

            ReservationEntity booking = buildReservation(ReservationStatus.ACTIVA);
            when(reservationRepository.findById(BOOKING_ID)).thenReturn(Optional.of(booking));
            when(reservationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
            when(emailTemplateService.bookingStatus(any(), anyString(), anyString(), anyString()))
                .thenReturn("<html>ok</html>");
            when(dtoMapper.toBooking(any())).thenReturn(stubbedDto);

            // Capturar los argumentos del enqueue
            ArgumentCaptor<String> subjectCaptor  = ArgumentCaptor.forClass(String.class);
            ArgumentCaptor<String> payloadCaptor  = ArgumentCaptor.forClass(String.class);

            sut.cancelBooking(BOOKING_ID, STUDENT_ID, false);

            // Exactamente una llamada a enqueue
            verify(emailOutboxService, times(1))
                .enqueue(eq(studentUser), subjectCaptor.capture(), anyString(), payloadCaptor.capture());

            assertEquals("Reserva cancelada", subjectCaptor.getValue(),
                "El asunto del email debe ser 'Reserva cancelada'");
            org.junit.jupiter.api.Assertions.assertTrue(
                payloadCaptor.getValue().contains("BOOKING_CANCELLATION"),
                "El payload debe contener el tipo BOOKING_CANCELLATION"
            );

            // Exactamente una notificación a suscriptores de disponibilidad
            verify(availabilitySubscriptionService, times(1))
                .notifySubscribers(
                    eq(room),
                    eq(FUTURE_DATE),
                    eq(FUTURE_START),
                    eq(FUTURE_END)
                );
        }
    }
}
