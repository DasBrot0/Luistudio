package com.luistudio.reservas.service.booking.validation;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
 * Prueba de Caja Blanca #4 - validateRoomAvailability y validateMaxActiveBookings
 * ============================================================
 *
 * Modulos evaluados
 * -----------------
 * - BookingValidationService.validateRoomAvailability(RoomEntity, BookingUpsertRequest, Long)
 * - BookingValidationService.validateMaxActiveBookings(UserEntity, Long)
 * -- metodos privados accedidos a traves de validate().
 *
 * Codigo fuente evaluado
 * ----------------------
 *
 *   // ---- validateRoomAvailability ----
 *   private void validateRoomAvailability(
 *       RoomEntity room, BookingUpsertRequest request, Long excludeBookingId) {
 *
 *       boolean available = roomService.isRoomAvailable(
 *           room, request.date(), request.start(), request.end(), excludeBookingId); // (A) delega
 *
 *       [PA] if (!available) {                                                       // linea 154
 *           throw new BusinessException(BAD_REQUEST,
 *               "La sala no esta disponible para el horario seleccionado");
 *       }
 *   }
 *
 *   // ---- validateMaxActiveBookings ----
 *   private void validateMaxActiveBookings(UserEntity user, Long excludeBookingId) {
 *
 *       [PE] if (excludeBookingId != null) {                                         // linea 160
 *           return;   // <-- retorno temprano: NO se cuenta nada
 *       }
 *
 *       int maxAllowed  = systemConfigService.getMaxActiveBookings();
 *       long activeCount = reservationRepository.countCurrentActiveForUser(
 *           user, AppTime.today(), AppTime.nowTime());
 *
 *       [PC] if (activeCount >= maxAllowed) {                                        // linea 166
 *           throw new BusinessException(BAD_REQUEST,
 *               "Alcanzaste el limite de reservas activas (" + maxAllowed + ")");
 *       }
 *   }
 *
 * Grafo de flujo y complejidad ciclomatica
 * -----------------------------------------
 *   Predicados independientes:
 *     PA: !available           (validateRoomAvailability)
 *     PE: excludeBookingId != null (validateMaxActiveBookings - retorno temprano)
 *     PC: activeCount >= maxAllowed (validateMaxActiveBookings - limite alcanzado)
 *
 *   Nodos de decision = 3  ->  M = 3 + 1 = 4
 *
 *   Caminos linealmente independientes:
 *     AV-01: PA=F, PE=F, PC=F  ->  retorno normal (sala disponible, limite no alcanzado)
 *     AV-02: PA=T               ->  excepcion sala no disponible
 *     AV-03: PA=F, PE=T         ->  retorno temprano en validateMaxActiveBookings (edicion)
 *     AV-04: PA=F, PE=F, PC=T   ->  excepcion limite de reservas activas alcanzado
 *
 *   Nota: el caso "sala disponible con excludeBookingId no nulo" (PA=F, PE=T) cubre
 *   adicionalmente que roomService.isRoomAvailable recibe el excludeBookingId correcto.
 *
 * Tabla de casos de prueba
 * -------------------------
 * | ID    | isRoomAvailable | excludeBookingId | activeCount | maxAllowed | Resultado esperado                             |
 * |-------|-----------------|------------------|-------------|------------|------------------------------------------------|
 * | AV-01 | true            | null             | 0           | 3          | Sin excepcion (camino feliz completo)          |
 * | AV-02 | false           | null             | -           | -          | "La sala no esta disponible..."                |
 * | AV-03 | true            | 99L              | -           | -          | Sin excepcion; countCurrentActiveForUser NO    |
 *          |                 |                  |             |            | debe invocarse (retorno temprano PE)           |
 * | AV-04 | true            | null             | 2           | 2          | "Alcanzaste el limite de reservas activas (2)" |
 * | AV-05 | true            | null             | 1           | 2          | Sin excepcion (por debajo del limite)          |
 *
 * Estrategia de mocks
 * -------------------
 *   - AppTime se fija con MockedStatic (lunes 07:00).
 *   - Para llegar a validateRoomAvailability y validateMaxActiveBookings, todos los
 *     validadores anteriores (endTime, capacity, bookingWindowAndSlot, maxDuration)
 *     deben pasar; se configuran con mocks en estado "OK" en configurePreMocks().
 *   - En AV-02 la excepcion ocurre en validateRoomAvailability; el contador de reservas
 *     activas NO se llama y no requiere stub.
 *   - En AV-03 se verifica con verify(..., never()) que countCurrentActiveForUser
 *     no se invoca cuando excludeBookingId != null.
 * ============================================================
 */
@ExtendWith(MockitoExtension.class)
class BookingValidationAvailabilityAndLimitTest {

    // -----------------------------------------------------------------------
    // Tiempo fijo compartido
    // -----------------------------------------------------------------------

    private static final LocalDateTime FIXED_NOW    = nextMondayAt7();
    private static final LocalDate     VALID_DATE   = FIXED_NOW.toLocalDate();
    private static final LocalTime     OPEN_TIME    = LocalTime.of(7, 0);
    private static final LocalTime     CLOSE_TIME   = LocalTime.of(22, 0);
    private static final LocalTime     VALID_START  = LocalTime.of(9, 0);
    private static final LocalTime     VALID_END    = LocalTime.of(10, 0);
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

    private RoomEntity room;
    private UserEntity user;

    // -----------------------------------------------------------------------
    // Setup
    // -----------------------------------------------------------------------

    @BeforeEach
    void setUp() {
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

    /** Request base completamente valido (fecha=lunes, 09:00-10:00, 5 personas). */
    private BookingUpsertRequest validRequest() {
        return new BookingUpsertRequest(
            1L, VALID_DATE, VALID_START, VALID_END,
            5, "Monterrico - Aula A1", null
        );
    }

    /**
     * Configura los mocks que preceden a validateRoomAvailability para que pasen
     * sin error:
     *   - validateEndTimeAfterStart: OK porque end(10:00) > start(09:00).
     *   - validateCapacity: OK porque people(5) <= capacity(20).
     *   - validateBookingWindowAndSlot: OK con el horario y slot base.
     *   - validateMaxDuration: OK porque duration(60) == slot(60).
     */
    private void configurePreMocks() {
        when(roomScheduleService.getCampusSlotMinutes(any())).thenReturn(SLOT_MINUTES);

        EffectiveSchedule openSchedule = new EffectiveSchedule(
            VALID_DATE.getDayOfWeek().getValue(),
            OPEN_TIME, CLOSE_TIME,
            false, false
        );
        when(roomScheduleService.getEffectiveScheduleForRoomDay(any(), any())).thenReturn(openSchedule);
    }

    // -----------------------------------------------------------------------
    // AV-01 -- Sala disponible y usuario dentro del limite (camino feliz completo)
    // Camino: PA=F, PE=F, PC=F  ->  retorno normal
    // -----------------------------------------------------------------------

    /**
     * Ruta cubierta: PA->F, PE->F, PC->F.
     *   - roomService.isRoomAvailable devuelve true  -> PA=F, no lanza excepcion.
     *   - excludeBookingId = null  -> PE=F, continua hacia el conteo.
     *   - activeCount(0) < maxAllowed(3)  -> PC=F, retorno normal.
     *
     * Condicion evaluada: camino completo sin ninguna excepcion.
     * Verifica que validate() complete su ejecucion cuando sala disponible y
     * el usuario no ha alcanzado el limite de reservas activas.
     */
    @Test
    @DisplayName("AV-01: Sala disponible y usuario dentro del limite -- pasa sin excepcion")
    void shouldPassWhenRoomIsAvailableAndUserIsWithinActiveBookingLimit() {
        try (MockedStatic<AppTime> appTime = mockStatic(AppTime.class)) {
            appTime.when(AppTime::nowDateTime).thenReturn(FIXED_NOW);
            appTime.when(AppTime::today).thenReturn(FIXED_NOW.toLocalDate());
            appTime.when(AppTime::nowTime).thenReturn(FIXED_NOW.toLocalTime());

            configurePreMocks();

            // PA=F: sala disponible
            when(roomService.isRoomAvailable(any(), any(), any(), any(), isNull())).thenReturn(true);

            // PE=F (excludeBookingId=null) y PC=F: activeCount(0) < maxAllowed(3)
            when(systemConfigService.getMaxActiveBookings()).thenReturn(3);
            when(reservationRepository.countCurrentActiveForUser(any(), any(), any())).thenReturn(0L);

            assertDoesNotThrow(
                () -> sut.validate(user, room, validRequest(), null),
                "Debe pasar sin excepcion cuando la sala esta disponible y el usuario no supero el limite"
            );
        }
    }

    // -----------------------------------------------------------------------
    // AV-02 -- Sala no disponible por solapamiento (PA=T)
    // Camino: PA=T  ->  excepcion inmediata en validateRoomAvailability
    // -----------------------------------------------------------------------

    /**
     * Ruta cubierta: PA->TRUE.
     *   - roomService.isRoomAvailable devuelve false  -> !available = true -> PA=T.
     *   - Lanza BusinessException "La sala no esta disponible...".
     *   - validateMaxActiveBookings NO se ejecuta (excepcion ya lanzada).
     *
     * Condicion evaluada: !available  (false devuelto por roomService.isRoomAvailable)
     * Mensaje esperado:   "La sala no esta disponible para el horario seleccionado"
     */
    @Test
    @DisplayName("AV-02: Sala no disponible por solapamiento -- lanza BusinessException (PA: !available)")
    void shouldFailWhenRoomIsNotAvailableDueToOverlap() {
        try (MockedStatic<AppTime> appTime = mockStatic(AppTime.class)) {
            appTime.when(AppTime::nowDateTime).thenReturn(FIXED_NOW);
            appTime.when(AppTime::today).thenReturn(FIXED_NOW.toLocalDate());
            appTime.when(AppTime::nowTime).thenReturn(FIXED_NOW.toLocalTime());

            configurePreMocks();

            // PA=T: sala NO disponible (solapamiento con otra reserva activa)
            when(roomService.isRoomAvailable(any(), any(), any(), any(), isNull())).thenReturn(false);

            BusinessException ex = assertThrows(
                BusinessException.class,
                () -> sut.validate(user, room, validRequest(), null)
            );
            assertEquals(
                "La sala no está disponible para el horario seleccionado",
                ex.getMessage(),
                "Debe rechazar cuando roomService.isRoomAvailable devuelve false"
            );
        }
    }

    // -----------------------------------------------------------------------
    // AV-03 -- Edicion de reserva con excludeBookingId (PE=T -> retorno temprano)
    // Camino: PA=F, PE=T  ->  retorno temprano; countCurrentActiveForUser NO se llama
    // -----------------------------------------------------------------------

    /**
     * Ruta cubierta: PA->F, PE->TRUE.
     *   - roomService.isRoomAvailable recibe excludeBookingId=99L (no nulo).
     *   - Devuelve true -> PA=F.
     *   - excludeBookingId(99L) != null -> PE=T -> retorno temprano.
     *   - countCurrentActiveForUser y getMaxActiveBookings NUNCA se invocan.
     *
     * Condicion evaluada: excludeBookingId != null  (99L != null)
     * Resultado esperado: sin excepcion; el limite de reservas activas se omite
     *   completamente, permitiendo la edicion sin contar la reserva existente.
     *
     * Verificacion adicional:
     *   verify(reservationRepository, never()).countCurrentActiveForUser(...)
     *   confirma que PE=T produce retorno temprano real, no solo camino no lanzador.
     */
    @Test
    @DisplayName("AV-03: Edicion con excludeBookingId -- retorno temprano, no se cuenta limite (PE: excludeBookingId != null)")
    void shouldSkipActiveBookingCountWhenExcludeBookingIdIsProvided() {
        final Long EXCLUDE_ID = 99L;

        try (MockedStatic<AppTime> appTime = mockStatic(AppTime.class)) {
            appTime.when(AppTime::nowDateTime).thenReturn(FIXED_NOW);
            appTime.when(AppTime::today).thenReturn(FIXED_NOW.toLocalDate());
            appTime.when(AppTime::nowTime).thenReturn(FIXED_NOW.toLocalTime());

            configurePreMocks();

            // PA=F: sala disponible (se pasa el excludeBookingId correctamente)
            when(roomService.isRoomAvailable(
                any(), any(), any(), any(), eq(EXCLUDE_ID))
            ).thenReturn(true);

            // PE=T: no debe llamarse ni getMaxActiveBookings ni countCurrentActiveForUser
            assertDoesNotThrow(
                () -> sut.validate(user, room, validRequest(), EXCLUDE_ID),
                "Editar una reserva existente no debe lanzar excepcion"
            );

            // Verificacion de caja blanca: el retorno temprano de PE hace que
            // countCurrentActiveForUser jamas se invoque en esta ruta
            verify(reservationRepository, never())
                .countCurrentActiveForUser(any(), any(), any());
            verify(systemConfigService, never())
                .getMaxActiveBookings();
        }
    }

    // -----------------------------------------------------------------------
    // AV-04 -- Usuario con limite maximo alcanzado (PA=F, PE=F, PC=T)
    // Camino: PA=F, PE=F, activeCount >= maxAllowed  ->  excepcion limite
    // -----------------------------------------------------------------------

    /**
     * Ruta cubierta: PA->F, PE->F, PC->TRUE.
     *   - Sala disponible -> PA=F.
     *   - excludeBookingId=null -> PE=F, sigue al conteo.
     *   - activeCount(2) >= maxAllowed(2) -> PC=T -> lanza excepcion.
     *
     * Condicion evaluada: activeCount >= maxAllowed  (2 >= 2, usando limite exacto)
     * Mensaje esperado:   "Alcanzaste el limite de reservas activas (2)"
     *
     * Nota: se usa igualdad estricta (activeCount == maxAllowed) para verificar
     *   que el predicado '>=' incluye el caso de borde justo en el limite.
     */
    @Test
    @DisplayName("AV-04: Limite de reservas activas alcanzado -- lanza BusinessException (PC: activeCount >= maxAllowed)")
    void shouldFailWhenUserReachesMaxActiveBookingsLimit() {
        try (MockedStatic<AppTime> appTime = mockStatic(AppTime.class)) {
            appTime.when(AppTime::nowDateTime).thenReturn(FIXED_NOW);
            appTime.when(AppTime::today).thenReturn(FIXED_NOW.toLocalDate());
            appTime.when(AppTime::nowTime).thenReturn(FIXED_NOW.toLocalTime());

            configurePreMocks();

            // PA=F: sala disponible
            when(roomService.isRoomAvailable(any(), any(), any(), any(), isNull())).thenReturn(true);

            // PE=F (excludeBookingId=null), PC=T: activeCount(2) >= maxAllowed(2)
            when(systemConfigService.getMaxActiveBookings()).thenReturn(2);
            when(reservationRepository.countCurrentActiveForUser(
                eq(user), any(LocalDate.class), any(LocalTime.class))
            ).thenReturn(2L);

            BusinessException ex = assertThrows(
                BusinessException.class,
                () -> sut.validate(user, room, validRequest(), null)
            );
            assertEquals(
                "Alcanzaste el límite de reservas activas (2)",
                ex.getMessage(),
                "Debe rechazar cuando el contador de reservas activas iguala el maximo permitido"
            );
        }
    }

    // -----------------------------------------------------------------------
    // AV-05 -- Usuario por debajo del limite (PA=F, PE=F, PC=F -- variante de borde)
    // Camino: PA=F, PE=F, PC=F con activeCount = maxAllowed - 1
    // -----------------------------------------------------------------------

    /**
     * Ruta cubierta: PA->F, PE->F, PC->FALSE.
     *   - Sala disponible -> PA=F.
     *   - excludeBookingId=null -> PE=F.
     *   - activeCount(1) < maxAllowed(2) -> PC=F -> retorno normal.
     *
     * Condicion evaluada: activeCount < maxAllowed  (1 < 2)
     * Complementa AV-04: mismo maxAllowed=2 pero con activeCount=1 (un caso debajo
     * del borde), confirmando que el predicado '>=' no falla en este caso.
     */
    @Test
    @DisplayName("AV-05: Usuario con una reserva activa por debajo del limite -- pasa sin excepcion")
    void shouldPassWhenUserHasOneActiverBookingBelowLimit() {
        try (MockedStatic<AppTime> appTime = mockStatic(AppTime.class)) {
            appTime.when(AppTime::nowDateTime).thenReturn(FIXED_NOW);
            appTime.when(AppTime::today).thenReturn(FIXED_NOW.toLocalDate());
            appTime.when(AppTime::nowTime).thenReturn(FIXED_NOW.toLocalTime());

            configurePreMocks();

            // PA=F: sala disponible
            when(roomService.isRoomAvailable(any(), any(), any(), any(), isNull())).thenReturn(true);

            // PE=F (excludeBookingId=null), PC=F: activeCount(1) < maxAllowed(2)
            when(systemConfigService.getMaxActiveBookings()).thenReturn(2);
            when(reservationRepository.countCurrentActiveForUser(
                eq(user), any(LocalDate.class), any(LocalTime.class))
            ).thenReturn(1L);

            assertDoesNotThrow(
                () -> sut.validate(user, room, validRequest(), null),
                "Debe pasar sin excepcion cuando activeCount esta estrictamente por debajo del limite"
            );
        }
    }

    // -----------------------------------------------------------------------
    // Utilidad estatica
    // -----------------------------------------------------------------------

    /**
     * Devuelve el proximo lunes a las 07:00 (siempre en el futuro).
     * Si hoy ya es lunes, avanza una semana para que VALID_DATE
     * siempre sea una fecha futura respecto a LocalDate.now().
     */
    private static LocalDateTime nextMondayAt7() {
        LocalDate today = LocalDate.now();
        int daysUntilMonday = (DayOfWeek.MONDAY.getValue() - today.getDayOfWeek().getValue() + 7) % 7;
        LocalDate monday = today.plusDays(daysUntilMonday == 0 ? 7 : daysUntilMonday);
        return monday.atTime(LocalTime.of(7, 0));
    }
}
