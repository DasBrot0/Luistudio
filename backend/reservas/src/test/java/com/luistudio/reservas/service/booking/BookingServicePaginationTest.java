package com.luistudio.reservas.service.booking;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.luistudio.reservas.dto.booking.BookingResponse;
import com.luistudio.reservas.dto.common.PageResponse;
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
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Pruebas Unitarias — Normalización de Paginación en BookingService
 *
 * Métodos evaluados:
 *   BookingService.listMyBookings(Long userId, int page, int size)
 *   BookingService.listAdminBookings(int page, int size, String status, LocalDate date)
 *
 * Lógica de normalización en ambos métodos:
 *   int safePage = Math.max(page, 0);               → página negativa → 0
 *   int safeSize = Math.min(Math.max(size, 1), 50); → tamaño < 1 → 1 ; tamaño > 50 → 50
 *
 * Casos cubiertos:
 *   PG-01  Página negativa se normaliza a 0                 (listMyBookings)
 *   PG-02  Tamaño menor a 1 se normaliza a 1                (listMyBookings)
 *   PG-03  Tamaño mayor a 50 se limita a 50                 (listMyBookings)
 *   PG-04  Página y tamaño válidos se pasan sin cambios     (listMyBookings)
 *   PG-05  Filtro de estado válido se enruta correctamente  (listAdminBookings)
 *
 * Estrategia de verificación:
 *   Se usa ArgumentCaptor<Pageable> para capturar el PageRequest que el servicio
 *   construye internamente y verificar que sus propiedades (pageNumber, pageSize)
 *   reflejen exactamente los valores normalizados, independientemente del valor
 *   de entrada bruto recibido por el método.
 */
@ExtendWith(MockitoExtension.class)
class BookingServicePaginationTest {

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

    @Captor
    private ArgumentCaptor<Pageable> pageableCaptor;

    // -----------------------------------------------------------------------
    // Objetos base reutilizables
    // -----------------------------------------------------------------------

    private static final Long USER_ID = 10L;

    private UserEntity user;
    private ReservationEntity sampleReservation;

    @BeforeEach
    void setUp() {
        RoleEntity role = new RoleEntity();
        role.setId(2L);
        role.setNombre("ESTUDIANTE");

        user = new UserEntity();
        user.setId(USER_ID);
        user.setNombres("Ana");
        user.setApellidos("Torres");
        user.setCorreo("ana@test.edu");
        user.setRol(role);

        RoomEntity room = new RoomEntity();
        room.setId(1L);
        room.setCodigo("B-201");
        room.setNombre("Sala B-201");
        room.setUbicacion("Pabellón B");

        sampleReservation = new ReservationEntity();
        sampleReservation.setId(100L);
        sampleReservation.setUsuario(user);
        sampleReservation.setSala(room);
        sampleReservation.setFecha(LocalDate.of(2025, 9, 1));
        sampleReservation.setHoraInicio(LocalTime.of(10, 0));
        sampleReservation.setHoraFin(LocalTime.of(11, 0));
        sampleReservation.setEstado(ReservationStatus.ACTIVA);
        sampleReservation.setCantidadPersonas(3);
    }

    // -----------------------------------------------------------------------
    // Helper: construye una PageImpl con pageNumber/pageSize provenientes
    // del Pageable recibido, para que PageResponse refleje los valores reales.
    // -----------------------------------------------------------------------

    private PageImpl<ReservationEntity> pageOf(List<ReservationEntity> items, Pageable pageable) {
        return new PageImpl<>(items, pageable, items.size());
    }

    // -----------------------------------------------------------------------
    // PG-01: Página negativa → se normaliza a 0
    // -----------------------------------------------------------------------

    /**
     * Entrada: page = -5 → safePage = Math.max(-5, 0) = 0
     * Verificación: el PageRequest capturado tiene pageNumber == 0.
     */
    @Test
    @DisplayName("PG-01: Página negativa (-5) se normaliza a 0 en listMyBookings")
    void pg01_negativePage_normalizesToZero() {
        when(userService.getById(USER_ID)).thenReturn(user);
        when(reservationRepository.findByUsuarioOrderByFechaDescHoraInicioDesc(
                eq(user), pageableCaptor.capture()))
            .thenAnswer(inv -> pageOf(List.of(sampleReservation), inv.getArgument(1)));
        when(dtoMapper.toBooking(any())).thenReturn(stubbedResponse());

        PageResponse<BookingResponse> response = sut.listMyBookings(USER_ID, -5, 10);

        Pageable captured = pageableCaptor.getValue();
        assertEquals(0, captured.getPageNumber(),
            "Una página negativa debe normalizarse a 0");
        assertEquals(10, captured.getPageSize(),
            "El tamaño válido no debe modificarse");

        // PageResponse refleja los valores reales del Page devuelto
        assertEquals(0, response.page(), "El número de página en la respuesta debe ser 0");
        assertEquals(10, response.size(), "El tamaño en la respuesta debe ser 10");
        assertEquals(1, response.totalElements(), "Debe haber 1 elemento en total");
    }

    // -----------------------------------------------------------------------
    // PG-02: Tamaño menor a 1 → se normaliza a 1
    // -----------------------------------------------------------------------

    /**
     * Entrada: size = 0 → safeSize = Math.min(Math.max(0, 1), 50) = 1
     * Verificación: el PageRequest capturado tiene pageSize == 1.
     */
    @Test
    @DisplayName("PG-02: Tamaño 0 se normaliza a 1 en listMyBookings")
    void pg02_sizeZero_normalizesToOne() {
        when(userService.getById(USER_ID)).thenReturn(user);
        when(reservationRepository.findByUsuarioOrderByFechaDescHoraInicioDesc(
                eq(user), pageableCaptor.capture()))
            .thenAnswer(inv -> pageOf(List.of(sampleReservation), inv.getArgument(1)));
        when(dtoMapper.toBooking(any())).thenReturn(stubbedResponse());

        PageResponse<BookingResponse> response = sut.listMyBookings(USER_ID, 0, 0);

        Pageable captured = pageableCaptor.getValue();
        assertEquals(1, captured.getPageSize(),
            "Un tamaño de 0 debe normalizarse a 1 (tamaño mínimo)");
        assertEquals(0, captured.getPageNumber());

        assertEquals(1, response.size(), "El tamaño mínimo en la respuesta debe ser 1");
    }

    // -----------------------------------------------------------------------
    // PG-03: Tamaño mayor a 50 → se limita a 50
    // -----------------------------------------------------------------------

    /**
     * Entrada: size = 200 → safeSize = Math.min(Math.max(200, 1), 50) = 50
     * Verificación: el PageRequest capturado tiene pageSize == 50.
     */
    @Test
    @DisplayName("PG-03: Tamaño 200 se limita al máximo de 50 en listMyBookings")
    void pg03_sizeExceedsMax_clampedToFifty() {
        when(userService.getById(USER_ID)).thenReturn(user);
        when(reservationRepository.findByUsuarioOrderByFechaDescHoraInicioDesc(
                eq(user), pageableCaptor.capture()))
            .thenAnswer(inv -> pageOf(List.of(sampleReservation), inv.getArgument(1)));
        when(dtoMapper.toBooking(any())).thenReturn(stubbedResponse());

        PageResponse<BookingResponse> response = sut.listMyBookings(USER_ID, 0, 200);

        Pageable captured = pageableCaptor.getValue();
        assertEquals(50, captured.getPageSize(),
            "Un tamaño de 200 debe recortarse al máximo permitido de 50");

        assertEquals(50, response.size(), "El tamaño máximo en la respuesta debe ser 50");
    }

    // -----------------------------------------------------------------------
    // PG-04: Página y tamaño válidos → se pasan sin cambios
    // -----------------------------------------------------------------------

    /**
     * Entrada: page = 2, size = 15 → ambos dentro del rango permitido.
     * Verificación: el PageRequest capturado refleja exactamente los valores de entrada.
     * También verifica la estructura completa de PageResponse.
     */
    @Test
    @DisplayName("PG-04: Página 2 y tamaño 15 válidos se pasan sin modificación en listMyBookings")
    void pg04_validPageAndSize_passedThrough() {
        when(userService.getById(USER_ID)).thenReturn(user);
        when(reservationRepository.findByUsuarioOrderByFechaDescHoraInicioDesc(
                eq(user), pageableCaptor.capture()))
            .thenAnswer(inv -> {
                Pageable p = inv.getArgument(1);
                // Simulamos una segunda página: 1 elemento en la página, 31 en total → 3 páginas
                return new PageImpl<>(List.of(sampleReservation), p, 31L);
            });
        when(dtoMapper.toBooking(any())).thenReturn(stubbedResponse());

        PageResponse<BookingResponse> response = sut.listMyBookings(USER_ID, 2, 15);

        Pageable captured = pageableCaptor.getValue();
        assertEquals(2, captured.getPageNumber(),
            "La página válida debe transmitirse sin cambios");
        assertEquals(15, captured.getPageSize(),
            "El tamaño válido debe transmitirse sin cambios");

        // Verificar la estructura completa de PageResponse
        assertEquals(2,  response.page(),          "page debe ser 2");
        assertEquals(15, response.size(),           "size debe ser 15");
        assertEquals(31, response.totalElements(), "totalElements debe ser 31");
        assertEquals(3,  response.totalPages(),     "totalPages debe ser 3 (ceil(31/15))");
        assertEquals(1,  response.content().size(), "content debe tener 1 elemento");
    }

    // -----------------------------------------------------------------------
    // PG-05: Filtro de estado válido → enruta a findByEstadoOrderByFechaDesc...
    // -----------------------------------------------------------------------

    /**
     * Entrada para listAdminBookings: status = "ACTIVA", date = null.
     * La normalización de paginación también aplica: page = -1 → 0, size = 5 → 5.
     * Verificación:
     *   - Se llama findByEstadoOrderByFechaDescHoraInicioDesc (ruta estado-solo).
     *   - El PageRequest tiene pageNumber=0, pageSize=5.
     *   - PageResponse tiene status correcto.
     */
    @Test
    @DisplayName("PG-05: Filtro de estado ACTIVA con página negativa se normaliza y enruta correctamente en listAdminBookings")
    void pg05_validStatusFilter_routesToStatusQuery_withNormalizedPage() {
        when(reservationRepository.findByEstadoOrderByFechaDescHoraInicioDesc(
                eq(ReservationStatus.ACTIVA), pageableCaptor.capture()))
            .thenAnswer(inv -> pageOf(List.of(sampleReservation), inv.getArgument(1)));
        when(dtoMapper.toBooking(any())).thenReturn(stubbedResponse());

        PageResponse<BookingResponse> response = sut.listAdminBookings(-1, 5, "ACTIVA", null);

        // Verificar normalización de paginación
        Pageable captured = pageableCaptor.getValue();
        assertEquals(0, captured.getPageNumber(),
            "La página -1 debe normalizarse a 0 en listAdminBookings");
        assertEquals(5, captured.getPageSize(),
            "El tamaño válido de 5 no debe modificarse");

        // Verificar que la ruta correcta del repositorio fue invocada
        verify(reservationRepository).findByEstadoOrderByFechaDescHoraInicioDesc(
            eq(ReservationStatus.ACTIVA), any(Pageable.class));

        // Verificar estructura de PageResponse
        assertEquals(0, response.page());
        assertEquals(5, response.size());
        assertEquals(1, response.totalElements());
        assertEquals(1, response.content().size());
    }

    // -----------------------------------------------------------------------
    // Helper: BookingResponse stub mínimo para que dtoMapper no devuelva null
    // -----------------------------------------------------------------------

    private BookingResponse stubbedResponse() {
        return new BookingResponse(
            100L, USER_ID, "ana@test.edu",
            1L, "B-201", "Sala B-201", "Pabellón B",
            3,
            LocalDate.of(2025, 9, 1), LocalTime.of(10, 0), LocalTime.of(11, 0),
            "ACTIVA", null, "https://calendar.google.com/stub", "/api/bookings/100/ics"
        );
    }
}
