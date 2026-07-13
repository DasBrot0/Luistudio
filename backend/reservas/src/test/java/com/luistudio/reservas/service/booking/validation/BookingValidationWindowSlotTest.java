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
 * Prueba de Caja Blanca #3 — validateBookingWindowAndSlot(...)
 * ============================================================
 *
 * Módulo evaluado
 * ---------------
 * BookingValidationService.validateBookingWindowAndSlot(RoomEntity, BookingUpsertRequest)
 * — método privado accedido a través de validate().
 *
 * Código fuente evaluado
 * ----------------------
 *   private void validateBookingWindowAndSlot(RoomEntity room, BookingUpsertRequest request) {
 *       LocalDateTime now = AppTime.nowDateTime();                                          // (A)
 *       LocalDate today   = now.toLocalDate();
 *       LocalDate requestDate = request.date();
 *
 *       [P1] if (requestDate.isBefore(today)) {                                        // línea 80
 *           throw new BusinessException(BAD_REQUEST, "Solo puedes reservar en horas y fechas futuras");
 *       }
 *
 *       LocalDate monday        = today.minusDays(today.getDayOfWeek().getValue() - MONDAY.getValue());
 *       LocalDate endCurrentWeek = monday.plusDays(6);
 *       boolean weekendToday    = today is SAT or SUN
 *       LocalDate maxAllowedDate = weekendToday ? endCurrentWeek.plusDays(7) : endCurrentWeek;
 *       [P2] if (requestDate.isAfter(maxAllowedDate)) {                                // línea 88
 *           throw new BusinessException(BAD_REQUEST, "Solo puedes reservar dentro de la semana actual" [o variante fin de semana]);
 *       }
 *
 *       [P3] if (requestDate.isEqual(today) && !request.start().isAfter(now.toLocalTime())) { // línea 97
 *           throw new BusinessException(BAD_REQUEST, "Solo puedes reservar en horarios posteriores a la hora actual");
 *       }
 *
 *       int slotMinutes    = roomScheduleService.getCampusSlotMinutes(room.getCampus());
 *       int durationMinutes = (int) Duration.between(request.start(), request.end()).toMinutes();
 *       [P4] if (durationMinutes != slotMinutes) {                                     // línea 103
 *           throw new BusinessException(BAD_REQUEST, "La reserva debe ocupar exactamente un bloque de " + slotMinutes + " minutos ...");
 *       }
 *
 *       EffectiveSchedule schedule = roomScheduleService.getEffectiveScheduleForRoomDay(room, requestDate);
 *       [P5] if (schedule.closed() || openTime == null || closeTime == null) {         // línea 111
 *           throw new BusinessException(BAD_REQUEST, "La sala no atiende en esa fecha");
 *       }
 *
 *       [P6] if (start.isBefore(openTime) || end.isAfter(closeTime)) {                // línea 117
 *           throw new BusinessException(BAD_REQUEST, "El horario no está dentro del rango disponible de la sala");
 *       }
 *
 *       long fromOpenToStart = Duration.between(openTime, start).toMinutes();
 *       long fromOpenToEnd   = Duration.between(openTime, end).toMinutes();
 *       [P7] if (fromOpenToStart < 0 || fromOpenToEnd < 0
 *                    || fromOpenToStart % slotMinutes != 0 || fromOpenToEnd % slotMinutes != 0) { // línea 123
 *           throw new BusinessException(BAD_REQUEST, "El horario debe alinearse con bloques de " + slotMinutes + " minutos ...");
 *       }
 *   }
 *
 * Grafo de flujo y complejidad ciclomática
 * ----------------------------------------
 *   Predicados independientes (cada uno cuenta como un nodo de decisión):
 *     P1: requestDate.isBefore(today)
 *     P2: requestDate.isAfter(maxAllowedDate)
 *     P3: requestDate.isEqual(today) AND !start.isAfter(nowTime)
 *     P4: durationMinutes != slotMinutes
 *     P5: schedule.closed() OR openTime==null OR closeTime==null
 *     P6: start.isBefore(openTime) OR end.isAfter(closeTime)
 *     P7: fromOpenToStart%slot != 0 OR fromOpenToEnd%slot != 0
 *
 *   Nodos de decisión = 7  -> M = 7 + 1 = 8
 *
 *   Caminos linealmente independientes:
 *     WS-01: P1=F, P2=F, P3=F, P4=F, P5=F, P6=F, P7=F -> retorno normal
 *     WS-02: P1=T                                       -> excepción fecha pasada
 *     WS-03: P1=F, P2=T                                 -> excepción fuera de semana
 *     WS-04: P1=F, P2=F, P3=T                           -> excepción hora actual
 *     WS-05: P1=F, P2=F, P3=F, P4=T                    -> excepción duración incorrecta
 *     WS-06: P1=F, P2=F, P3=F, P4=F, P5=T              -> excepción sala cerrada
 *     WS-07: P1=F, P2=F, P3=F, P4=F, P5=F, P6=T        -> excepción fuera de horario
 *     WS-08: P1=F, P2=F, P3=F, P4=F, P5=F, P6=F, P7=T  -> excepción no alineado
 *
 * Tabla de casos de prueba
 * -------------------------
 * | ID    | now (fijo)         | requestDate    | start  | end    | slot | schedule     | Resultado esperado                              |
 * |-------|--------------------|----------------|--------|--------|------|-------------|--------------------------------------------------|
 * | WS-01 | lunes 07:00        | ese lunes      | 09:00  | 10:00  |  60  | 07:00-22:00 | Sin excepción                                    |
 * | WS-02 | lunes 07:00        | lunes anterior | 09:00  | 10:00  |  60  | 07:00-22:00 | "Solo puedes reservar en horas y fechas futuras" |
 * | WS-03 | lunes 07:00        | lunes +8 días  | 09:00  | 10:00  |  60  | 07:00-22:00 | "Solo puedes reservar dentro de la semana actual"|
 * | WS-04 | lunes 10:30        | ese lunes      | 10:00  | 11:00  |  60  | 07:00-22:00 | "Solo puedes reservar en horarios posteriores..."  |
 * | WS-05 | lunes 07:00        | ese lunes      | 09:00  | 10:30  |  60  | 07:00-22:00 | "La reserva debe ocupar exactamente un bloque..."  |
 * | WS-06 | lunes 07:00        | ese lunes      | 09:00  | 10:00  |  60  | closed=true | "La sala no atiende en esa fecha"                |
 * | WS-07 | lunes 07:00        | ese lunes      | 06:00  | 07:00  |  60  | 08:00-22:00 | "El horario no está dentro del rango disponible..."|
 * | WS-08 | lunes 07:00        | ese lunes      | 09:30  | 10:30  |  60  | 07:00-22:00 | "El horario debe alinearse con bloques de 60..."   |
 *
 * Estrategia de mocks
 * -------------------
 *   - AppTime.nowDateTime() se fija con MockedStatic en cada test.
 *   - Para tests WS-02 a WS-08 (caminos de error) los validadores anteriores se dejan en
 *     estado "válido" y solo la condición bajo prueba activa la excepción.
 *   - Para WS-01 (camino feliz) se configuran todos los mocks en estado "OK",
 *     incluyendo validateRoomAvailability y validateMaxActiveBookings.
 * ============================================================
 */
@ExtendWith(MockitoExtension.class)
class BookingValidationWindowSlotTest {

    // -----------------------------------------------------------------------
    // Tiempo fijo compartido por toda la suite
    // "Ahora" = próximo lunes a las 07:00 -> slot válido = 09:00-10:00
    // -----------------------------------------------------------------------

    /** Lunes fijo a las 07:00, siempre en el futuro respecto a LocalDate.now(). */
    private static final LocalDateTime FIXED_NOW   = nextMondayAt7();
    private static final LocalDate     VALID_DATE  = FIXED_NOW.toLocalDate();   // ese lunes

    /** La apertura de la sala base: 07:00. */
    private static final LocalTime     OPEN_TIME   = LocalTime.of(7, 0);
    private static final LocalTime     CLOSE_TIME  = LocalTime.of(22, 0);

    /**
     * Slot base de 09:00 a 10:00 (60 min exactos, alineado desde 07:00: (09:00-07:00)=120 min -> 120%60=0 ✓).
     */
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

    /**
     * Request base completamente válido:
     *   fecha = VALID_DATE (ese lunes), start=09:00, end=10:00, people=5.
     */
    private BookingUpsertRequest validRequest() {
        return new BookingUpsertRequest(
            1L, VALID_DATE, VALID_START, VALID_END,
            5, "Monterrico - Aula A1", null
        );
    }

    /**
     * Request con una fecha específica; resto igual al base.
     */
    private BookingUpsertRequest requestWithDate(LocalDate date) {
        return new BookingUpsertRequest(
            1L, date, VALID_START, VALID_END,
            5, "Monterrico - Aula A1", null
        );
    }

    /**
     * Request con horario específico; fecha = VALID_DATE.
     */
    private BookingUpsertRequest requestWithSlot(LocalTime start, LocalTime end) {
        return new BookingUpsertRequest(
            1L, VALID_DATE, start, end,
            5, "Monterrico - Aula A1", null
        );
    }

    /**
     * EffectiveSchedule base: sala abierta de 07:00 a 22:00 (lunes).
     */
    private EffectiveSchedule openSchedule() {
        return new EffectiveSchedule(
            VALID_DATE.getDayOfWeek().getValue(),
            OPEN_TIME, CLOSE_TIME,
            false, false
        );
    }

    /**
     * Configura los mocks que SUPERAN a validateBookingWindowAndSlot (para el camino feliz)
     * más validateRoomAvailability y validateMaxActiveBookings.
     */
    private void configurePostWindowMocksToPass() {
        when(roomService.isRoomAvailable(any(), any(), any(), any(), isNull())).thenReturn(true);
        when(systemConfigService.getMaxActiveBookings()).thenReturn(3);
        when(reservationRepository.countCurrentActiveForUser(any(), any(), any())).thenReturn(0L);
    }

    // -----------------------------------------------------------------------
    // WS-01 — Reserva válida: todas las condiciones de ventana y slot se cumplen
    // Camino: P1=F, P2=F, P3=F, P4=F, P5=F, P6=F, P7=F -> retorno normal
    // -----------------------------------------------------------------------

    /**
     * Ruta cubierta (P1->F, P2->F, P3->F, P4->F, P5->F, P6->F, P7->F):
     *   - requestDate = ese lunes == today -> NO es pasado (P1=F)
     *   - requestDate == endCurrentWeek (ese mismo lunes está dentro de la semana) -> P2=F
     *   - start(09:00) > nowTime(07:00) -> P3=F
     *   - duration(60 min) == slot(60 min) -> P4=F
     *   - schedule abierta -> P5=F
     *   - start(09:00) >= open(07:00) && end(10:00) <= close(22:00) -> P6=F
     *   - (09:00-07:00)=120, 120%60=0 y (10:00-07:00)=180, 180%60=0 -> P7=F
     *   -> todo validateBookingWindowAndSlot pasa; validate() completo sin excepción.
     */
    @Test
    @DisplayName("WS-01: Reserva dentro del día y horario permitido — pasa sin excepción")
    void shouldPassWhenBookingIsFullyValidWithinWindowAndSlot() {
        try (MockedStatic<AppTime> appTime = mockStatic(AppTime.class)) {
            appTime.when(AppTime::nowDateTime).thenReturn(FIXED_NOW);
            appTime.when(AppTime::today).thenReturn(FIXED_NOW.toLocalDate());
            appTime.when(AppTime::nowTime).thenReturn(FIXED_NOW.toLocalTime());

            when(roomScheduleService.getCampusSlotMinutes(any())).thenReturn(SLOT_MINUTES);
            when(roomScheduleService.getEffectiveScheduleForRoomDay(any(), any())).thenReturn(openSchedule());
            configurePostWindowMocksToPass();

            assertDoesNotThrow(
                () -> sut.validate(user, room, validRequest(), null),
                "Una reserva completamente válida no debe lanzar ninguna excepción"
            );
        }
    }

    // -----------------------------------------------------------------------
    // WS-02 — Reserva en fecha pasada  (P1=T)
    // Camino: requestDate.isBefore(today) == TRUE -> excepción inmediata
    // -----------------------------------------------------------------------

    /**
     * Ruta cubierta: P1 -> TRUE.
     *   requestDate = lunes anterior (7 días antes de VALID_DATE) < today -> lanza excepción.
     *   Los predicados P2..P7 NO se evalúan.
     *
     * Condición evaluada: requestDate.isBefore(today)
     * Mensaje esperado:   "Solo puedes reservar en horas y fechas futuras"
     */
    @Test
    @DisplayName("WS-02: Reserva en fecha pasada — lanza BusinessException (P1: requestDate.isBefore(today))")
    void shouldFailWhenRequestDateIsInThePast() {
        try (MockedStatic<AppTime> appTime = mockStatic(AppTime.class)) {
            appTime.when(AppTime::nowDateTime).thenReturn(FIXED_NOW);
            appTime.when(AppTime::today).thenReturn(FIXED_NOW.toLocalDate());
            appTime.when(AppTime::nowTime).thenReturn(FIXED_NOW.toLocalTime());

            // Fecha de hace una semana -> siempre antes de today
            LocalDate pastDate = VALID_DATE.minusWeeks(1);
            BookingUpsertRequest req = requestWithDate(pastDate);

            BusinessException ex = assertThrows(
                BusinessException.class,
                () -> sut.validate(user, room, req, null)
            );
            assertEquals(
                "Solo puedes reservar en horas y fechas futuras",
                ex.getMessage(),
                "Debe rechazar una reserva cuya fecha es anterior a hoy"
            );
        }
    }

    // -----------------------------------------------------------------------
    // WS-03 — Reserva fuera de la semana permitida (P1=F, P2=T)
    // Camino: requestDate es el lunes de la semana siguiente+1 (8 días adelante)
    // -----------------------------------------------------------------------

    /**
     * Ruta cubierta: P1->F, P2->TRUE.
     *   - FIXED_NOW es lunes (día laboral) -> weekendToday=false.
     *   - maxAllowedDate = endCurrentWeek = ese domingo (lunes + 6 días).
     *   - requestDate = VALID_DATE + 8 días > maxAllowedDate -> P2=T -> lanza excepción.
     *
     * Condición evaluada: requestDate.isAfter(maxAllowedDate) con weekendToday=false
     * Mensaje esperado:   "Solo puedes reservar dentro de la semana actual"
     */
    @Test
    @DisplayName("WS-03: Reserva fuera de la semana actual — lanza BusinessException (P2: requestDate.isAfter(maxAllowedDate))")
    void shouldFailWhenRequestDateIsOutsideCurrentWeek() {
        try (MockedStatic<AppTime> appTime = mockStatic(AppTime.class)) {
            appTime.when(AppTime::nowDateTime).thenReturn(FIXED_NOW);
            appTime.when(AppTime::today).thenReturn(FIXED_NOW.toLocalDate());
            appTime.when(AppTime::nowTime).thenReturn(FIXED_NOW.toLocalTime());

            // VALID_DATE es lunes -> endCurrentWeek = ese domingo (VALID_DATE + 6).
            // +8 días supera el domingo de la semana actual.
            LocalDate tooFarDate = VALID_DATE.plusDays(8);
            BookingUpsertRequest req = requestWithDate(tooFarDate);

            BusinessException ex = assertThrows(
                BusinessException.class,
                () -> sut.validate(user, room, req, null)
            );
            assertEquals(
                "Solo puedes reservar dentro de la semana actual",
                ex.getMessage(),
                "Debe rechazar una reserva más allá del domingo de la semana actual"
            );
        }
    }

    // -----------------------------------------------------------------------
    // WS-04 — Reserva para hoy con hora de inicio anterior a la hora actual (P3=T)
    // Camino: P1=F, P2=F, requestDate==today && start <= nowTime -> P3=T
    // -----------------------------------------------------------------------

    /**
     * Ruta cubierta: P1->F, P2->F, P3->TRUE.
     *   - requestDate = VALID_DATE = hoy (lunes).
     *   - "Ahora" fijado a las 10:30 -> start=10:00 no es posterior a 10:30 -> P3=T.
     *
     * Condición evaluada: requestDate.isEqual(today) && !request.start().isAfter(nowTime)
     *   Sub-condición 1: requestDate == today  -> TRUE
     *   Sub-condición 2: start(10:00) <= nowTime(10:30) -> !isAfter -> TRUE
     * Mensaje esperado:  "Solo puedes reservar en horarios posteriores a la hora actual"
     */
    @Test
    @DisplayName("WS-04: Reserva hoy con start <= hora actual — lanza BusinessException (P3: start no es futuro)")
    void shouldFailWhenStartTimeIsNotAfterCurrentTimeOnSameDay() {
        // Fijamos "ahora" a las 10:30 -> start=10:00 NO es posterior
        LocalDateTime nowAt1030 = VALID_DATE.atTime(LocalTime.of(10, 30));

        try (MockedStatic<AppTime> appTime = mockStatic(AppTime.class)) {
            appTime.when(AppTime::nowDateTime).thenReturn(nowAt1030);
            appTime.when(AppTime::today).thenReturn(nowAt1030.toLocalDate());
            appTime.when(AppTime::nowTime).thenReturn(nowAt1030.toLocalTime());

            // start=10:00 <= now=10:30 -> debe fallar
            BookingUpsertRequest req = requestWithSlot(LocalTime.of(10, 0), LocalTime.of(11, 0));

            BusinessException ex = assertThrows(
                BusinessException.class,
                () -> sut.validate(user, room, req, null)
            );
            assertEquals(
                "Solo puedes reservar en horarios posteriores a la hora actual",
                ex.getMessage(),
                "Debe rechazar reserva cuyo inicio ya pasó o es igual a la hora actual"
            );
        }
    }

    // -----------------------------------------------------------------------
    // WS-05 — Duración diferente al bloque configurado (P4=T)
    // Camino: P1=F, P2=F, P3=F, durationMinutes != slotMinutes -> P4=T
    // -----------------------------------------------------------------------

    /**
     * Ruta cubierta: P1->F, P2->F, P3->F, P4->TRUE.
     *   - slot configurado = 60 min.
     *   - request.start=09:00, request.end=10:30 -> duration = 90 min ≠ 60 -> P4=T.
     *
     * Condición evaluada: durationMinutes != slotMinutes  (90 != 60)
     * Mensaje esperado:   "La reserva debe ocupar exactamente un bloque de 60 minutos en este campus"
     */
    @Test
    @DisplayName("WS-05: Duración del bloque incorrecta — lanza BusinessException (P4: durationMinutes != slotMinutes)")
    void shouldFailWhenDurationDoesNotMatchSlotDuration() {
        try (MockedStatic<AppTime> appTime = mockStatic(AppTime.class)) {
            appTime.when(AppTime::nowDateTime).thenReturn(FIXED_NOW);
            appTime.when(AppTime::today).thenReturn(FIXED_NOW.toLocalDate());
            appTime.when(AppTime::nowTime).thenReturn(FIXED_NOW.toLocalTime());

            // slot=60 min, pero end-start = 90 min -> P4 verdadero
            when(roomScheduleService.getCampusSlotMinutes(any())).thenReturn(SLOT_MINUTES);

            // 09:00 a 10:30 = 90 minutos ≠ 60
            BookingUpsertRequest req = requestWithSlot(
                LocalTime.of(9, 0), LocalTime.of(10, 30)
            );

            BusinessException ex = assertThrows(
                BusinessException.class,
                () -> sut.validate(user, room, req, null)
            );
            assertEquals(
                "La reserva debe ocupar exactamente un bloque de 60 minutos en este campus",
                ex.getMessage(),
                "Debe rechazar la reserva cuando la duración no coincide con el bloque del campus"
            );
        }
    }

    // -----------------------------------------------------------------------
    // WS-06 — Sala cerrada en la fecha solicitada (P5=T, vía schedule.closed())
    // Camino: P1=F, P2=F, P3=F, P4=F, schedule.closed()==true -> P5=T
    // -----------------------------------------------------------------------

    /**
     * Ruta cubierta: P1->F, P2->F, P3->F, P4->F, P5->TRUE.
     *   - duration = 60 min = slot -> P4=F.
     *   - EffectiveSchedule devuelve closed=true -> P5=T.
     *
     * Condición evaluada: schedule.closed() == true
     * Mensaje esperado:   "La sala no atiende en esa fecha"
     */
    @Test
    @DisplayName("WS-06: Sala cerrada en la fecha solicitada — lanza BusinessException (P5: schedule.closed())")
    void shouldFailWhenRoomIsClosedOnRequestedDate() {
        try (MockedStatic<AppTime> appTime = mockStatic(AppTime.class)) {
            appTime.when(AppTime::nowDateTime).thenReturn(FIXED_NOW);
            appTime.when(AppTime::today).thenReturn(FIXED_NOW.toLocalDate());
            appTime.when(AppTime::nowTime).thenReturn(FIXED_NOW.toLocalTime());

            when(roomScheduleService.getCampusSlotMinutes(any())).thenReturn(SLOT_MINUTES);

            // La sala está marcada como cerrada ese día
            EffectiveSchedule closedSchedule = new EffectiveSchedule(
                VALID_DATE.getDayOfWeek().getValue(),
                null, null,
                true,   // closed = true  -> P5 verdadero
                false
            );
            when(roomScheduleService.getEffectiveScheduleForRoomDay(any(), any())).thenReturn(closedSchedule);

            BusinessException ex = assertThrows(
                BusinessException.class,
                () -> sut.validate(user, room, validRequest(), null)
            );
            assertEquals(
                "La sala no atiende en esa fecha",
                ex.getMessage(),
                "Debe rechazar la reserva cuando la sala está cerrada en la fecha solicitada"
            );
        }
    }

    // -----------------------------------------------------------------------
    // WS-07 — Horario fuera del rango de apertura/cierre (P6=T, vía start < openTime)
    // Camino: P1=F, P2=F, P3=F, P4=F, P5=F, start.isBefore(openTime) -> P6=T
    // -----------------------------------------------------------------------

    /**
     * Ruta cubierta: P1->F, P2->F, P3->F, P4->F, P5->F, P6->TRUE.
     *   - La sala abre a las 08:00.
     *   - request.start = 07:00 < 08:00 -> start.isBefore(openTime) -> P6=T.
     *
     * Condición evaluada: start.isBefore(schedule.openTime())   (07:00 < 08:00)
     * Mensaje esperado:   "El horario no está dentro del rango disponible de la sala"
     *
     * Nota: el slot 07:00-08:00 tiene 60 min (= slot configurado) y cumple todas
     *   las condiciones anteriores; solo falla por estar antes de la apertura.
     */
    @Test
    @DisplayName("WS-07: Reserva con hora de inicio anterior a la apertura — lanza BusinessException (P6: start fuera de rango)")
    void shouldFailWhenStartTimeIsBeforeRoomOpenTime() {
        // Fijamos "ahora" a 06:00 para que start=07:00 sea futuro (P3=F)
        LocalDateTime nowAt06 = VALID_DATE.atTime(LocalTime.of(6, 0));

        try (MockedStatic<AppTime> appTime = mockStatic(AppTime.class)) {
            appTime.when(AppTime::nowDateTime).thenReturn(nowAt06);
            appTime.when(AppTime::today).thenReturn(nowAt06.toLocalDate());
            appTime.when(AppTime::nowTime).thenReturn(nowAt06.toLocalTime());

            when(roomScheduleService.getCampusSlotMinutes(any())).thenReturn(SLOT_MINUTES);

            // La sala abre a las 08:00; el bloque 07:00-08:00 queda ANTES de la apertura
            EffectiveSchedule lateOpen = new EffectiveSchedule(
                VALID_DATE.getDayOfWeek().getValue(),
                LocalTime.of(8, 0),   // openTime = 08:00
                LocalTime.of(22, 0),
                false, false
            );
            when(roomScheduleService.getEffectiveScheduleForRoomDay(any(), any())).thenReturn(lateOpen);

            // start=07:00, end=08:00 -> 60 min = slot, pero start < openTime (08:00)
            BookingUpsertRequest req = requestWithSlot(LocalTime.of(7, 0), LocalTime.of(8, 0));

            BusinessException ex = assertThrows(
                BusinessException.class,
                () -> sut.validate(user, room, req, null)
            );
            assertEquals(
                "El horario no está dentro del rango disponible de la sala",
                ex.getMessage(),
                "Debe rechazar la reserva cuando el inicio está antes de la apertura de la sala"
            );
        }
    }

    // -----------------------------------------------------------------------
    // WS-08 — Horario no alineado con los bloques desde la apertura (P7=T)
    // Camino: P1=F, P2=F, P3=F, P4=F, P5=F, P6=F,
    //         fromOpenToStart % slotMinutes != 0 -> P7=T
    // -----------------------------------------------------------------------

    /**
     * Ruta cubierta: P1->F, P2->F, P3->F, P4->F, P5->F, P6->F, P7->TRUE.
     *   - La sala abre a las 07:00.
     *   - Slot configurado = 60 min.
     *   - request.start = 09:30 -> fromOpenToStart = 150 min -> 150 % 60 = 30 ≠ 0 -> P7=T.
     *   - request.end = 10:30 -> end <= closeTime(22:00) -> P6=F (no falla por rango).
     *   - duration = 60 min = slot -> P4=F.
     *
     * Condición evaluada: fromOpenToStart % slotMinutes != 0   (150 % 60 = 30)
     * Mensaje esperado:   "El horario debe alinearse con bloques de 60 minutos desde la apertura del día"
     */
    @Test
    @DisplayName("WS-08: Reserva no alineada con los bloques desde apertura — lanza BusinessException (P7: % slotMinutes != 0)")
    void shouldFailWhenSlotIsNotAlignedFromOpenTime() {
        try (MockedStatic<AppTime> appTime = mockStatic(AppTime.class)) {
            appTime.when(AppTime::nowDateTime).thenReturn(FIXED_NOW);
            appTime.when(AppTime::today).thenReturn(FIXED_NOW.toLocalDate());
            appTime.when(AppTime::nowTime).thenReturn(FIXED_NOW.toLocalTime());

            when(roomScheduleService.getCampusSlotMinutes(any())).thenReturn(SLOT_MINUTES);

            // Sala abierta de 07:00 a 22:00 — mismo rango que el base
            when(roomScheduleService.getEffectiveScheduleForRoomDay(any(), any())).thenReturn(openSchedule());

            // 09:30 a 10:30 = 60 min (P4=F), dentro del rango 07:00-22:00 (P6=F),
            // pero (09:30-07:00)=150 min -> 150%60=30 ≠ 0 -> P7=T
            BookingUpsertRequest req = requestWithSlot(
                LocalTime.of(9, 30), LocalTime.of(10, 30)
            );

            BusinessException ex = assertThrows(
                BusinessException.class,
                () -> sut.validate(user, room, req, null)
            );
            assertEquals(
                "El horario debe alinearse con bloques de 60 minutos desde la apertura del día",
                ex.getMessage(),
                "Debe rechazar la reserva cuando el inicio no está alineado con los bloques del campus"
            );
        }
    }

    // -----------------------------------------------------------------------
    // Utilidad estática
    // -----------------------------------------------------------------------

    /**
     * Devuelve el próximo lunes a las 07:00 (siempre en el futuro).
     * Si hoy ya es lunes, avanza una semana para garantizar que VALID_DATE
     * esté en el futuro respecto a LocalDate.now().
     */
    private static LocalDateTime nextMondayAt7() {
        LocalDate today = LocalDate.now();
        int daysUntilMonday = (DayOfWeek.MONDAY.getValue() - today.getDayOfWeek().getValue() + 7) % 7;
        LocalDate monday = today.plusDays(daysUntilMonday == 0 ? 7 : daysUntilMonday);
        return monday.atTime(LocalTime.of(7, 0));
    }
}
