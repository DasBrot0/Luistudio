package com.luistudio.reservas.service.booking.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
 * ============================================================
 * Prueba de Caja Blanca #2 — validateCapacity(...)
 * ============================================================
 *
 * Módulo evaluado
 * ---------------
 * BookingValidationService.validateCapacity(RoomEntity, BookingUpsertRequest)
 * — método privado accedido a través de validate().
 *
 * Código fuente evaluado
 * ----------------------
 *   private void validateCapacity(RoomEntity room, BookingUpsertRequest request) {
 *       int people      = request.people();
 *       int roomCapacity = room.getCapacidad();
 *       int roomMax     = room.getMaximoPersonas() == null ? roomCapacity : room.getMaximoPersonas(); // ← P0
 *       int roomMin     = room.getMinimoPersonas()  == null ? 1           : room.getMinimoPersonas(); // ← P_min_null
 *       boolean minRequired = Boolean.TRUE.equals(room.getMinimoPersonasObligatorio());
 *
 *       if (people > roomCapacity || people > roomMax) {   // ← P1 (OR de dos sub-predicados)
 *           throw new BusinessException(..., "La cantidad de personas supera el máximo...");
 *       }
 *       if (minRequired && people < roomMin) {             // ← P2 (AND de dos sub-predicados)
 *           throw new BusinessException(..., "La reserva requiere al menos " + roomMin + "...");
 *       }
 *   }
 *
 * Grafo de flujo y complejidad ciclomática
 * ----------------------------------------
 *   Predicados independientes:
 *     P1a: people > roomCapacity
 *     P1b: people > roomMax
 *     P2a: minRequired == true
 *     P2b: people < roomMin
 *
 *   Aristas (E) = 8  |  Nodos (N) = 6  |  Componentes (P) = 1
 *   M = E − N + 2P = 8 − 6 + 2 = 4
 *
 *   Caminos linealmente independientes:
 *     C1: P1a=F, P1b=F, P2a=F → retorno normal            (CB-CAP-01)
 *     C2: P1a=T  (P1b irrelevante) → excepción máximo     (CB-CAP-02)
 *     C3: P1a=F, P1b=T → excepción máximo                 (CB-CAP-03)
 *     C4: P1a=F, P1b=F, P2a=T, P2b=T → excepción mínimo  (CB-CAP-04)
 *     C5: P1a=F, P1b=F, P2a=T, P2b=F → retorno normal     (CB-CAP-05)
 *       → adicional: P2a=F → retorno normal (variante implícita en CB-CAP-01)
 *
 * Tabla de datos de entrada y resultados esperados
 * ------------------------------------------------
 * | ID         | capacidad | maximoPersonas | minimoPersonas | minimoObl. | people | Resultado esperado              |
 * |------------|-----------|---------------|----------------|------------|--------|---------------------------------|
 * | CB-CAP-01  |    20     |      20       |       5        |   false    |   10   | Sin excepción                   |
 * | CB-CAP-02  |    20     |      20       |       1        |   false    |   25   | BusinessException "máximo"      |
 * | CB-CAP-03  |    30     |      15       |       1        |   false    |   20   | BusinessException "máximo"      |
 * | CB-CAP-04  |    20     |      20       |      10        |   true     |    5   | BusinessException "mínimo"      |
 * | CB-CAP-05  |    20     |      20       |      10        |   false    |    5   | Sin excepción (mín no obl.)     |
 *
 * Estrategia de mocks
 * -------------------
 * - validateCapacity es el segundo paso de validate(). Para llegar a él se necesita
 *   que validateEndTimeAfterStart pase (end > start: siempre se cumple con VALID_END > VALID_START).
 * - En CB-CAP-01 y CB-CAP-05 (caminos sin excepción) los validadores posteriores también
 *   deben pasar; se configuran con mocks en estado "OK".
 * - En CB-CAP-02, CB-CAP-03 y CB-CAP-04 la excepción ocurre dentro de validateCapacity,
 *   así que los validadores posteriores no se ejecutan y no necesitan mock.
 * - AppTime se bloquea con MockedStatic para un lunes a las 07:00 (misma lógica que CB-01).
 * ============================================================
 */
@ExtendWith(MockitoExtension.class)
class BookingValidationCapacityTest {

    // -----------------------------------------------------------------------
    // Tiempo fijo compartido
    // -----------------------------------------------------------------------

    private static final LocalDateTime FIXED_NOW  = nextMondayAt7();
    private static final LocalDate     VALID_DATE  = FIXED_NOW.toLocalDate();
    private static final LocalTime     SLOT_OPEN   = LocalTime.of(7, 0);
    private static final LocalTime     VALID_START = LocalTime.of(9, 0);
    private static final LocalTime     VALID_END   = LocalTime.of(10, 0);
    private static final int           SLOT_MINUTES = 60;

    // -----------------------------------------------------------------------
    // Colaboradores mockeados
    // -----------------------------------------------------------------------

    @Mock private RoomScheduleService   roomScheduleService;
    @Mock private RoomService           roomService;
    @Mock private ReservationRepository reservationRepository;
    @Mock private SystemConfigService   systemConfigService;

    @InjectMocks
    private BookingValidationService sut;

    private UserEntity user;

    @BeforeEach
    void setUp() {
        user = new UserEntity();
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Construye una sala con los parámetros de capacidad exactos que necesita cada caso.
     *
     * @param capacidad           capacidad física de la sala
     * @param maximoPersonas      límite de personas configurado (roomMax)
     * @param minimoPersonas      mínimo de personas configurado (roomMin)
     * @param minimoObligatorio   si el mínimo es obligatorio
     */
    private RoomEntity buildRoom(int capacidad, int maximoPersonas,
                                 int minimoPersonas, boolean minimoObligatorio) {
        RoomEntity room = new RoomEntity();
        room.setCapacidad(capacidad);
        room.setMaximoPersonas(maximoPersonas);
        room.setMinimoPersonas(minimoPersonas);
        room.setMinimoPersonasObligatorio(minimoObligatorio);
        room.setEstado(RoomState.DISPONIBLE);
        CampusEntity campus = new CampusEntity();
        campus.setNombre("Monterrico");
        PabellonEntity pabellon = new PabellonEntity();
        pabellon.setCampus(campus);
        room.setPabellon(pabellon);
        return room;
    }

    /** Construye el request con la cantidad de personas indicada. */
    private BookingUpsertRequest requestWithPeople(int people) {
        return new BookingUpsertRequest(
            1L,
            VALID_DATE,
            VALID_START,
            VALID_END,
            people,
            "Monterrico - Aula A1",
            null
        );
    }

    /**
     * Configura los mocks de todos los validadores POSTERIORES a validateCapacity
     * para que pasen sin error. Solo se necesita en los casos de camino feliz
     * donde la ejecución llega hasta el final de validate().
     */
    private void configurePostCapacityMocksToPass(RoomEntity room) {
        when(roomScheduleService.getCampusSlotMinutes(any())).thenReturn(SLOT_MINUTES);

        EffectiveSchedule openSchedule = new EffectiveSchedule(
            VALID_DATE.getDayOfWeek().getValue(),
            SLOT_OPEN,
            LocalTime.of(22, 0),
            false,
            false
        );
        when(roomScheduleService.getEffectiveScheduleForRoomDay(any(), any())).thenReturn(openSchedule);
        when(roomService.isRoomAvailable(any(), any(), any(), any(), isNull())).thenReturn(true);
        when(systemConfigService.getMaxActiveBookings()).thenReturn(3);
        when(reservationRepository.countCurrentActiveForUser(any(), any(), any())).thenReturn(0L);
    }

    // -----------------------------------------------------------------------
    // CB-CAP-01 — Camino feliz: personas dentro del rango válido
    // Condición evaluada: P1a=F, P1b=F, P2a=F  →  sin excepción
    // Datos: people=10, capacity=20, roomMax=20, roomMin=5, minObl=false
    // -----------------------------------------------------------------------

    /**
     * Ruta cubierta:
     *   people(10) > roomCapacity(20) → FALSE
     *   people(10) > roomMax(20)      → FALSE  → no se lanza excepción de máximo
     *   minRequired(false) && ...     → FALSE  → no se lanza excepción de mínimo
     *   → retorno normal
     */
    @Test
    @DisplayName("CB-CAP-01: Personas dentro del rango válido — pasa sin excepción")
    void shouldPassWhenPeopleIsWithinValidRange() {
        try (MockedStatic<AppTime> appTime = mockStatic(AppTime.class)) {
            appTime.when(AppTime::nowDateTime).thenReturn(FIXED_NOW);
            appTime.when(AppTime::today).thenReturn(FIXED_NOW.toLocalDate());
            appTime.when(AppTime::nowTime).thenReturn(FIXED_NOW.toLocalTime());

            // people=10, capacity=20, max=20, min=5, minObl=false
            RoomEntity room = buildRoom(20, 20, 5, false);
            configurePostCapacityMocksToPass(room);

            assertDoesNotThrow(
                () -> sut.validate(user, room, requestWithPeople(10), null),
                "10 personas en sala de capacidad 20 no debe lanzar excepción"
            );
        }
    }

    // -----------------------------------------------------------------------
    // CB-CAP-02 — people > roomCapacity (P1a=T)
    // Condición evaluada: people(25) > roomCapacity(20) → TRUE
    // Datos: people=25, capacity=20, roomMax=20, roomMin=1, minObl=false
    // -----------------------------------------------------------------------

    /**
     * Ruta cubierta:
     *   people(25) > roomCapacity(20) → TRUE  → lanza "La cantidad de personas supera..."
     *   (el segundo operando del OR y el segundo if no se evalúan)
     */
    @Test
    @DisplayName("CB-CAP-02: Personas mayor que la capacidad física — lanza BusinessException")
    void shouldFailWhenPeopleExceedsRoomCapacity() {
        try (MockedStatic<AppTime> appTime = mockStatic(AppTime.class)) {
            appTime.when(AppTime::nowDateTime).thenReturn(FIXED_NOW);
            appTime.when(AppTime::today).thenReturn(FIXED_NOW.toLocalDate());
            appTime.when(AppTime::nowTime).thenReturn(FIXED_NOW.toLocalTime());

            // people=25, capacity=20 → P1a verdadero
            RoomEntity room = buildRoom(20, 20, 1, false);

            BusinessException ex = assertThrows(
                BusinessException.class,
                () -> sut.validate(user, room, requestWithPeople(25), null)
            );

            assertEquals(
                "La cantidad de personas supera el máximo permitido para la sala",
                ex.getMessage(),
                "Debe indicar que se supera el máximo cuando people > capacity"
            );
        }
    }

    // -----------------------------------------------------------------------
    // CB-CAP-03 — people > roomMax aunque people <= capacity (P1a=F, P1b=T)
    // Condición evaluada: people(20) <= capacity(30) pero people(20) > roomMax(15)
    // Datos: people=20, capacity=30, roomMax=15, roomMin=1, minObl=false
    // -----------------------------------------------------------------------

    /**
     * Ruta cubierta:
     *   people(20) > roomCapacity(30)  → FALSE
     *   people(20) > roomMax(15)       → TRUE   → OR es verdadero
     *   → lanza "La cantidad de personas supera..."
     *
     * Este caso distingue roomMax de capacity: roomMax es el límite operativo
     * configurado por el administrador, que puede ser menor que la capacidad física.
     */
    @Test
    @DisplayName("CB-CAP-03: Personas mayor que el máximo permitido (roomMax < capacity) — lanza BusinessException")
    void shouldFailWhenPeopleExceedsRoomMax() {
        try (MockedStatic<AppTime> appTime = mockStatic(AppTime.class)) {
            appTime.when(AppTime::nowDateTime).thenReturn(FIXED_NOW);
            appTime.when(AppTime::today).thenReturn(FIXED_NOW.toLocalDate());
            appTime.when(AppTime::nowTime).thenReturn(FIXED_NOW.toLocalTime());

            // capacity=30, roomMax=15 → people=20 pasa P1a pero falla P1b
            RoomEntity room = buildRoom(30, 15, 1, false);

            BusinessException ex = assertThrows(
                BusinessException.class,
                () -> sut.validate(user, room, requestWithPeople(20), null)
            );

            assertEquals(
                "La cantidad de personas supera el máximo permitido para la sala",
                ex.getMessage(),
                "Debe indicar que se supera el máximo cuando people > roomMax"
            );
        }
    }

    // -----------------------------------------------------------------------
    // CB-CAP-04 — Mínimo obligatorio no cumplido (P2a=T, P2b=T)
    // Condición evaluada: minRequired=true && people(5) < roomMin(10)
    // Datos: people=5, capacity=20, roomMax=20, roomMin=10, minObl=true
    // -----------------------------------------------------------------------

    /**
     * Ruta cubierta:
     *   people(5) > roomCapacity(20) → FALSE
     *   people(5) > roomMax(20)      → FALSE  → no se lanza excepción de máximo
     *   minRequired(true) && people(5) < roomMin(10) → TRUE
     *   → lanza "La reserva requiere al menos 10 personas para esta sala"
     */
    @Test
    @DisplayName("CB-CAP-04: Personas menor al mínimo obligatorio — lanza BusinessException")
    void shouldFailWhenPeopleIsBelowRequiredMinimum() {
        try (MockedStatic<AppTime> appTime = mockStatic(AppTime.class)) {
            appTime.when(AppTime::nowDateTime).thenReturn(FIXED_NOW);
            appTime.when(AppTime::today).thenReturn(FIXED_NOW.toLocalDate());
            appTime.when(AppTime::nowTime).thenReturn(FIXED_NOW.toLocalTime());

            // minimoObligatorio=true, people=5 < roomMin=10
            RoomEntity room = buildRoom(20, 20, 10, true);

            BusinessException ex = assertThrows(
                BusinessException.class,
                () -> sut.validate(user, room, requestWithPeople(5), null)
            );

            assertEquals(
                "La reserva requiere al menos 10 personas para esta sala",
                ex.getMessage(),
                "Debe indicar el mínimo cuando el mínimo es obligatorio y no se cumple"
            );
        }
    }

    // -----------------------------------------------------------------------
    // CB-CAP-05 — Mínimo NO obligatorio: people < roomMin pero se ignora (P2a=F)
    // Condición evaluada: minRequired=false → segundo if no lanza excepción
    // Datos: people=5, capacity=20, roomMax=20, roomMin=10, minObl=false
    // -----------------------------------------------------------------------

    /**
     * Ruta cubierta:
     *   people(5) > roomCapacity(20) → FALSE
     *   people(5) > roomMax(20)      → FALSE  → no se lanza excepción de máximo
     *   minRequired(false) && ...    → FALSE  → cortocircuito, no evalúa people < roomMin
     *   → retorno normal (el mínimo de personas no es obligatorio)
     *
     * Este caso contrasta directamente con CB-CAP-04: mismos datos de sala y personas,
     * única diferencia es minimoPersonasObligatorio=false.
     */
    @Test
    @DisplayName("CB-CAP-05: Personas menor al mínimo cuando mínimo NO es obligatorio — pasa sin excepción")
    void shouldPassWhenPeopleIsBelowMinimumButMinimumIsNotRequired() {
        try (MockedStatic<AppTime> appTime = mockStatic(AppTime.class)) {
            appTime.when(AppTime::nowDateTime).thenReturn(FIXED_NOW);
            appTime.when(AppTime::today).thenReturn(FIXED_NOW.toLocalDate());
            appTime.when(AppTime::nowTime).thenReturn(FIXED_NOW.toLocalTime());

            // minimoObligatorio=false → el mínimo se ignora aunque people < roomMin
            RoomEntity room = buildRoom(20, 20, 10, false);
            configurePostCapacityMocksToPass(room);

            assertDoesNotThrow(
                () -> sut.validate(user, room, requestWithPeople(5), null),
                "Cuando el mínimo no es obligatorio, people < roomMin no debe lanzar excepción"
            );
        }
    }

    // -----------------------------------------------------------------------
    // Utilidad estática
    // -----------------------------------------------------------------------

    /**
     * Devuelve el próximo lunes a las 07:00.
     * Si hoy ya es lunes, avanza una semana más para que FIXED_NOW esté siempre
     * en el futuro y VALID_DATE (ese mismo lunes) quede dentro de la semana actual.
     */
    private static LocalDateTime nextMondayAt7() {
        LocalDate today = LocalDate.now();
        int daysUntilMonday = (DayOfWeek.MONDAY.getValue() - today.getDayOfWeek().getValue() + 7) % 7;
        LocalDate monday = today.plusDays(daysUntilMonday == 0 ? 7 : daysUntilMonday);
        return monday.atTime(LocalTime.of(7, 0));
    }
}
