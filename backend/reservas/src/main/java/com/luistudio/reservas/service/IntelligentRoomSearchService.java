package com.luistudio.reservas.service;

import com.luistudio.reservas.dto.room.IntelligentRoomSearchRequest;
import com.luistudio.reservas.dto.room.IntelligentRoomSearchResponse;
import com.luistudio.reservas.dto.room.RoomResponse;
import com.luistudio.reservas.dto.room.RoomSearchIntent;
import com.luistudio.reservas.model.RoomEntity;
import com.luistudio.reservas.model.RoomState;
import com.luistudio.reservas.model.RoomType;
import com.luistudio.reservas.repository.RoomRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IntelligentRoomSearchService {
    private final RoomIntentInterpreter intentInterpreter;
    private final RoomRepository roomRepository;
    private final RoomService roomService;
    private final DtoMapper dtoMapper;

    public IntelligentRoomSearchService(
        RoomIntentInterpreter intentInterpreter,
        RoomRepository roomRepository,
        RoomService roomService,
        DtoMapper dtoMapper
    ) {
        this.intentInterpreter = intentInterpreter;
        this.roomRepository = roomRepository;
        this.roomService = roomService;
        this.dtoMapper = dtoMapper;
    }

    @Transactional(readOnly = true)
    public IntelligentRoomSearchResponse search(IntelligentRoomSearchRequest request) {
        RoomSearchIntent intent = intentInterpreter.interpret(request.query().trim());
        int limit = request.limit() == null ? 3 : Math.max(1, Math.min(3, request.limit()));
        List<IntelligentRoomSearchResponse.Recommendation> recommendations = roomRepository.findByEstadoNot(RoomState.INACTIVA)
            .stream()
            .filter(room -> roomService.isRoomAvailable(room, request.date(), request.start(), request.end(), null))
            .filter(room -> isCompatible(room, intent))
            .map(room -> recommend(room, intent))
            .sorted(Comparator.comparingInt(IntelligentRoomSearchResponse.Recommendation::score).reversed()
                .thenComparing(item -> item.room().code()))
            .limit(limit)
            .toList();
        return new IntelligentRoomSearchResponse(intent, recommendations);
    }

    private boolean isCompatible(RoomEntity room, RoomSearchIntent intent) {
        boolean peopleFit = room.getCapacidad() >= intent.minimumCapacity()
            && room.getMaximoPersonas() >= intent.minimumCapacity()
            && (!Boolean.TRUE.equals(room.getMinimoPersonasObligatorio()) || room.getMinimoPersonas() <= intent.minimumCapacity());
        boolean noiseFit = room.getNivelRuido().satisfies(intent.maximumNoise());
        boolean concentrationFit = !intent.requiresConcentration() || Boolean.TRUE.equals(room.getPermiteConcentracion());
        return peopleFit && noiseFit && concentrationFit && hasRequiredEquipment(room, intent.requiredEquipment());
    }

    private boolean hasRequiredEquipment(RoomEntity room, Set<String> requiredEquipment) {
        Set<String> roomEquipment = room.getEquipamiento().stream().map(this::normalize).collect(java.util.stream.Collectors.toSet());
        return requiredEquipment.stream().map(this::normalize).allMatch(roomEquipment::contains);
    }

    private IntelligentRoomSearchResponse.Recommendation recommend(RoomEntity room, RoomSearchIntent intent) {
        List<String> reasons = new ArrayList<>(List.of("Disponible en el horario solicitado"));
        int score = 40;
        int surplus = room.getCapacidad() - intent.minimumCapacity();
        score += Math.max(0, 25 - surplus * 2);
        reasons.add("Capacidad para " + intent.minimumCapacity() + " persona" + (intent.minimumCapacity() == 1 ? "" : "s"));
        if (room.getNivelRuido() == intent.maximumNoise()) {
            score += 15;
            reasons.add("Nivel de ruido " + room.getNivelRuido().name().toLowerCase(Locale.ROOT));
        } else {
            score += 10;
        }
        if (intent.requiresConcentration()) {
            score += 15;
            reasons.add("Adecuada para concentración");
        }
        if (room.getTipo() == intent.roomType()) {
            score += 15;
            reasons.add("Tipo de espacio alineado con tu actividad");
        } else if (room.getTipo() == RoomType.GENERAL) {
            score += 5;
        }
        if (!intent.requiredEquipment().isEmpty()) {
            score += Math.min(10, intent.requiredEquipment().size() * 3);
            reasons.add("Incluye: " + String.join(", ", intent.requiredEquipment()));
        }
        RoomResponse response = dtoMapper.toRoom(room);
        return new IntelligentRoomSearchResponse.Recommendation(response, score, reasons);
    }

    private String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
