package com.luistudio.reservas.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.luistudio.reservas.dto.room.RoomResponse;
import com.luistudio.reservas.dto.room.RoomScheduleInput;
import com.luistudio.reservas.dto.room.RoomScheduleResponse;
import com.luistudio.reservas.dto.room.RoomUpsertRequest;
import com.luistudio.reservas.exception.BusinessException;
import com.luistudio.reservas.exception.GlobalExceptionHandler;
import com.luistudio.reservas.exception.NotFoundException;
import com.luistudio.reservas.model.RoomState;
import com.luistudio.reservas.security.AuthPrincipal;
import com.luistudio.reservas.service.AccessGuard;
import com.luistudio.reservas.service.AvailabilitySubscriptionService;
import com.luistudio.reservas.service.BookingService;
import com.luistudio.reservas.service.RoomService;
import java.time.LocalTime;
import java.util.List;
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
 * Prueba de Caja Negra #3 — Registro y edición de salas
 *   POST /api/rooms          (createRoom — admin)
 *   PUT  /api/rooms/{roomId} (updateRoom — admin)
 * ============================================================
 *
 * Funcionalidad evaluada
 * ----------------------
 *   Endpoints administrativos de gestión de salas en RoomController.
 *   Ambos métodos aceptan el mismo DTO: RoomUpsertRequest (10 campos de entrada).
 *
 * Criterio de caja negra
 * ----------------------
 *   Las pruebas se diseñan exclusivamente desde el contrato externo del endpoint:
 *     · Campos de entrada (10): name, campus, location, capacity, maxPeople,
 *       minPeople, minPeopleRequired, schedule, pabellonCode, status.
 *     · Respuestas HTTP esperadas: 200 OK, 400 Bad Request, 404 Not Found.
 *     · Mensajes de error documentados en el contrato de la API.
 *   No se accede ni se presupone ningún detalle de implementación interna.
 *
 * Campos de entrada de RoomUpsertRequest
 * ----------------------------------------
 *   name             @NotBlank @Size(max=120) String
 *   campus           @NotBlank @Size(max=120) String
 *   location         @NotBlank @Size(max=120) String
 *   capacity         @NotNull  @Min(1) Integer
 *   maxPeople        @NotNull  @Min(1) Integer
 *   minPeople        @Min(1) Integer              (opcional)
 *   minPeopleRequired @NotNull Boolean
 *   schedule         List<@Valid RoomScheduleInput> (opcional)
 *   pabellonCode     @Size(max=20) String          (opcional)
 *   status           RoomState                     (opcional)
 *
 * Casos de prueba
 * ---------------
 * | ID         | Descripción                                            | Endpoint         | HTTP esperado | Resultado esperado                                              |
 * |------------|--------------------------------------------------------|------------------|---------------|-----------------------------------------------------------------|
 * | CN-ROOM-01 | Registrar sala válida con todos los campos correctos    | POST /api/rooms  | 200 OK        | RoomResponse con id=50, name="Sala de Cómputo A", DISPONIBLE   |
 * | CN-ROOM-02 | Registrar sala sin nombre (name en blanco)              | POST /api/rooms  | 400           | Body contiene "name" (validación @NotBlank)                    |
 * | CN-ROOM-03 | Registrar sala con capacity < 1 (capacity=0)           | POST /api/rooms  | 400           | Body contiene "capacity" (validación @Min(1))                  |
 * | CN-ROOM-04 | Registrar sala con maxPeople inválido (maxPeople=0)    | POST /api/rooms  | 400           | Body contiene "maxPeople" (validación @Min(1))                 |
 * | CN-ROOM-05 | Registrar sala con pabellonCode demasiado largo (>20)  | POST /api/rooms  | 400           | Body contiene "pabellonCode" (@Size(max=20))                   |
 * | CN-ROOM-06 | Editar sala existente con datos válidos                | PUT  /api/rooms/5| 200 OK        | RoomResponse con id=5, nombre actualizado, DISPONIBLE          |
 *
 * Estrategia de mock
 * ------------------
 *   · AccessGuard.requireAdmin() retorna siempre un AuthPrincipal administrador.
 *     LENIENT para que los tests interceptados por Bean Validation no fallen
 *     por stub no utilizado.
 *   · RoomService.createRoom() y RoomService.updateRoom() se controlan por caso:
 *       - CN-ROOM-01: createRoom retorna RoomResponse con id=50 y status=DISPONIBLE.
 *       - CN-ROOM-02 a CN-ROOM-05: Bean Validation intercepta antes del servicio.
 *       - CN-ROOM-06: updateRoom retorna RoomResponse con id=5 y nombre actualizado.
 *   · MockMvc standalone + GlobalExceptionHandler garantizan el contrato HTTP
 *     sin levantar el contexto completo de Spring.
 * ============================================================
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RoomAdminBlackBoxTest {

    // -----------------------------------------------------------------------
    // ID de sala existente usada en la edición
    // -----------------------------------------------------------------------

    private static final long EXISTING_ROOM_ID = 5L;

    // -----------------------------------------------------------------------
    // Colaboradores
    // -----------------------------------------------------------------------

    @Mock private RoomService                  roomService;
    @Mock private BookingService               bookingService;
    @Mock private AccessGuard                  accessGuard;
    @Mock private AvailabilitySubscriptionService subscriptionService;

    @InjectMocks
    private RoomController controller;

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

        // Siempre se simula un administrador autenticado.
        when(accessGuard.requireAdmin())
            .thenReturn(new AuthPrincipal(1L, "admin@test.com", "ADMIN"));
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Construye un RoomUpsertRequest completamente válido.
     *
     * @param name        nombre de la sala
     * @param capacity    capacidad física de la sala
     * @param maxPeople   máximo de personas configurado
     * @param pabellonCode código de pabellón (puede ser null)
     */
    private RoomUpsertRequest validRequest(
            String name, int capacity, int maxPeople, String pabellonCode) {
        return new RoomUpsertRequest(
            name,
            "Monterrico",
            "Pabellón A - Piso 2",
            capacity,
            maxPeople,
            1,
            false,
            List.of(new RoomScheduleInput(1, LocalTime.of(7, 0), LocalTime.of(22, 0), false)),
            pabellonCode,
            RoomState.DISPONIBLE,
            null,
            null,
            null,
            null
        );
    }

    /**
     * Construye un RoomResponse de éxito para el stub del servicio.
     *
     * @param id   id de la sala retornada
     * @param name nombre de la sala
     */
    private RoomResponse successResponse(long id, String name) {
        return new RoomResponse(
            id,                         // id
            "MNT-A2-0001",              // code
            name,                       // name
            "Sala - " + name,           // resourceLabel
            "Monterrico",               // campus
            "Campus Monterrico",        // campusLabel
            "Pabellón A - Piso 2",      // venue
            "Pabellón A",               // venueLabel
            30,                         // capacity
            "Pabellón A - Piso 2",      // location
            1,                          // minPeople
            false,                      // minPeopleRequired
            30,                         // maxPeople
            60,                         // slotMinutes
            List.of(new RoomScheduleResponse(1, LocalTime.of(7, 0), LocalTime.of(22, 0), false, false)),
            "DISPONIBLE",               // status
            "PA",                       // pabellonCode
            null,                       // noiseLevel
            null,                       // supportsConcentration
            null,                       // roomType
            null                        // equipment
        );
    }

    // -----------------------------------------------------------------------
    // CN-ROOM-01 — Registrar sala válida con todos los campos correctos
    //
    // Datos de entrada:
    //   POST /api/rooms
    //   name="Sala de Cómputo A", campus="Monterrico", location="Pabellón A - Piso 2",
    //   capacity=30, maxPeople=30, minPeople=1, minPeopleRequired=false,
    //   schedule=[{dayOfWeek=1, openTime=07:00, closeTime=22:00, closed=false}],
    //   pabellonCode="PA", status=DISPONIBLE
    //
    // Resultado esperado: HTTP 200, body con id=50, name="Sala de Cómputo A", status=DISPONIBLE
    // Resultado obtenido: HTTP 200 ✓
    // -----------------------------------------------------------------------

    /**
     * Verifica que registrar una sala con todos los campos en rango válido
     * retorna HTTP 200 con el RoomResponse de la sala creada.
     * Partición de equivalencia: todas las entradas dentro del dominio válido.
     */
    @Test
    @DisplayName("CN-ROOM-01: Registrar sala válida con todos los campos correctos — HTTP 200 con RoomResponse")
    void shouldReturn200WhenRoomCreationIsValid() throws Exception {
        // Arrange
        when(roomService.createRoom(any(RoomUpsertRequest.class)))
            .thenReturn(new RoomResponse(
                50L,
                "MNT-A2-0001",
                "Sala de Cómputo A",
                "Sala - Sala de Cómputo A",
                "Monterrico",
                "Campus Monterrico",
                "Pabellón A - Piso 2",
                "Pabellón A",
                30,
                "Pabellón A - Piso 2",
                1,
                false,
                30,
                60,
                List.of(new RoomScheduleResponse(1, LocalTime.of(7, 0), LocalTime.of(22, 0), false, false)),
                "DISPONIBLE",
                "PA",
                null,
                null,
                null,
                null
            ));

        RoomUpsertRequest request = validRequest("Sala de Cómputo A", 30, 30, "PA");

        // Act & Assert
        MvcResult result = mockMvc.perform(
                post("/api/rooms")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(50))
            .andExpect(jsonPath("$.name").value("Sala de Cómputo A"))
            .andExpect(jsonPath("$.status").value("DISPONIBLE"))
            .andReturn();

        assertEquals(200, result.getResponse().getStatus(),
            "CN-ROOM-01: Se esperaba HTTP 200 al registrar una sala completamente válida");
        assertNotNull(result.getResponse().getContentAsString(),
            "El cuerpo de la respuesta no debe ser nulo");
    }

    // -----------------------------------------------------------------------
    // CN-ROOM-02 — Registrar sala sin nombre (name en blanco)
    //
    // Datos de entrada:
    //   POST /api/rooms
    //   name="" (cadena vacía), resto de campos válidos
    //
    // Resultado esperado: HTTP 400, body contiene "name" (@NotBlank)
    // Resultado obtenido: HTTP 400 ✓ (Bean Validation intercepta antes del servicio)
    // -----------------------------------------------------------------------

    /**
     * Verifica que enviar name como cadena vacía produce HTTP 400.
     * Partición de equivalencia: valor inválido (vacío) para campo obligatorio name.
     * Valor de frontera: cadena de longitud 0 con @NotBlank.
     */
    @Test
    @DisplayName("CN-ROOM-02: Registrar sala sin nombre (name vacío) — HTTP 400 con error de validación")
    void shouldReturn400WhenNameIsBlank() throws Exception {
        // name="" viola @NotBlank
        String json = """
            {
              "name": "",
              "campus": "Monterrico",
              "location": "Pabellón A - Piso 2",
              "capacity": 30,
              "maxPeople": 30,
              "minPeople": 1,
              "minPeopleRequired": false,
              "schedule": [],
              "pabellonCode": "PA",
              "status": "DISPONIBLE"
            }
            """;

        MvcResult result = mockMvc.perform(
                post("/api/rooms")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertNotNull(responseBody, "El cuerpo del error no debe ser nulo");
        assertEquals(true,
            responseBody.contains("name"),
            "CN-ROOM-02: La respuesta debe indicar que 'name' es el campo inválido. Body: " + responseBody);
    }

    // -----------------------------------------------------------------------
    // CN-ROOM-03 — Registrar sala con capacity < 1 (capacity = 0)
    //
    // Datos de entrada:
    //   POST /api/rooms
    //   capacity=0, resto de campos válidos
    //
    // Resultado esperado: HTTP 400, body contiene "capacity" (@Min(1))
    // Resultado obtenido: HTTP 400 ✓ (Bean Validation intercepta antes del servicio)
    // -----------------------------------------------------------------------

    /**
     * Verifica que enviar capacity=0 produce HTTP 400.
     * Valor de frontera inferior: 0, siendo 1 el mínimo permitido por @Min(1).
     * Partición de equivalencia: valor fuera del dominio válido para capacity.
     */
    @Test
    @DisplayName("CN-ROOM-03: Registrar sala con capacity=0 (< 1) — HTTP 400 con error de validación")
    void shouldReturn400WhenCapacityIsZero() throws Exception {
        String json = """
            {
              "name": "Sala de Cómputo A",
              "campus": "Monterrico",
              "location": "Pabellón A - Piso 2",
              "capacity": 0,
              "maxPeople": 30,
              "minPeople": 1,
              "minPeopleRequired": false,
              "schedule": [],
              "pabellonCode": "PA",
              "status": "DISPONIBLE"
            }
            """;

        MvcResult result = mockMvc.perform(
                post("/api/rooms")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertNotNull(responseBody, "El cuerpo del error no debe ser nulo");
        assertEquals(true,
            responseBody.contains("capacity"),
            "CN-ROOM-03: La respuesta debe indicar que 'capacity' es el campo inválido. Body: " + responseBody);
    }

    // -----------------------------------------------------------------------
    // CN-ROOM-04 — Registrar sala con maxPeople inválido (maxPeople = 0)
    //
    // Datos de entrada:
    //   POST /api/rooms
    //   maxPeople=0, resto de campos válidos
    //
    // Resultado esperado: HTTP 400, body contiene "maxPeople" (@Min(1))
    // Resultado obtenido: HTTP 400 ✓ (Bean Validation intercepta antes del servicio)
    // -----------------------------------------------------------------------

    /**
     * Verifica que enviar maxPeople=0 produce HTTP 400.
     * Valor de frontera inferior: 0, siendo 1 el mínimo permitido por @Min(1).
     * Partición de equivalencia: valor fuera del dominio válido para maxPeople.
     */
    @Test
    @DisplayName("CN-ROOM-04: Registrar sala con maxPeople=0 (< 1) — HTTP 400 con error de validación")
    void shouldReturn400WhenMaxPeopleIsZero() throws Exception {
        String json = """
            {
              "name": "Sala de Cómputo A",
              "campus": "Monterrico",
              "location": "Pabellón A - Piso 2",
              "capacity": 30,
              "maxPeople": 0,
              "minPeople": 1,
              "minPeopleRequired": false,
              "schedule": [],
              "pabellonCode": "PA",
              "status": "DISPONIBLE"
            }
            """;

        MvcResult result = mockMvc.perform(
                post("/api/rooms")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json)
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertNotNull(responseBody, "El cuerpo del error no debe ser nulo");
        assertEquals(true,
            responseBody.contains("maxPeople"),
            "CN-ROOM-04: La respuesta debe indicar que 'maxPeople' es el campo inválido. Body: " + responseBody);
    }

    // -----------------------------------------------------------------------
    // CN-ROOM-05 — Registrar sala con código de pabellón demasiado largo (>20 chars)
    //
    // Datos de entrada:
    //   POST /api/rooms
    //   pabellonCode="ABCDEFGHIJKLMNOPQRSTU" (21 caracteres), resto válido
    //
    // Resultado esperado: HTTP 400, body contiene "pabellonCode" (@Size(max=20))
    // Resultado obtenido: HTTP 400 ✓ (Bean Validation intercepta antes del servicio)
    // -----------------------------------------------------------------------

    /**
     * Verifica que enviar pabellonCode con 21 caracteres produce HTTP 400.
     * Valor de frontera superior: 21 chars, siendo 20 el máximo permitido.
     * Partición de equivalencia: valor fuera del dominio válido para pabellonCode.
     */
    @Test
    @DisplayName("CN-ROOM-05: Registrar sala con pabellonCode de 21 chars (>20 máx) — HTTP 400 con error de validación")
    void shouldReturn400WhenPabellonCodeExceedsMaxLength() throws Exception {
        // 21 caracteres — exactamente uno más allá de @Size(max=20)
        String tooLongCode = "A".repeat(21);

        RoomUpsertRequest request = new RoomUpsertRequest(
            "Sala de Cómputo A",
            "Monterrico",
            "Pabellón A - Piso 2",
            30,
            30,
            1,
            false,
            List.of(),
            tooLongCode,
            RoomState.DISPONIBLE,
            null,
            null,
            null,
            null
        );

        MvcResult result = mockMvc.perform(
                post("/api/rooms")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        assertNotNull(responseBody, "El cuerpo del error no debe ser nulo");
        assertEquals(true,
            responseBody.contains("pabellonCode"),
            "CN-ROOM-05: La respuesta debe indicar que 'pabellonCode' es el campo inválido. Body: " + responseBody);
    }

    // -----------------------------------------------------------------------
    // CN-ROOM-06 — Editar sala existente con datos válidos
    //
    // Datos de entrada:
    //   PUT /api/rooms/5
    //   name="Laboratorio B Actualizado", campus="Monterrico",
    //   location="Pabellón B - Piso 1", capacity=25, maxPeople=25,
    //   minPeople=1, minPeopleRequired=false,
    //   schedule=[{dayOfWeek=1, openTime=08:00, closeTime=21:00, closed=false}],
    //   pabellonCode="PB", status=DISPONIBLE
    //
    // Resultado esperado: HTTP 200, body con id=5, name="Laboratorio B Actualizado", DISPONIBLE
    // Resultado obtenido: HTTP 200 ✓
    // -----------------------------------------------------------------------

    /**
     * Verifica que editar una sala existente con todos los campos en rango válido
     * retorna HTTP 200 con el RoomResponse actualizado.
     * Partición de equivalencia: todas las entradas dentro del dominio válido
     * sobre un recurso existente (roomId=5).
     */
    @Test
    @DisplayName("CN-ROOM-06: Editar sala existente con datos válidos — HTTP 200 con RoomResponse actualizado")
    void shouldReturn200WhenRoomUpdateIsValid() throws Exception {
        // Arrange
        RoomResponse updatedRoom = new RoomResponse(
            EXISTING_ROOM_ID,
            "MNT-PB1-0002",
            "Laboratorio B Actualizado",
            "Sala - Laboratorio B Actualizado",
            "Monterrico",
            "Campus Monterrico",
            "Pabellón B - Piso 1",
            "Pabellón B",
            25,
            "Pabellón B - Piso 1",
            1,
            false,
            25,
            60,
            List.of(new RoomScheduleResponse(1, LocalTime.of(8, 0), LocalTime.of(21, 0), false, false)),
            "DISPONIBLE",
            "PB",
            null,
            null,
            null,
            null
        );
        when(roomService.updateRoom(eq(EXISTING_ROOM_ID), any(RoomUpsertRequest.class)))
            .thenReturn(updatedRoom);

        RoomUpsertRequest request = new RoomUpsertRequest(
            "Laboratorio B Actualizado",
            "Monterrico",
            "Pabellón B - Piso 1",
            25,
            25,
            1,
            false,
            List.of(new RoomScheduleInput(1, LocalTime.of(8, 0), LocalTime.of(21, 0), false)),
            "PB",
            RoomState.DISPONIBLE,
            null,
            null,
            null,
            null
        );

        // Act & Assert
        MvcResult result = mockMvc.perform(
                put("/api/rooms/{id}", EXISTING_ROOM_ID)
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(EXISTING_ROOM_ID))
            .andExpect(jsonPath("$.name").value("Laboratorio B Actualizado"))
            .andExpect(jsonPath("$.status").value("DISPONIBLE"))
            .andReturn();

        assertEquals(200, result.getResponse().getStatus(),
            "CN-ROOM-06: Se esperaba HTTP 200 al editar una sala existente con datos válidos");
        assertNotNull(result.getResponse().getContentAsString(),
            "El cuerpo de la respuesta no debe ser nulo");
    }

    @Test
    @DisplayName("Eliminar sala existente devuelve HTTP 204 sin cuerpo")
    void shouldReturn204WhenRoomIsDeleted() throws Exception {
        mockMvc.perform(delete("/api/rooms/{id}", EXISTING_ROOM_ID))
            .andExpect(status().isNoContent());

        verify(roomService).deleteRoom(EXISTING_ROOM_ID);
    }
}
