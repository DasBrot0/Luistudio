package com.luistudio.reservas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import com.luistudio.reservas.dto.room.IntelligentRoomSearchRequest;
import com.luistudio.reservas.dto.room.RoomResponse;
import com.luistudio.reservas.dto.room.RoomSearchIntent;
import com.luistudio.reservas.dto.room.RoomSearchAnalysis;
import com.luistudio.reservas.dto.room.RoomSearchCandidate;
import com.luistudio.reservas.model.RoomEntity;
import com.luistudio.reservas.model.RoomNoiseLevel;
import com.luistudio.reservas.model.RoomState;
import com.luistudio.reservas.model.RoomType;
import com.luistudio.reservas.model.CampusEntity;
import com.luistudio.reservas.model.PabellonEntity;
import com.luistudio.reservas.repository.RoomRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IntelligentRoomSearchServiceTest {
    @Mock RoomIntentInterpreter interpreter;
    @Mock RoomRepository roomRepository;
    @Mock RoomService roomService;
    @Mock RoomScheduleService roomScheduleService;
    @Mock DtoMapper dtoMapper;
    private IntelligentRoomSearchService service;

    @BeforeEach
    void setUp() {
        service = new IntelligentRoomSearchService(interpreter, roomRepository, roomService, roomScheduleService, dtoMapper);
    }

    @Test
    void returnsAtMostThreeCompatibleRoomsInDeterministicOrder() {
        RoomSearchIntent intent = new RoomSearchIntent(RoomType.ESTUDIO_GRUPAL, 4, RoomNoiseLevel.MEDIO, true, Set.of("pizarra"));
        List<RoomEntity> rooms = List.of(room(1L, "B-205", 8), room(2L, "A-101", 4), room(3L, "C-301", 6));
        when(roomRepository.findByEstadoNot(RoomState.INACTIVA)).thenReturn(rooms);
        when(roomScheduleService.getCampusSlotMinutes(any())).thenReturn(60);
        when(roomScheduleService.getEffectiveScheduleForRoomDay(any(), any())).thenReturn(
            new RoomScheduleService.EffectiveSchedule(1, LocalTime.of(6, 0), LocalTime.of(22, 0), false, false)
        );
        when(roomService.isRoomAvailable(any(), any(), any(), any(), isNull())).thenReturn(true);
        when(interpreter.interpret(any(), any())).thenReturn(new RoomSearchAnalysis(intent, List.of(
            new RoomSearchAnalysis.CandidateMatch(1L, 4, "Adecuada para estudiar"),
            new RoomSearchAnalysis.CandidateMatch(2L, 20, "Coincidencia exacta con la necesidad"),
            new RoomSearchAnalysis.CandidateMatch(3L, 8, "Apta para el grupo")
        )));
        for (RoomEntity room : rooms) when(dtoMapper.toRoom(room)).thenReturn(response(room));

        var result = service.search(new IntelligentRoomSearchRequest(
            "sala silenciosa para cuatro con pizarra", LocalDate.of(2026, 7, 13),
            LocalTime.of(10, 0), LocalTime.of(11, 0), 3
        ));

        assertThat(result.intent()).isEqualTo(intent);
        assertThat(result.recommendations()).extracting(item -> item.room().code())
            .containsExactly("A-101", "C-301", "B-205");
        assertThat(result.recommendations()).isSortedAccordingTo(
            java.util.Comparator.comparingInt(com.luistudio.reservas.dto.room.IntelligentRoomSearchResponse.Recommendation::score).reversed()
                .thenComparing(item -> item.room().code())
        );
    }

    @Test
    void explainsWhenRequestedDurationDoesNotMatchConfiguredBlocks() {
        RoomSearchIntent intent = new RoomSearchIntent(RoomType.GENERAL, 1, RoomNoiseLevel.ALTO, false, Set.of());
        RoomEntity room = room(1L, "A-101", 4);
        when(roomRepository.findByEstadoNot(RoomState.INACTIVA)).thenReturn(List.of(room));
        when(roomScheduleService.getCampusSlotMinutes(any())).thenReturn(60);
        when(interpreter.interpret(any(), any())).thenReturn(new RoomSearchAnalysis(intent, List.of()));

        var result = service.search(new IntelligentRoomSearchRequest(
            "necesito estudiar", LocalDate.of(2026, 7, 15),
            LocalTime.of(8, 0), LocalTime.of(21, 0), 3
        ));

        assertThat(result.recommendations()).isEmpty();
        assertThat(result.message()).contains("780 minutos", "60 minutos");
    }

    @Test
    void excludesRoomsThatViolateAnExplicitNegativeConstraint() {
        RoomSearchIntent intent = new RoomSearchIntent(RoomType.ESTUDIO_GRUPAL, 1, RoomNoiseLevel.MEDIO, true, Set.of());
        RoomEntity buildingM = locatedRoom(1L, "M-201", "Edificio M", -12.0852400, -76.9714300);
        RoomEntity buildingF1 = locatedRoom(2L, "F1-201", "Pabellón F1", -12.0841048, -76.9712867);
        prepareAvailableRooms(List.of(buildingM, buildingF1));
        when(interpreter.interpret(any(), any())).thenReturn(new RoomSearchAnalysis(intent, List.of(
            new RoomSearchAnalysis.CandidateMatch(1L, 0, "El usuario excluyó el edificio M", true),
            new RoomSearchAnalysis.CandidateMatch(2L, 15, "Alternativa fuera del edificio M", false)
        )));
        when(dtoMapper.toRoom(buildingF1)).thenReturn(response(buildingF1));

        var result = service.search(new IntelligentRoomSearchRequest(
            "necesito estudiar, pero no quiero en el edificio M", LocalDate.of(2026, 7, 15),
            LocalTime.of(10, 0), LocalTime.of(11, 0), 3
        ));

        assertThat(result.recommendations()).extracting(item -> item.room().code()).containsExactly("F1-201");
    }

    @Test
    void ranksFartherRoomsUsingCoordinatesWhenRequested() {
        RoomSearchIntent intent = new RoomSearchIntent(RoomType.ESTUDIO_GRUPAL, 1, RoomNoiseLevel.MEDIO, true, Set.of());
        RoomEntity buildingM = locatedRoom(1L, "M-201", "Edificio M", -12.0852400, -76.9714300);
        RoomEntity buildingF1 = locatedRoom(2L, "F1-201", "Pabellón F1", -12.0841048, -76.9712867);
        RoomEntity mayorazgo = locatedRoom(3L, "CDM-ESTUDIO", "Centro Deportivo Mayorazgo", -12.0596826, -76.9421069);
        List<RoomEntity> rooms = List.of(buildingM, buildingF1, mayorazgo);
        prepareAvailableRooms(rooms);
        when(interpreter.interpret(any(), any())).thenReturn(new RoomSearchAnalysis(
            intent,
            rooms.stream().map(room -> new RoomSearchAnalysis.CandidateMatch(room.getId(), 5, "Compatible")).toList(),
            new RoomSearchAnalysis.ProximityPreference(RoomSearchAnalysis.ProximityMode.FAR, 1L)
        ));
        for (RoomEntity room : rooms) when(dtoMapper.toRoom(room)).thenReturn(response(room));

        var result = service.search(new IntelligentRoomSearchRequest(
            "quiero estudiar lo más lejos posible del edificio M", LocalDate.of(2026, 7, 15),
            LocalTime.of(10, 0), LocalTime.of(11, 0), 3
        ));

        assertThat(result.recommendations().getFirst().room().code()).isEqualTo("CDM-ESTUDIO");
        assertThat(result.recommendations().getFirst().reasons()).anyMatch(reason -> reason.contains("priorizamos lejanía"));
    }

    @Test
    void usesAnUnavailableActiveRoomAsSpatialReferenceButNeverRecommendsIt() {
        RoomSearchIntent intent = new RoomSearchIntent(RoomType.ESTUDIO_GRUPAL, 1, RoomNoiseLevel.MEDIO, true, Set.of());
        RoomEntity unavailableBuildingM = locatedRoom(1L, "M-201", "Edificio M", -12.0852400, -76.9714300);
        RoomEntity buildingF1 = locatedRoom(2L, "F1-201", "Pabellón F1", -12.0841048, -76.9712867);
        RoomEntity mayorazgo = locatedRoom(3L, "CDM-ESTUDIO", "Centro Deportivo Mayorazgo", -12.0596826, -76.9421069);
        List<RoomEntity> activeRooms = List.of(unavailableBuildingM, buildingF1, mayorazgo);
        when(roomRepository.findByEstadoNot(RoomState.INACTIVA)).thenReturn(activeRooms);
        when(roomScheduleService.getCampusSlotMinutes(any())).thenReturn(60);
        when(roomScheduleService.getEffectiveScheduleForRoomDay(any(), any())).thenReturn(
            new RoomScheduleService.EffectiveSchedule(3, LocalTime.of(6, 0), LocalTime.of(22, 0), false, false)
        );
        when(roomService.isRoomAvailable(any(), any(), any(), any(), isNull()))
            .thenAnswer(invocation -> !((RoomEntity) invocation.getArgument(0)).getId().equals(1L));
        when(interpreter.interpret(any(), any())).thenReturn(new RoomSearchAnalysis(
            intent,
            activeRooms.stream().map(room -> new RoomSearchAnalysis.CandidateMatch(room.getId(), 5, "Compatible")).toList(),
            new RoomSearchAnalysis.ProximityPreference(RoomSearchAnalysis.ProximityMode.FAR, 1L)
        ));
        when(dtoMapper.toRoom(buildingF1)).thenReturn(response(buildingF1));
        when(dtoMapper.toRoom(mayorazgo)).thenReturn(response(mayorazgo));

        var result = service.search(new IntelligentRoomSearchRequest(
            "quiero estudiar lejos del edificio M", LocalDate.of(2026, 7, 15),
            LocalTime.of(10, 0), LocalTime.of(11, 0), 3
        ));

        ArgumentCaptor<List<RoomSearchCandidate>> catalogCaptor = ArgumentCaptor.forClass(List.class);
        org.mockito.Mockito.verify(interpreter).interpret(any(), catalogCaptor.capture());
        assertThat(catalogCaptor.getValue()).extracting(RoomSearchCandidate::id).containsExactly(1L, 2L, 3L);
        assertThat(result.recommendations()).extracting(item -> item.room().code()).doesNotContain("M-201");
        assertThat(result.recommendations().getFirst().reasons()).anyMatch(reason -> reason.contains("de Edificio M"));
    }

    private void prepareAvailableRooms(List<RoomEntity> rooms) {
        when(roomRepository.findByEstadoNot(RoomState.INACTIVA)).thenReturn(rooms);
        when(roomScheduleService.getCampusSlotMinutes(any())).thenReturn(60);
        when(roomScheduleService.getEffectiveScheduleForRoomDay(any(), any())).thenReturn(
            new RoomScheduleService.EffectiveSchedule(3, LocalTime.of(6, 0), LocalTime.of(22, 0), false, false)
        );
        when(roomService.isRoomAvailable(any(), any(), any(), any(), isNull())).thenReturn(true);
    }

    private RoomEntity locatedRoom(Long id, String code, String venue, double latitude, double longitude) {
        RoomEntity room = room(id, code, 6);
        CampusEntity campus = new CampusEntity();
        campus.setNombre(code.startsWith("CDM") ? "Mayorazgo" : "Monterrico");
        PabellonEntity pabellon = new PabellonEntity();
        pabellon.setNombre(venue);
        pabellon.setCodigo(venue);
        pabellon.setCampus(campus);
        pabellon.setLatitude(BigDecimal.valueOf(latitude));
        pabellon.setLongitude(BigDecimal.valueOf(longitude));
        room.setPabellon(pabellon);
        return room;
    }

    private RoomEntity room(Long id, String code, int capacity) {
        RoomEntity room = new RoomEntity();
        room.setId(id);
        room.setCodigo(code);
        room.setNombre(code);
        room.setCapacidad(capacity);
        room.setMaximoPersonas(capacity);
        room.setMinimoPersonas(1);
        room.setMinimoPersonasObligatorio(false);
        room.setNivelRuido(RoomNoiseLevel.BAJO);
        room.setPermiteConcentracion(true);
        room.setTipo(RoomType.ESTUDIO_GRUPAL);
        room.setEquipamiento(new LinkedHashSet<>(Set.of("pizarra")));
        room.setActividadesPermitidas(new LinkedHashSet<>(Set.of("estudio grupal")));
        room.setServiciosCercanos(new LinkedHashSet<>(Set.of("comedor cercano")));
        room.setCaracteristicasAccesibilidad(new LinkedHashSet<>());
        room.setEstado(RoomState.DISPONIBLE);
        return room;
    }

    private RoomResponse response(RoomEntity room) {
        return new RoomResponse(room.getId(), room.getCodigo(), room.getNombre(), room.getCodigo(), null, null, null, null,
            room.getCapacidad(), null, 1, false, room.getMaximoPersonas(), 60, List.of(), "DISPONIBLE", null,
            room.getNivelRuido().name(), true, room.getTipo().name(), room.getEquipamiento());
    }
}
