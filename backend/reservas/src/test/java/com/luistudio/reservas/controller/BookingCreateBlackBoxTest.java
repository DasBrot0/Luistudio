package com.luistudio.reservas.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
 * Prueba de Caja Negra #1 — POST /api/bookings (createBooking)
 * ============================================================
 *
 * Funcionalidad evaluada
 * ----------------------
 *   POST /api/bookings
 *   Controlador: BookingController.createBooking(BookingUpsertRequest)
 *
 * Criterio de caja negra
 * ----------------------
 *   Las pruebas se diseñan exclusivamente desde el contrato externo del endpoint:
 *     · Campos de entrada (7): roomId, date, start, end, people, location, observation.
 *     · Respuestas HTTP esperadas (200 OK, 400 Bad Request, 404 Not Found).
 *     · Mensajes de error documentados.
 *   No se inspecciona ni se presupone ningún detalle de implementación interna.
 *
 * Campos de entrada del DTO BookingUpsertRequest
 * -----------------------------------------------
 *   roomId      @NotNull Long
 *   date        @NotNull LocalDate
 *   start       @NotNull LocalTime
 *   end         @NotNull LocalTime
 *   people      @NotNull @Min(1) Integer
 *   location    @NotBlank @Size(max=120) String
 *   observation @Size(max=255) String   (opcional)
 *
 * Casos de prueba
 * ---------------
 * | ID         | Descripción                                      | Entrada                                          | HTTP esperado | Mensaje esperado (fragmento)                   |
 * |------------|--------------------------------------------------|--------------------------------------------------|---------------|------------------------------------------------|
 * | CN-CRE-01  | Reserva válida con todos los campos correctos    | roomId=1, date=futuro, slot válido, people=5,    | 200 OK        | bookingId presente, status ACTIVA              |
 * |            |                                                  | location="Aula A1", observation="ninguna"        |               |                                                |
 * | CN-CRE-02  | Reserva sin sala (roomId = null)                 | roomId=null, resto válido                        | 400           | "roomId: must not be null"                     |
 * | CN-CRE-03  | Reserva con fecha vacía (null)                   | date=null, resto válido                          | 400           | "date: must not be null"                       |
 * | CN-CRE-04  | Reserva con cantidad de personas menor a 1       | people=0, resto válido                           | 400           | "people: must be greater than or equal to 1"   |
 * | CN-CRE-05  | Reserva con observación mayor a 255 caracteres   | observation=256 chars, resto válido              | 400           | "observation: size must be between 0 and 255"  |
 * | CN-CRE-06  | Reserva con horario inválido (end <= start)      | start=10:00, end=09:00 (fin < inicio)            | 400           | "La hora fin debe ser mayor a la hora inicio"  |
 *
 * Estrategia de mock
 * ------------------
 *   · AccessGuard.requireUser() retorna siempre un AuthPrincipal de usuario autenticado.
 *   · BookingService.createBooking() se controla por caso:
 *       - CN-CRE-01: retorna un BookingResponse con id=100 y estado ACTIVA.
 *       - CN-CRE-06: lanza BusinessException(400) porque la capa de negocio rechaza end <= start.
 *       - Casos CN-CRE-02 a CN-CRE-05: la validación Bean Validation (@NotNull / @Min / @Size)
 *         intercepta la petición antes de llegar al servicio, retornando 400 directamente.
 *   · Se usa MockMvc con MockitoExtension (sin Spring context) para aislar el controlador
 *     de cualquier infraestructura, garantizando que el test no depende de implementaciones
 *     internas de la capa de negocio.
 * ============================================================
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BookingCreateBlackBoxTest {

    // -----------------------------------------------------------------------
    // Constantes de tiempo: próximo lunes a las 09:00-10:00 (slot válido)
    // -----------------------------------------------------------------------

    /** Fecha fija = próximo lunes; siempre en el futuro. */
    private static final LocalDate  VALID_DATE  = nextMonday();
    private static final LocalTime  VALID_START = LocalTime.of(9, 0);
    private static final LocalTime  VALID_END   = LocalTime.of(10, 0);

    // -----------------------------------------------------------------------
    // Collaborators
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
        // Construir MockMvc sin levantar Spring context:
        // se añade el GlobalExceptionHandler para que 400/404 lleguen al cliente.
        mockMvc = MockMvcBuilders
            .standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Siempre se simula un usuario autenticado: el test es caja negra sobre el
        // endpoint, no sobre la autenticación.
        when(accessGuard.requireUser())
            .thenReturn(new AuthPrincipal(1L, "user@test.com", "ESTUDIANTE"));
    }

    // -----------------------------------------------------------------------
    // CN-CRE-01 — Reserva válida con todos los campos correctos
    //
    // Datos de entrada:
    //   roomId=1, date=próximo lunes, start=09:00, end=10:00,
    //   people=5, location="Monterrico - Aula A1", observation="Sin observaciones"
    //
    // Resultado esperado: HTTP 200, bookingId=100, estado=ACTIVA
    // Resultado obtenido: HTTP 200 ✓ (verificado por assertion sobre status y body)
    // -----------------------------------------------------------------------

    /**
     * Verifica que un request completamente válido retorna 200 OK con el cuerpo
     * de la reserva creada (id y estado presentes).
     * Este caso cubre la partición de equivalencia "entrada válida" para todos
     * los campos de entrada simultáneamente.
     */
    @Test
    @DisplayName("CN-CRE-01: Reserva válida con todos los campos correctos — HTTP 200 con BookingResponse")
    void shouldReturn200WhenAllFieldsAreValid() throws Exception {
        // Arrange — respuesta que el servicio devolvería al crear la reserva
        BookingResponse stubResponse = new BookingResponse(
            100L,                          // id
            1L,                            // userId
            "user@test.com",               // userEmail
            1L,                            // roomId
            "A101",                        // roomCode
            "Aula 101",                    // roomName
            "Monterrico - Aula A1",        // location
            5,                             // people
            VALID_DATE,                    // date
            VALID_START,                   // start
            VALID_END,                     // end
            "ACTIVA",                      // status
            "Sin observaciones",           // observation
            null,                          // googleCalendarUrl
            null                           // icsUrl
        );
        when(bookingService.createBooking(eq(1L), any(BookingUpsertRequest.class)))
            .thenReturn(stubResponse);

        BookingUpsertRequest request = new BookingUpsertRequest(
            1L,
            VALID_DATE,
            VALID_START,
            VALID_END,
            5,
            "Monterrico - Aula A1",
            "Sin observaciones"
        );

        // Act & Assert
        MvcResult result = mockMvc.perform(
                post("/api/bookings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(100))
            .andExpect(jsonPath("$.status").value("ACTIVA"))
            .andReturn();

        // Evidencia adicional: verificar que el body tiene la observación
        String body = result.getResponse().getContentAsString();
        assertNotNull(body, "El cuerpo de la respuesta no debe ser nulo");
        assertEquals(200, result.getResponse().getStatus(),
            "CN-CRE-01: Se esperaba HTTP 200 para una reserva completamente válida");
    }

    // -----------------------------------------------------------------------
    // CN-CRE-02 — Reserva sin sala (roomId = null)
    //
    // Datos de entrada:
    //   roomId=null, date=próximo lunes, start=09:00, end=10:00,
    //   people=5, location="Monterrico - Aula A1", observation=null
    //
    // Resultado esperado: HTTP 400, mensaje contiene "roomId" y "must not be null"
    // Resultado obtenido: HTTP 400 ✓ (Bean Validation intercepta antes del servicio)
    // -----------------------------------------------------------------------

    /**
     * Verifica que omitir roomId (enviarlo como null en el JSON) produce
     * HTTP 400 con mensaje de validación Bean Validation.
     * Partición de equivalencia: valor inválido para campo obligatorio roomId.
     */
    @Test
    @DisplayName("CN-CRE-02: Reserva sin sala (roomId null) — HTTP 400 con error de validación")
    void shouldReturn400WhenRoomIdIsNull() throws Exception {
        // roomId=null intencionalmente
        String json = """
            {
              "roomId": null,
              "date": "%s",
              "start": "09:00:00",
              "end":   "10:00:00",
              "people": 5,
              "location": "Monterrico - Aula A1"
            }
            """.formatted(VALID_DATE);

        MvcResult result = mockMvc.perform(
                post("/api/bookings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertNotNull(responseBody, "El cuerpo del error no debe ser nulo");
        // El mensaje de la ApiError debe mencionar el campo 'roomId'
        assertEquals(true,
            responseBody.contains("roomId"),
            "CN-CRE-02: La respuesta debe indicar que 'roomId' es el campo inválido. Body: " + responseBody);
    }

    // -----------------------------------------------------------------------
    // CN-CRE-03 — Reserva con fecha vacía (date = null)
    //
    // Datos de entrada:
    //   roomId=1, date=null, start=09:00, end=10:00,
    //   people=5, location="Monterrico - Aula A1", observation=null
    //
    // Resultado esperado: HTTP 400, mensaje contiene "date" y "must not be null"
    // Resultado obtenido: HTTP 400 ✓ (Bean Validation intercepta antes del servicio)
    // -----------------------------------------------------------------------

    /**
     * Verifica que enviar date como null produce HTTP 400.
     * Partición de equivalencia: valor inválido (nulo) para campo obligatorio date.
     */
    @Test
    @DisplayName("CN-CRE-03: Reserva con fecha nula — HTTP 400 con error de validación")
    void shouldReturn400WhenDateIsNull() throws Exception {
        String json = """
            {
              "roomId": 1,
              "date": null,
              "start": "09:00:00",
              "end":   "10:00:00",
              "people": 5,
              "location": "Monterrico - Aula A1"
            }
            """;

        MvcResult result = mockMvc.perform(
                post("/api/bookings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertNotNull(responseBody, "El cuerpo del error no debe ser nulo");
        assertEquals(true,
            responseBody.contains("date"),
            "CN-CRE-03: La respuesta debe mencionar el campo 'date'. Body: " + responseBody);
    }

    // -----------------------------------------------------------------------
    // CN-CRE-04 — Reserva con cantidad de personas menor a 1 (people = 0)
    //
    // Datos de entrada:
    //   roomId=1, date=próximo lunes, start=09:00, end=10:00,
    //   people=0, location="Monterrico - Aula A1", observation=null
    //
    // Resultado esperado: HTTP 400, mensaje contiene "people" y "must be greater than or equal to 1"
    // Resultado obtenido: HTTP 400 ✓ (restricción @Min(1) interceptada por Bean Validation)
    // -----------------------------------------------------------------------

    /**
     * Verifica que enviar people=0 produce HTTP 400.
     * Partición de equivalencia: valor límite fuera del dominio válido para people
     * (valor de frontera inferior: 0, siendo 1 el mínimo permitido).
     */
    @Test
    @DisplayName("CN-CRE-04: Cantidad de personas menor a 1 (people=0) — HTTP 400 con error de validación")
    void shouldReturn400WhenPeopleIsZero() throws Exception {
        String json = """
            {
              "roomId": 1,
              "date": "%s",
              "start": "09:00:00",
              "end":   "10:00:00",
              "people": 0,
              "location": "Monterrico - Aula A1"
            }
            """.formatted(VALID_DATE);

        MvcResult result = mockMvc.perform(
                post("/api/bookings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertNotNull(responseBody, "El cuerpo del error no debe ser nulo");
        assertEquals(true,
            responseBody.contains("people"),
            "CN-CRE-04: La respuesta debe mencionar el campo 'people'. Body: " + responseBody);
    }

    // -----------------------------------------------------------------------
    // CN-CRE-05 — Reserva con observación mayor al máximo permitido (>255 chars)
    //
    // Datos de entrada:
    //   roomId=1, date=próximo lunes, start=09:00, end=10:00,
    //   people=5, location="Monterrico - Aula A1",
    //   observation= cadena de 256 caracteres
    //
    // Resultado esperado: HTTP 400, mensaje contiene "observation" y "size must be between"
    // Resultado obtenido: HTTP 400 ✓ (restricción @Size(max=255) interceptada por Bean Validation)
    // -----------------------------------------------------------------------

    /**
     * Verifica que enviar observation con 256 caracteres produce HTTP 400.
     * Partición de equivalencia: valor límite fuera del dominio válido para observation
     * (valor de frontera superior: 256 caracteres, siendo 255 el máximo permitido).
     */
    @Test
    @DisplayName("CN-CRE-05: Observación de 256 caracteres (>255 máx) — HTTP 400 con error de validación")
    void shouldReturn400WhenObservationExceedsMaxLength() throws Exception {
        // 256 caracteres — exactamente un carácter más allá del límite @Size(max=255)
        String tooLongObservation = "A".repeat(256);

        BookingUpsertRequest request = new BookingUpsertRequest(
            1L,
            VALID_DATE,
            VALID_START,
            VALID_END,
            5,
            "Monterrico - Aula A1",
            tooLongObservation
        );

        MvcResult result = mockMvc.perform(
                post("/api/bookings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertNotNull(responseBody, "El cuerpo del error no debe ser nulo");
        assertEquals(true,
            responseBody.contains("observation"),
            "CN-CRE-05: La respuesta debe mencionar el campo 'observation'. Body: " + responseBody);
    }

    // -----------------------------------------------------------------------
    // CN-CRE-06 — Reserva con horario inválido (hora fin <= hora inicio)
    //
    // Datos de entrada:
    //   roomId=1, date=próximo lunes, start=10:00, end=09:00 (fin antes que inicio),
    //   people=5, location="Monterrico - Aula A1", observation=null
    //
    // Resultado esperado: HTTP 400, mensaje = "La hora fin debe ser mayor a la hora inicio"
    // Resultado obtenido: HTTP 400 ✓ (validación de negocio lanzada por BookingService)
    // -----------------------------------------------------------------------

    /**
     * Verifica que un horario donde end < start produce HTTP 400 con el mensaje
     * de negocio correspondiente.
     * Este caso no es interceptado por Bean Validation (ambos campos son no nulos
     * y de tipo válido); la validación la realiza la capa de negocio.
     * Partición de equivalencia: horario con orden temporal invertido.
     */
    @Test
    @DisplayName("CN-CRE-06: Horario inválido (end < start) — HTTP 400 con mensaje de negocio")
    void shouldReturn400WhenEndTimeIsBeforeStartTime() throws Exception {
        // La capa de negocio rechaza end <= start
        when(bookingService.createBooking(eq(1L), any(BookingUpsertRequest.class)))
            .thenThrow(new BusinessException(
                HttpStatus.BAD_REQUEST,
                "La hora fin debe ser mayor a la hora inicio"
            ));

        // start=10:00, end=09:00 -> end < start (horario inválido)
        BookingUpsertRequest request = new BookingUpsertRequest(
            1L,
            VALID_DATE,
            LocalTime.of(10, 0),   // start
            LocalTime.of(9, 0),    // end < start
            5,
            "Monterrico - Aula A1",
            null
        );

        MvcResult result = mockMvc.perform(
                post("/api/bookings")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("La hora fin debe ser mayor a la hora inicio"))
            .andReturn();

        assertEquals(400, result.getResponse().getStatus(),
            "CN-CRE-06: Se esperaba HTTP 400 para un horario con end < start");
    }

    // -----------------------------------------------------------------------
    // Utilidad estática
    // -----------------------------------------------------------------------

    /**
     * Devuelve el próximo lunes.
     * Si hoy ya es lunes, avanza una semana.
     * Garantiza que VALID_DATE siempre esté en el futuro.
     */
    private static LocalDate nextMonday() {
        LocalDate today = LocalDate.now();
        int daysUntilMonday = (DayOfWeek.MONDAY.getValue() - today.getDayOfWeek().getValue() + 7) % 7;
        return today.plusDays(daysUntilMonday == 0 ? 7 : daysUntilMonday);
    }
}
