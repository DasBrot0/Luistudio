package com.luistudio.reservas.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.luistudio.reservas.dto.room.IntelligentRoomSearchRequest;
import com.luistudio.reservas.dto.room.IntelligentRoomSearchResponse;
import com.luistudio.reservas.dto.room.RoomSearchIntent;
import com.luistudio.reservas.exception.GlobalExceptionHandler;
import com.luistudio.reservas.model.RoomNoiseLevel;
import com.luistudio.reservas.model.RoomType;
import com.luistudio.reservas.service.AccessGuard;
import com.luistudio.reservas.service.AvailabilitySubscriptionService;
import com.luistudio.reservas.service.BookingService;
import com.luistudio.reservas.service.IntelligentRoomSearchService;
import com.luistudio.reservas.service.RoomService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * Caja Negra #40 — Búsqueda inteligente de salas.
 *
 * Funcionalidad evaluada: POST /api/rooms/intelligent-search.
 * Campos externos: query, date, start, end y limit.
 *
 * Casos y resultados esperados:
 * - CN-SEARCH-01: cinco campos válidos -> HTTP 200.
 * - CN-SEARCH-02: end anterior a start -> HTTP 400.
 * - CN-SEARCH-03: query vacía -> HTTP 400.
 * - CN-SEARCH-04: date nula -> HTTP 400.
 * - CN-SEARCH-05: limit menor a 1 -> HTTP 400.
 * - CN-SEARCH-06: limit mayor a 3 -> HTTP 400.
 * - CN-SEARCH-07: minutos arbitrarios o duración no reservable -> HTTP 400.
 *
 * La prueba usa únicamente el contrato HTTP y mantiene mockeado el servicio de
 * búsqueda, por lo que no depende de su implementación interna ni de servicios IA.
 */
@ExtendWith(MockitoExtension.class)
class IntelligentRoomSearchBlackBoxTest {

    @Mock private RoomService roomService;
    @Mock private BookingService bookingService;
    @Mock private AccessGuard accessGuard;
    @Mock private AvailabilitySubscriptionService subscriptionService;
    @Mock private IntelligentRoomSearchService searchService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        RoomController controller = new RoomController(
            roomService, bookingService, accessGuard, subscriptionService, searchService
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    @Test
    @DisplayName("CN-SEARCH-01: request válido de cinco campos — HTTP 200")
    void returns200ForValidFiveFieldRequest() throws Exception {
        IntelligentRoomSearchResponse response = new IntelligentRoomSearchResponse(
            new RoomSearchIntent(RoomType.GENERAL, 4, RoomNoiseLevel.MEDIO, true, Set.of()),
            List.of()
        );
        when(searchService.search(any(IntelligentRoomSearchRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/rooms/intelligent-search")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"query":"sala silenciosa para cuatro personas","date":"2026-07-13",
                     "start":"09:00:00","end":"10:00:00","limit":3}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.intent.minimumCapacity").value(4));
    }

    @Test
    @DisplayName("CN-SEARCH-02: hora fin no posterior a inicio — HTTP 400")
    void returns400ForInvalidTimeRange() throws Exception {
        mockMvc.perform(post("/api/rooms/intelligent-search")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"query":"sala silenciosa para cuatro personas","date":"2026-07-13",
                     "start":"10:00:00","end":"09:00:00","limit":3}
                    """))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("CN-SEARCH-03: query vacía — HTTP 400")
    void returns400ForBlankQuery() throws Exception {
        assertBadRequest("""
            {"query":" ","date":"2026-07-13","start":"09:00:00","end":"10:00:00","limit":3}
            """);
    }

    @Test
    @DisplayName("CN-SEARCH-04: fecha nula — HTTP 400")
    void returns400ForNullDate() throws Exception {
        assertBadRequest("""
            {"query":"sala silenciosa","date":null,"start":"09:00:00","end":"10:00:00","limit":3}
            """);
    }

    @Test
    @DisplayName("CN-SEARCH-05: límite menor a 1 — HTTP 400")
    void returns400ForLimitBelowMinimum() throws Exception {
        assertBadRequest("""
            {"query":"sala silenciosa","date":"2026-07-13","start":"09:00:00","end":"10:00:00","limit":0}
            """);
    }

    @Test
    @DisplayName("CN-SEARCH-06: límite mayor a 3 — HTTP 400")
    void returns400ForLimitAboveMaximum() throws Exception {
        assertBadRequest("""
            {"query":"sala silenciosa","date":"2026-07-13","start":"09:00:00","end":"10:00:00","limit":4}
            """);
    }

    @Test
    @DisplayName("CN-SEARCH-07: horario con minutos arbitrarios — HTTP 400")
    void returns400ForNonReservableTimeBlock() throws Exception {
        assertBadRequest("""
            {"query":"necesito estudiar","date":"2026-07-15","start":"08:22:00","end":"21:00:00","limit":3}
            """);
    }

    private void assertBadRequest(String body) throws Exception {
        mockMvc.perform(post("/api/rooms/intelligent-search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }
}
