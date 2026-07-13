package com.luistudio.reservas.service.booking.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.luistudio.reservas.dto.booking.BookingUpsertRequest;
import com.luistudio.reservas.exception.BusinessException;
import com.luistudio.reservas.model.RoomEntity;
import com.luistudio.reservas.model.CampusEntity;
import com.luistudio.reservas.model.PabellonEntity;
import com.luistudio.reservas.model.RoomState;
import com.luistudio.reservas.model.UserEntity;
import com.luistudio.reservas.repository.ReservationRepository;
import com.luistudio.reservas.service.RoomScheduleService;
import com.luistudio.reservas.service.RoomScheduleService.EffectiveSchedule;
import com.luistudio.reservas.service.RoomService;
import com.luistudio.reservas.service.SystemConfigService;
import com.luistudio.reservas.util.AppTime;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Prueba de Caja Blanca — BookingValidationService.validate(...)
 *
 * Módulo evaluado: BookingValidationService.validate(UserEntity, RoomEntity, BookingUpsertRequest, Long)
 *
 * Complejidad Ciclomática:
 *   El método validate() orquesta 6 sub-validadores. Contando todos sus predicados
 *   internos independientes:
 *     validateEndTimeAfterStart  → 1 predicado
 *     validateCapacity           → 3 predicados (people>capacity, people>roomMax, minRequired && people<roomMin)
 *     validateBookingWindowAndSlot → 6 predicados (pastDate, afterMaxDate, sameDay&&pastSlot,
 *                                                   duration!=slot, schedule.closed, start<open||end>close)
 *     validateMaxDuration        → 1 predicado
 *     validateRoomAvailability   → 1 predicado
 *     validateMaxActiveBookings  → 2 predicados (excludeId!=null, activeCount>=max)
 *   Total de predicados binarios: 14  →  M = 14 + 1 = 15  (>> 4)
 *
 * Estrategia de mock:
 *   - AppTime se mockea con MockedStatic para fijar "now" a un lunes a las 08:00 hs.
 *   - Para cada caso se configuran los mocks de forma que SOLO la ruta bajo prueba falle,
 *     dejando todas las demás condiciones en estado "válido".
 */
/*
 * Aclaración de la unidad de análisis: se evalúa el flujo interprocedural completo.
 * validate() es el punto de entrada y delega en seis subvalidadores privados. Una
 * herramienta que mida cada método por separado reportará M=1 para el orquestador;
 * en esta prueba de módulo se contabilizan las decisiones alcanzables desde él,
 * que producen las rutas observables documentadas arriba (M=15).
 */
@ExtendWith(MockitoExtension.class)
class BookingValidationServiceTest {

    // -----------------------------------------------------------------------
    // Constantes de tiempo fijas para toda la suite
    // -----------------------------------------------------------------------

    /**
     * "Ahora" fijo: el próximo lunes a las 07:00 (inicio de semana laboral).
     * VALID_DATE es ese mismo lunes, con un bloque a las 09:00–10:00.
     * Esto garantiza que:
     *   - requestDate == today  → pasa el check de fecha pasada
     *   - requestDate <= endCurrentWeek (Sunday)  → pasa el check de semana
     *   - request.start() (09:00) > now.toLocalTime() (07:00)  → pasa el check de hora actual
     */
    private static final LocalDateTime FIXED_NOW  = nextMondayAt7();
    private static final LocalDate     VALID_DATE  = FIXED_NOW.toLocalDate();
    private static final LocalTime     SLOT_OPEN   = LocalTime.of(7, 0);
    private static final LocalTime     VALID_START = LocalTime.of(9, 0);   // 09:00 > 07:00 ✓
    private static final LocalTime     VALID_END   = LocalTime.of(10, 0);  // exactamente 60 min
    private static final int           SLOT_MINUTES = 60;

    // -----------------------------------------------------------------------
    // Mocks e instancia bajo prueba
    // -----------------------------------------------------------------------

    @Mock private RoomScheduleService    roomScheduleService;
    @Mock private RoomService            roomService;
    @Mock private ReservationRepository  reservationRepository;
    @Mock private SystemConfigService    systemConfigService;

    @InjectMocks
    private BookingValidationService sut;

    private RoomEntity room;
    private UserEntity user;

    // -----------------------------------------------------------------------
    // Setup
    // -----------------------------------------------------------------------

    @BeforeEach
    void setUp() {
        // Sala base: capacidad 20, máx 20, mín 1 (no obligatorio)
        room = new RoomEntity();
        room.setCapacidad(20);
        room.setMaximoPersonas(20);
        room.setMinimoPersonas(1);
        room.setMinimoPersonasObligatorio(false);
        room.setEstado(RoomState.DISPONIBLE);
        CampusEntity campus = new CampusEntity();
        campus.setNombre("Monterrico");
        PabellonEntity pabellon = new PabellonEntity();
        pabellon.setCampus(campus);
        room.setPabellon(pabellon);

        user = new UserEntity();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** Construye un request totalmente válido. */
    private BookingUpsertRequest validRequest() {
        return new BookingUpsertRequest(
            1L,
            VALID_DATE,
            VALID_START,
            VALID_END,
            5,
            "Monterrico - Aula A1",
            null
        );
    }

    /** Configura todos los mocks para que el request pase sin errores. */
    private void configureAllMocksToPass() {
        when(roomScheduleService.getCampusSlotMinutes(any())).thenReturn(SLOT_MINUTES);

        EffectiveSchedule openSchedule = new EffectiveSchedule(
            VALID_DATE.getDayOfWeek().getValue(),
            SLOT_OPEN,
            LocalTime.of(22, 0),
            false,
            false
        );
        when(roomScheduleService.getEffectiveScheduleForRoomDay(any(), any())).thenReturn(openSchedule);
        when(roomService.isRoomAvailable(any(), any(), any(), any(), any())).thenReturn(true);
        when(systemConfigService.getMaxActiveBookings()).thenReturn(3);
        when(reservationRepository.countCurrentActiveForUser(any(), any(), any())).thenReturn(0L);
    }

    // -----------------------------------------------------------------------
    // CASO 1 — Reserva válida: todas las rutas de validación se completan sin excepción
    // -----------------------------------------------------------------------

    /**
     * Ruta cubierta: validate() → validateEndTimeAfterStart (OK) →
     *   validateCapacity (OK) → validateBookingWindowAndSlot (OK) →
     *   validateMaxDuration (OK) → validateRoomAvailability (OK) →
     *   validateMaxActiveBookings (OK) → retorno normal.
     */
    @Test
    @DisplayName("CB-01: Reserva completamente válida pasa todas las validaciones sin excepción")
    void shouldPassAllValidationsForAValidRequest() {
        try (MockedStatic<AppTime> appTime = mockStatic(AppTime.class)) {
            appTime.when(AppTime::nowDateTime).thenReturn(FIXED_NOW);
            appTime.when(AppTime::today).thenReturn(FIXED_NOW.toLocalDate());
            appTime.when(AppTime::nowTime).thenReturn(FIXED_NOW.toLocalTime());

            configureAllMocksToPass();

            assertDoesNotThrow(() -> sut.validate(user, room, validRequest(), null),
                "Una reserva completamente válida no debe lanzar ninguna excepción");
        }
    }

    // -----------------------------------------------------------------------
    // CASO 2 — Hora fin <= hora inicio → validateEndTimeAfterStart lanza excepción
    // -----------------------------------------------------------------------

    /**
     * Ruta cubierta: validate() → validateEndTimeAfterStart →
     *   predicado !end.isAfter(start) == TRUE → lanza BusinessException.
     * El resto de validadores NO se ejecuta.
     */
    @Test
    @DisplayName("CB-02: Hora fin igual a hora inicio lanza BusinessException (validateEndTimeAfterStart)")
    void shouldFailWhenEndTimeIsNotAfterStartTime() {
        try (MockedStatic<AppTime> appTime = mockStatic(AppTime.class)) {
            appTime.when(AppTime::nowDateTime).thenReturn(FIXED_NOW);
            appTime.when(AppTime::today).thenReturn(FIXED_NOW.toLocalDate());
            appTime.when(AppTime::nowTime).thenReturn(FIXED_NOW.toLocalTime());

            // end == start → predicado !end.isAfter(start) es verdadero
            BookingUpsertRequest bad = new BookingUpsertRequest(
                1L, VALID_DATE,
                VALID_START, VALID_START,   // end == start
                5, "Monterrico - Aula A1", null
            );

            BusinessException ex = assertThrows(BusinessException.class,
                () -> sut.validate(user, room, bad, null));
            assertEquals("La hora fin debe ser mayor a la hora inicio", ex.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // CASO 3 — Cantidad de personas supera capacidad → validateCapacity lanza excepción
    // -----------------------------------------------------------------------

    /**
     * Ruta cubierta: validate() → validateEndTimeAfterStart (OK) →
     *   validateCapacity → predicado people > roomCapacity == TRUE → lanza BusinessException.
     */
    @Test
    @DisplayName("CB-03: Cantidad de personas supera capacidad lanza BusinessException (validateCapacity)")
    void shouldFailWhenPeopleExceedsRoomCapacity() {
        try (MockedStatic<AppTime> appTime = mockStatic(AppTime.class)) {
            appTime.when(AppTime::nowDateTime).thenReturn(FIXED_NOW);
            appTime.when(AppTime::today).thenReturn(FIXED_NOW.toLocalDate());
            appTime.when(AppTime::nowTime).thenReturn(FIXED_NOW.toLocalTime());

            // 21 personas, capacidad máx 20
            BookingUpsertRequest bad = new BookingUpsertRequest(
                1L, VALID_DATE, VALID_START, VALID_END,
                21,                              // people > capacidad
                "Monterrico - Aula A1", null
            );

            BusinessException ex = assertThrows(BusinessException.class,
                () -> sut.validate(user, room, bad, null));
            assertEquals("La cantidad de personas supera el máximo permitido para la sala", ex.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // CASO 4 — Sala cerrada en esa fecha → validateBookingWindowAndSlot lanza excepción
    // -----------------------------------------------------------------------

    /**
     * Ruta cubierta: validate() → validateEndTimeAfterStart (OK) → validateCapacity (OK) →
     *   validateBookingWindowAndSlot → predicado schedule.closed() == TRUE →
     *   lanza BusinessException "La sala no atiende en esa fecha".
     */
    @Test
    @DisplayName("CB-04: Sala cerrada en la fecha solicitada lanza BusinessException (validateBookingWindowAndSlot)")
    void shouldFailWhenRoomIsClosedOnRequestedDate() {
        try (MockedStatic<AppTime> appTime = mockStatic(AppTime.class)) {
            appTime.when(AppTime::nowDateTime).thenReturn(FIXED_NOW);
            appTime.when(AppTime::today).thenReturn(FIXED_NOW.toLocalDate());
            appTime.when(AppTime::nowTime).thenReturn(FIXED_NOW.toLocalTime());

            // slot = 60 min → duration == slot → pasa la comprobación de bloque
            when(roomScheduleService.getCampusSlotMinutes(any())).thenReturn(SLOT_MINUTES);

            // La sala está cerrada ese día
            EffectiveSchedule closedSchedule = new EffectiveSchedule(
                VALID_DATE.getDayOfWeek().getValue(),
                null, null,
                true,   // closed = true
                false
            );
            when(roomScheduleService.getEffectiveScheduleForRoomDay(any(), any())).thenReturn(closedSchedule);

            BusinessException ex = assertThrows(BusinessException.class,
                () -> sut.validate(user, room, validRequest(), null));
            assertEquals("La sala no atiende en esa fecha", ex.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // CASO 5 — Sala no disponible (solapamiento) → validateRoomAvailability lanza excepción
    // -----------------------------------------------------------------------

    /**
     * Ruta cubierta: validate() → validateEndTimeAfterStart (OK) → validateCapacity (OK) →
     *   validateBookingWindowAndSlot (OK) → validateMaxDuration (OK) →
     *   validateRoomAvailability → predicado !available == TRUE → lanza BusinessException.
     */
    @Test
    @DisplayName("CB-05: Sala no disponible lanza BusinessException (validateRoomAvailability)")
    void shouldFailWhenRoomIsNotAvailable() {
        try (MockedStatic<AppTime> appTime = mockStatic(AppTime.class)) {
            appTime.when(AppTime::nowDateTime).thenReturn(FIXED_NOW);
            appTime.when(AppTime::today).thenReturn(FIXED_NOW.toLocalDate());
            appTime.when(AppTime::nowTime).thenReturn(FIXED_NOW.toLocalTime());

            when(roomScheduleService.getCampusSlotMinutes(any())).thenReturn(SLOT_MINUTES);

            EffectiveSchedule openSchedule = new EffectiveSchedule(
                VALID_DATE.getDayOfWeek().getValue(),
                SLOT_OPEN, LocalTime.of(22, 0),
                false, false
            );
            when(roomScheduleService.getEffectiveScheduleForRoomDay(any(), any())).thenReturn(openSchedule);

            // La sala devuelve NO disponible
            when(roomService.isRoomAvailable(any(), any(), any(), any(), isNull())).thenReturn(false);

            BusinessException ex = assertThrows(BusinessException.class,
                () -> sut.validate(user, room, validRequest(), null));
            assertEquals("La sala no está disponible para el horario seleccionado", ex.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // CASO 6 — Usuario alcanzó límite de reservas activas → validateMaxActiveBookings lanza excepción
    // -----------------------------------------------------------------------

    /**
     * Ruta cubierta: validate() → validateEndTimeAfterStart (OK) → validateCapacity (OK) →
     *   validateBookingWindowAndSlot (OK) → validateMaxDuration (OK) →
     *   validateRoomAvailability (OK) →
     *   validateMaxActiveBookings → predicado excludeId==null (TRUE, continúa) →
     *     predicado activeCount >= maxAllowed == TRUE → lanza BusinessException.
     */
    @Test
    @DisplayName("CB-06: Límite de reservas activas alcanzado lanza BusinessException (validateMaxActiveBookings)")
    void shouldFailWhenUserReachesMaxActiveBookings() {
        try (MockedStatic<AppTime> appTime = mockStatic(AppTime.class)) {
            appTime.when(AppTime::nowDateTime).thenReturn(FIXED_NOW);
            appTime.when(AppTime::today).thenReturn(FIXED_NOW.toLocalDate());
            appTime.when(AppTime::nowTime).thenReturn(FIXED_NOW.toLocalTime());

            when(roomScheduleService.getCampusSlotMinutes(any())).thenReturn(SLOT_MINUTES);

            EffectiveSchedule openSchedule = new EffectiveSchedule(
                VALID_DATE.getDayOfWeek().getValue(),
                SLOT_OPEN, LocalTime.of(22, 0),
                false, false
            );
            when(roomScheduleService.getEffectiveScheduleForRoomDay(any(), any())).thenReturn(openSchedule);
            when(roomService.isRoomAvailable(any(), any(), any(), any(), isNull())).thenReturn(true);

            // max permitido = 1, usuario ya tiene 1 activa → activeCount >= maxAllowed
            when(systemConfigService.getMaxActiveBookings()).thenReturn(1);
            when(reservationRepository.countCurrentActiveForUser(
                eq(user), any(LocalDate.class), any(LocalTime.class))
            ).thenReturn(1L);

            BusinessException ex = assertThrows(BusinessException.class,
                () -> sut.validate(user, room, validRequest(), null));
            assertEquals("Alcanzaste el límite de reservas activas (1)", ex.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // Utilidad estática
    // -----------------------------------------------------------------------

    /**
     * Devuelve un LocalDateTime = próximo lunes a las 07:00.
     * Si hoy ya es lunes, devuelve el lunes de la próxima semana para que
     * FIXED_NOW esté siempre en un lunes a las 07:00 y los slots (09:00-10:00)
     * queden en el futuro dentro de la misma semana.
     */
    private static LocalDateTime nextMondayAt7() {
        LocalDate today = LocalDate.now();
        int daysUntilMonday = (DayOfWeek.MONDAY.getValue() - today.getDayOfWeek().getValue() + 7) % 7;
        LocalDate monday = today.plusDays(daysUntilMonday == 0 ? 7 : daysUntilMonday);
        return monday.atTime(LocalTime.of(7, 0));
    }
}
