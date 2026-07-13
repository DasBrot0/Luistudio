package com.luistudio.reservas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import com.luistudio.reservas.dto.room.IntelligentRoomSearchRequest;
import com.luistudio.reservas.dto.room.RoomResponse;
import com.luistudio.reservas.dto.room.RoomSearchIntent;
import com.luistudio.reservas.model.RoomEntity;
import com.luistudio.reservas.model.RoomNoiseLevel;
import com.luistudio.reservas.model.RoomState;
import com.luistudio.reservas.model.RoomType;
import com.luistudio.reservas.repository.RoomRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class IntelligentRoomSearchServiceTest {
    @Mock RoomIntentInterpreter interpreter;
    @Mock RoomRepository roomRepository;
    @Mock RoomService roomService;
    @Mock DtoMapper dtoMapper;
    private IntelligentRoomSearchService service;

    @BeforeEach
    void setUp() {
        service = new IntelligentRoomSearchService(interpreter, roomRepository, roomService, dtoMapper);
    }

    @Test
    void returnsAtMostThreeCompatibleRoomsInDeterministicOrder() {
        RoomSearchIntent intent = new RoomSearchIntent(RoomType.ESTUDIO_GRUPAL, 4, RoomNoiseLevel.MEDIO, true, Set.of("pizarra"));
        List<RoomEntity> rooms = List.of(room(1L, "B-205", 8), room(2L, "A-101", 4), room(3L, "C-301", 6));
        when(interpreter.interpret(any())).thenReturn(intent);
        when(roomRepository.findByEstadoNot(RoomState.INACTIVA)).thenReturn(rooms);
        when(roomService.isRoomAvailable(any(), any(), any(), any(), isNull())).thenReturn(true);
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
        room.setEstado(RoomState.DISPONIBLE);
        return room;
    }

    private RoomResponse response(RoomEntity room) {
        return new RoomResponse(room.getId(), room.getCodigo(), room.getNombre(), room.getCodigo(), null, null, null, null,
            room.getCapacidad(), null, 1, false, room.getMaximoPersonas(), 60, List.of(), "DISPONIBLE", null,
            room.getNivelRuido().name(), true, room.getTipo().name(), room.getEquipamiento());
    }
}
