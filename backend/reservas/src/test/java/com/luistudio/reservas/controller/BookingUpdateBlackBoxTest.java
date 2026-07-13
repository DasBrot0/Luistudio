package com.luistudio.reservas.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.luistudio.reservas.dto.booking.BookingResponse;
import com.luistudio.reservas.dto.booking.BookingUpsertRequest;
import com.luistudio.reservas.exception.BusinessException;
import com.luistudio.reservas.exception.GlobalExceptionHandler;
import com.luistudio.reservas.exception.NotFoundException;
import com.luistudio.reservas.security.AuthPrincipal;
import com.luistudio.reservas.service.AccessGuard;
import com.luistudio.reservas.service.BookingService;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * ============================================================
 * Prueba de Caja Negra #2 — PUT /api/bookings/{bookingId} (updateBooking)
 * ============================================================
 *
 * Funcionalidad evaluada
 * ----------------------
 *   PUT /api/bookings/{bookingId}
 *   Controlador: BookingController.updateBooking(Long bookingId, BookingUpsertRequest)
 *
 * Criterio de caja negra
 * ----------------------
 *   Las pruebas se diseñan exclusivamente desde el contrato externo del endpoint:
 *     · Path variable: bookingId (Long).
 *     · Campos del cuerpo (7): roomId, date, start, end, people, location, observation.
 *     · Respuestas HTTP esperadas: 200 OK, 400 Bad Request, 404 Not Found.
 *     · Mensajes de error documentados.
 *   No se inspecciona ni se presupone ningún detalle de implementación interna.
 *
 * Campos de entrada
 * -----------------
 *   Path:  bookingId (Long, requerido)
 *   Body:  BookingUpsertRequest
 *     roomId      @NotNull Long
 *     date        @NotNull LocalDate
 *     start       @NotNull LocalTime
 *     end         @NotNull LocalTime
 *     people      @NotNull @Min(1) Integer
 *     location    @NotBlank @Size(max=120) String
 *     observation @Size(max=255) String   (opcional)
 *
 * Casos de prueba
 * ---------------
 * | ID         | Descripción                                       | Entrada                                                     | HTTP esperado | Resultado esperado                                     |
 * |------------|---------------------------------------------------|-------------------------------------------------------------|---------------|--------------------------------------------------------|
 * | CN-UPD-01  | Modificar reserva existente con datos válidos     | bookingId=10, roomId=1, fecha futura, slot válido, people=5 | 200 OK        | BookingResponse con id=10 y estado ACTIVA              |
 * | CN-UPD-02  | Modificar reserva inexistente                     | bookingId=999, datos válidos                                | 404           | message = "Reserva no encontrada"                      |
 * | CN-UPD-03  | Modificar reserva con sala inexistente            | bookingId=10, roomId=9999 (no existe), resto válido         | 404           | message = "Sala no encontrada"                         |
 * | CN-UPD-04  | Modificar reserva con horario inválido            | bookingId=10, start=10:00, end=09:00 (end < start)         | 400           | message = "La hora fin debe ser mayor a la hora inicio"|
 * | CN-UPD-05  | Modificar reserva generando conflicto de dis.     | bookingId=10, datos válidos, sala ya ocupada en ese slot    | 400           | message = "La sala no está disponible para el horario" |
 *
 * Estrategia de mock
 * ------------------
 *   · AccessGuard.requireUser() siempre retorna un AuthPrincipal (usuario autenticado).
 *     LENIENT para que los tests donde Bean Validation rechaza antes no fallen.
 *   · BookingService.updateBooking(bookingId, userId, request) se controla por caso:
 *       - CN-UPD-01: retorna BookingResponse con id=10 y status=ACTIVA.
 *       - CN-UPD-02: lanza NotFoundException("Reserva no encontrada").
 *       - CN-UPD-03: lanza NotFoundException("Sala no encontrada").
 *       - CN-UPD-04: lanza BusinessException(400, "La hora fin debe ser mayor…").
 *       - CN-UPD-05: lanza BusinessException(400, "La sala no está disponible…").
 *   · MockMvc standalone + GlobalExceptionHandler aseguran que los códigos HTTP
 *     y los cuerpos de error llegan correctamente al cliente.
 * ============================================================
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BookingUpdateBlackBoxTest {

    // -----------------------------------------------------------------------
    // Constantes de tiempo: próximo lunes a las 09:00-10:00 (slot siempre futuro)
    // -----------------------------------------------------------------------

    /** Fecha fija = próximo lunes; siempre en el futuro respecto a LocalDate.now(). */
    private static final LocalDate VALID_DATE  = nextMonday();
    private static final LocalTime VALID_START = LocalTime.of(9, 0);
    private static final LocalTime VALID_END   = LocalTime.of(10, 0);

    /** ID de reserva existente usada en los casos exitosos/de negocio. */
    private static final long EXISTING_BOOKING_ID  = 10L;

    /** ID de reserva que el servicio no encontrará. */
    private static final long MISSING_BOOKING_ID   = 999L;

    /** ID de sala que el servicio no encontrará. */
    private static final long MISSING_ROOM_ID      = 9999L;

    // -----------------------------------------------------------------------
    // Colaboradores
    // -----------------------------------------------------------------------

    @Mock private BookingService bookingService;
    @Mock private AccessGuard    accessGuard;

    @InjectMocks
    private BookingController controller;

    private MockMvc      mockMvc;
    private ObjectMapper objectMapper;

    // -----------------------------------------------------------------------
    // Setup
    // -----------------------------------------------------------------------

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Usuario autenticado por defecto en todos los tests.
        when(accessGuard.requireUser())
            .thenReturn(new AuthPrincipal(1L, "user@test.com", "ESTUDIANTE"));
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Construye un BookingUpsertRequest completamente válido para la mayoría de casos.
     *
     * @param roomId identificador de la sala a reservar
     */
    private BookingUpsertRequest validRequest(long roomId) {
        return new BookingUpsertRequest(
            roomId,
            VALID_DATE,
            VALID_START,
            VALID_END,
            5,
            "Monterrico - Aula A1",
            null
        );
    }

    /**
     * Construye un BookingResponse de éxito para el stub del servicio.
     *
     * @param bookingId id de la reserva actualizada
     * @param roomId    id de la nueva sala asignada
     */
    private BookingResponse successResponse(long bookingId, long roomId) {
        return new BookingResponse(
            bookingId,               // id
            1L,                      // userId
            "user@test.com",         // userEmail
            roomId,                  // roomId
            "A101",                  // roomCode
            "Aula 101",              // roomName
            "Monterrico - Aula A1",  // location
            5,                       // people
            VALID_DATE,              // date
            VALID_START,             // start
            VALID_END,               // end
            "ACTIVA",                // status
            null,                    // observation
            null,                    // googleCalendarUrl
            null                     // icsUrl
        );
    }

    // -----------------------------------------------------------------------
    // CN-UPD-01 — Modificar reserva existente con datos válidos
    //
    // Datos de entrada:
    //   PUT /api/bookings/10
    //   Body: roomId=1, date=próximo lunes, start=09:00, end=10:00,
    //         people=5, location="Monterrico - Aula A1", observation=null
    //
    // Resultado esperado: HTTP 200, body con id=10, status="ACTIVA"
    // Resultado obtenido: HTTP 200 ✓
    // -----------------------------------------------------------------------

    /**
     * Verifica que modificar una reserva existente con todos los campos válidos
     * retorna HTTP 200 con el BookingResponse actualizado.
     * Partición de equivalencia: todas las entradas en el dominio válido.
     */
    @Test
    @DisplayName("CN-UPD-01: Modificar reserva existente con datos válidos — HTTP 200 con BookingResponse actualizado")
    void shouldReturn200WhenUpdateIsValid() throws Exception {
        // Arrange
        when(bookingService.updateBooking(
                eq(EXISTING_BOOKING_ID), eq(1L), any(BookingUpsertRequest.class)))
            .thenReturn(successResponse(EXISTING_BOOKING_ID, 1L));

        // Act & Assert
        MvcResult result = mockMvc.perform(
                put("/api/bookings/{id}", EXISTING_BOOKING_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRequest(1L)))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(EXISTING_BOOKING_ID))
            .andExpect(jsonPath("$.status").value("ACTIVA"))
            .andReturn();

        assertEquals(200, result.getResponse().getStatus(),
            "CN-UPD-01: Se esperaba HTTP 200 al modificar una reserva válida");
        assertNotNull(result.getResponse().getContentAsString(),
            "El cuerpo de la respuesta no debe ser nulo");
    }

    // -----------------------------------------------------------------------
    // CN-UPD-02 — Modificar reserva inexistente
    //
    // Datos de entrada:
    //   PUT /api/bookings/999
    //   Body: datos completamente válidos
    //
    // Resultado esperado: HTTP 404, message = "Reserva no encontrada"
    // Resultado obtenido: HTTP 404 ✓ (NotFoundException propagada por GlobalExceptionHandler)
    // -----------------------------------------------------------------------

    /**
     * Verifica que intentar modificar un bookingId que no existe en el sistema
     * retorna HTTP 404 con el mensaje "Reserva no encontrada".
     * Partición de equivalencia: bookingId fuera del dominio de reservas existentes.
     */
    @Test
    @DisplayName("CN-UPD-02: Modificar reserva inexistente — HTTP 404 con mensaje de error")
    void shouldReturn404WhenBookingDoesNotExist() throws Exception {
        // Arrange — el servicio lanza NotFoundException para bookingId=999
        when(bookingService.updateBooking(
                eq(MISSING_BOOKING_ID), eq(1L), any(BookingUpsertRequest.class)))
            .thenThrow(new NotFoundException("Reserva no encontrada"));

        // Act & Assert
        MvcResult result = mockMvc.perform(
                put("/api/bookings/{id}", MISSING_BOOKING_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRequest(1L)))
            )
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("Reserva no encontrada"))
            .andReturn();

        assertEquals(404, result.getResponse().getStatus(),
            "CN-UPD-02: Se esperaba HTTP 404 para una reserva inexistente");
    }

    // -----------------------------------------------------------------------
    // CN-UPD-03 — Modificar reserva con sala inexistente
    //
    // Datos de entrada:
    //   PUT /api/bookings/10
    //   Body: roomId=9999 (sala que no existe), resto válido
    //
    // Resultado esperado: HTTP 404, message = "Sala no encontrada"
    // Resultado obtenido: HTTP 404 ✓ (NotFoundException propagada por GlobalExceptionHandler)
    // -----------------------------------------------------------------------

    /**
     * Verifica que referenciar un roomId que no existe en el sistema
     * retorna HTTP 404 con el mensaje "Sala no encontrada".
     * Partición de equivalencia: roomId fuera del dominio de salas existentes.
     */
    @Test
    @DisplayName("CN-UPD-03: Modificar reserva con sala inexistente — HTTP 404 con mensaje de error")
    void shouldReturn404WhenRoomDoesNotExist() throws Exception {
        // Arrange — la sala 9999 no existe; el servicio lanza NotFoundException
        when(bookingService.updateBooking(
                eq(EXISTING_BOOKING_ID), eq(1L), any(BookingUpsertRequest.class)))
            .thenThrow(new NotFoundException("Sala no encontrada"));

        // Act & Assert
        MvcResult result = mockMvc.perform(
                put("/api/bookings/{id}", EXISTING_BOOKING_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRequest(MISSING_ROOM_ID)))
            )
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.message").value("Sala no encontrada"))
            .andReturn();

        assertEquals(404, result.getResponse().getStatus(),
            "CN-UPD-03: Se esperaba HTTP 404 al referenciar una sala inexistente");
    }

    // -----------------------------------------------------------------------
    // CN-UPD-04 — Modificar reserva con horario inválido (end < start)
    //
    // Datos de entrada:
    //   PUT /api/bookings/10
    //   Body: roomId=1, date=próximo lunes, start=10:00, end=09:00,
    //         people=5, location="Monterrico - Aula A1", observation=null
    //
    // Resultado esperado: HTTP 400, message = "La hora fin debe ser mayor a la hora inicio"
    // Resultado obtenido: HTTP 400 ✓ (BusinessException propagada por GlobalExceptionHandler)
    // -----------------------------------------------------------------------

    /**
     * Verifica que enviar un horario donde end < start retorna HTTP 400.
     * Ambos campos pasan Bean Validation (no son nulos y son LocalTime válidos);
     * la regla de negocio end > start es verificada por la capa de servicio.
     * Partición de equivalencia: horario con orden temporal invertido.
     */
    @Test
    @DisplayName("CN-UPD-04: Modificar reserva con horario inválido (end < start) — HTTP 400 con mensaje de negocio")
    void shouldReturn400WhenEndTimeIsBeforeStartTime() throws Exception {
        // Arrange — la capa de negocio rechaza end <= start
        when(bookingService.updateBooking(
                eq(EXISTING_BOOKING_ID), eq(1L), any(BookingUpsertRequest.class)))
            .thenThrow(new BusinessException(
                HttpStatus.BAD_REQUEST,
                "La hora fin debe ser mayor a la hora inicio"
            ));

        // start=10:00, end=09:00 → end < start (horario invertido)
        BookingUpsertRequest badRequest = new BookingUpsertRequest(
            1L,
            VALID_DATE,
            LocalTime.of(10, 0),   // start
            LocalTime.of(9, 0),    // end < start
            5,
            "Monterrico - Aula A1",
            null
        );

        // Act & Assert
        MvcResult result = mockMvc.perform(
                put("/api/bookings/{id}", EXISTING_BOOKING_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(badRequest))
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("La hora fin debe ser mayor a la hora inicio"))
            .andReturn();

        assertEquals(400, result.getResponse().getStatus(),
            "CN-UPD-04: Se esperaba HTTP 400 para un horario con end < start");
    }

    // -----------------------------------------------------------------------
    // CN-UPD-05 — Modificar reserva generando conflicto de disponibilidad
    //
    // Datos de entrada:
    //   PUT /api/bookings/10
    //   Body: roomId=1, date=próximo lunes, start=09:00, end=10:00,
    //         people=5, location="Monterrico - Aula A1", observation=null
    //   (el slot 09:00-10:00 ya está ocupado por otra reserva)
    //
    // Resultado esperado: HTTP 400,
    //   message = "La sala no está disponible para el horario seleccionado"
    // Resultado obtenido: HTTP 400 ✓ (BusinessException propagada por GlobalExceptionHandler)
    // -----------------------------------------------------------------------

    /**
     * Verifica que intentar mover una reserva a un slot ya ocupado por otra reserva
     * retorna HTTP 400 con el mensaje de conflicto de disponibilidad.
     * Partición de equivalencia: slot válido pero no disponible (solapamiento).
     */
    @Test
    @DisplayName("CN-UPD-05: Modificar reserva generando conflicto de disponibilidad — HTTP 400 con mensaje de negocio")
    void shouldReturn400WhenSlotIsAlreadyTaken() throws Exception {
        // Arrange — la sala ya tiene otra reserva en ese slot
        when(bookingService.updateBooking(
                eq(EXISTING_BOOKING_ID), eq(1L), any(BookingUpsertRequest.class)))
            .thenThrow(new BusinessException(
                HttpStatus.BAD_REQUEST,
                "La sala no está disponible para el horario seleccionado"
            ));

        // Act & Assert — el slot 09:00-10:00 está ocupado
        MvcResult result = mockMvc.perform(
                put("/api/bookings/{id}", EXISTING_BOOKING_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(validRequest(1L)))
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message")
                .value("La sala no está disponible para el horario seleccionado"))
            .andReturn();

        assertEquals(400, result.getResponse().getStatus(),
            "CN-UPD-05: Se esperaba HTTP 400 cuando el slot ya está ocupado por otra reserva");
    }

    // -----------------------------------------------------------------------
    // Utilidad estática
    // -----------------------------------------------------------------------

    /**
     * Devuelve el próximo lunes.
     * Si hoy ya es lunes, avanza una semana para garantizar que VALID_DATE
     * siempre esté en el futuro respecto a LocalDate.now().
     */
    private static LocalDate nextMonday() {
        LocalDate today = LocalDate.now();
        int daysUntilMonday = (DayOfWeek.MONDAY.getValue() - today.getDayOfWeek().getValue() + 7) % 7;
        return today.plusDays(daysUntilMonday == 0 ? 7 : daysUntilMonday);
    }
}
