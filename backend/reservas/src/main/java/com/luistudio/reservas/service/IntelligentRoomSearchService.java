package com.luistudio.reservas.service;

import com.luistudio.reservas.dto.room.IntelligentRoomSearchRequest;
import com.luistudio.reservas.dto.room.IntelligentRoomSearchResponse;
import com.luistudio.reservas.dto.room.RoomResponse;
import com.luistudio.reservas.dto.room.RoomSearchIntent;
import com.luistudio.reservas.dto.room.RoomSearchAnalysis;
import com.luistudio.reservas.dto.room.RoomSearchCandidate;
import com.luistudio.reservas.model.RoomEntity;
import com.luistudio.reservas.model.RoomState;
import com.luistudio.reservas.model.RoomType;
import com.luistudio.reservas.repository.RoomRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.time.Duration;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IntelligentRoomSearchService {
    private final RoomIntentInterpreter intentInterpreter;
    private final RoomRepository roomRepository;
    private final RoomService roomService;
    private final RoomScheduleService roomScheduleService;
    private final DtoMapper dtoMapper;

    public IntelligentRoomSearchService(
        RoomIntentInterpreter intentInterpreter,
        RoomRepository roomRepository,
        RoomService roomService,
        RoomScheduleService roomScheduleService,
        DtoMapper dtoMapper
    ) {
        this.intentInterpreter = intentInterpreter;
        this.roomRepository = roomRepository;
        this.roomService = roomService;
        this.roomScheduleService = roomScheduleService;
        this.dtoMapper = dtoMapper;
    }

    @Transactional(readOnly = true)
    public IntelligentRoomSearchResponse search(IntelligentRoomSearchRequest request) {
        int limit = request.limit() == null ? 3 : Math.max(1, Math.min(3, request.limit()));
        List<RoomEntity> activeRooms = roomRepository.findByEstadoNot(RoomState.INACTIVA);
        int requestedMinutes = (int) Duration.between(request.start(), request.end()).toMinutes();
        List<RoomEntity> durationCompatibleRooms = activeRooms.stream()
            .filter(room -> roomScheduleService.getCampusSlotMinutes(room.getCampus()) == requestedMinutes)
            .toList();
        List<RoomEntity> alignedRooms = durationCompatibleRooms.stream()
            .filter(room -> isAlignedWithReservableSlot(room, request))
            .toList();
        List<RoomEntity> availableRooms = alignedRooms.stream()
            .filter(room -> roomService.isRoomAvailable(room, request.date(), request.start(), request.end(), null))
            .toList();
        RoomSearchAnalysis analysis = intentInterpreter.interpret(
            request.query().trim(),
            activeRooms.stream().map(this::toCandidate).toList()
        );
        RoomSearchIntent intent = analysis.intent();
        Set<Long> candidateIds = availableRooms.stream().map(RoomEntity::getId).collect(Collectors.toSet());
        Map<Long, RoomSearchAnalysis.CandidateMatch> matchesByRoom = analysis.candidateMatches().stream()
            .filter(match -> candidateIds.contains(match.roomId()))
            .collect(Collectors.toMap(
                RoomSearchAnalysis.CandidateMatch::roomId,
                Function.identity(),
                (first, second) -> first.relevanceScore() >= second.relevanceScore() ? first : second
            ));
        Set<Long> excludedRoomIds = matchesByRoom.values().stream()
            .filter(RoomSearchAnalysis.CandidateMatch::excluded)
            .map(RoomSearchAnalysis.CandidateMatch::roomId)
            .collect(Collectors.toSet());
        Map<Long, ProximityAdjustment> proximityByRoom = proximityAdjustments(
            availableRooms,
            activeRooms,
            analysis.proximityPreference()
        );
        List<IntelligentRoomSearchResponse.Recommendation> recommendations = availableRooms.stream()
            .filter(room -> !excludedRoomIds.contains(room.getId()))
            .filter(room -> isCompatible(room, intent))
            .map(room -> recommend(room, intent, matchesByRoom.get(room.getId()), proximityByRoom.get(room.getId())))
            .sorted(Comparator.comparingInt(IntelligentRoomSearchResponse.Recommendation::score).reversed()
                .thenComparing(item -> item.room().code()))
            .limit(limit)
            .toList();
        return new IntelligentRoomSearchResponse(
            intent,
            recommendations,
            recommendations.isEmpty() ? emptyResultMessage(requestedMinutes, activeRooms, durationCompatibleRooms, alignedRooms, availableRooms) : null
        );
    }

    private boolean isAlignedWithReservableSlot(RoomEntity room, IntelligentRoomSearchRequest request) {
        int slotMinutes = roomScheduleService.getCampusSlotMinutes(room.getCampus());
        RoomScheduleService.EffectiveSchedule schedule = roomScheduleService.getEffectiveScheduleForRoomDay(room, request.date());
        if (schedule.closed() || schedule.openTime() == null || schedule.closeTime() == null
            || request.start().isBefore(schedule.openTime()) || request.end().isAfter(schedule.closeTime())) {
            return false;
        }
        long fromOpening = Duration.between(schedule.openTime(), request.start()).toMinutes();
        return fromOpening >= 0 && fromOpening % slotMinutes == 0;
    }

    private String emptyResultMessage(
        int requestedMinutes,
        List<RoomEntity> activeRooms,
        List<RoomEntity> durationCompatibleRooms,
        List<RoomEntity> alignedRooms,
        List<RoomEntity> availableRooms
    ) {
        Set<Integer> configuredDurations = activeRooms.stream()
            .map(room -> roomScheduleService.getCampusSlotMinutes(room.getCampus()))
            .collect(Collectors.toCollection(java.util.TreeSet::new));
        if (durationCompatibleRooms.isEmpty()) {
            return "La duración solicitada es de " + requestedMinutes + " minutos. Elige un bloque de "
                + configuredDurations.stream().map(value -> value + " minutos").collect(Collectors.joining(" o ")) + ".";
        }
        if (alignedRooms.isEmpty()) {
            return "El horario debe comenzar en un bloque válido y terminar dentro del horario de atención de la sala.";
        }
        if (availableRooms.isEmpty()) {
            return "No hay salas libres durante ese bloque. Prueba otra hora disponible.";
        }
        return "Hay salas disponibles, pero ninguna cumple todos los requisitos indicados.";
    }

    private RoomSearchCandidate toCandidate(RoomEntity room) {
        return new RoomSearchCandidate(
            room.getId(), room.getCodigo(), room.getNombre(), room.getDescripcion(), room.getCampus(), room.getVenue(),
            room.getUbicacion(), coordinate(room, true), coordinate(room, false), room.getCapacidad(),
            room.getNivelRuido().name(), room.getPermiteConcentracion(),
            room.getTipo().name(), Set.copyOf(room.getEquipamiento()), Set.copyOf(room.getActividadesPermitidas()),
            Set.copyOf(room.getServiciosCercanos()), Set.copyOf(room.getCaracteristicasAccesibilidad())
        );
    }

    private Double coordinate(RoomEntity room, boolean latitude) {
        if (room.getPabellon() == null) {
            return null;
        }
        java.math.BigDecimal value = latitude ? room.getPabellon().getLatitude() : room.getPabellon().getLongitude();
        return value == null ? null : value.doubleValue();
    }

    private Map<Long, ProximityAdjustment> proximityAdjustments(
        List<RoomEntity> targetRooms,
        List<RoomEntity> catalogRooms,
        RoomSearchAnalysis.ProximityPreference preference
    ) {
        if (preference == null || preference.mode() == RoomSearchAnalysis.ProximityMode.NONE
            || preference.referenceRoomId() == null || preference.referenceRoomId() <= 0) {
            return Map.of();
        }
        RoomEntity reference = catalogRooms.stream()
            .filter(room -> room.getId().equals(preference.referenceRoomId()))
            .findFirst()
            .orElse(null);
        Double referenceLatitude = reference == null ? null : coordinate(reference, true);
        Double referenceLongitude = reference == null ? null : coordinate(reference, false);
        if (referenceLatitude == null || referenceLongitude == null) {
            return Map.of();
        }

        Map<Long, Double> distances = new HashMap<>();
        for (RoomEntity room : targetRooms) {
            Double latitude = coordinate(room, true);
            Double longitude = coordinate(room, false);
            if (latitude != null && longitude != null) {
                distances.put(room.getId(), haversineKm(referenceLatitude, referenceLongitude, latitude, longitude));
            }
        }
        double maxDistance = distances.values().stream().mapToDouble(Double::doubleValue).max().orElse(0);
        Map<Long, ProximityAdjustment> result = new HashMap<>();
        for (Map.Entry<Long, Double> entry : distances.entrySet()) {
            double ratio = maxDistance == 0 ? 0.5 : entry.getValue() / maxDistance;
            double preferredRatio = preference.mode() == RoomSearchAnalysis.ProximityMode.NEAR ? 1 - ratio : ratio;
            int score = (int) Math.round(preferredRatio * 20);
            String direction = preference.mode() == RoomSearchAnalysis.ProximityMode.NEAR ? "cercanía" : "lejanía";
            result.put(entry.getKey(), new ProximityAdjustment(
                score,
                String.format(Locale.ROOT, "A %.2f km de %s; priorizamos %s", entry.getValue(), reference.getVenue(), direction)
            ));
        }
        return result;
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        double earthRadiusKm = 6371.0088;
        double latitudeDelta = Math.toRadians(lat2 - lat1);
        double longitudeDelta = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latitudeDelta / 2) * Math.sin(latitudeDelta / 2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
            * Math.sin(longitudeDelta / 2) * Math.sin(longitudeDelta / 2);
        return earthRadiusKm * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
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

    private IntelligentRoomSearchResponse.Recommendation recommend(
        RoomEntity room,
        RoomSearchIntent intent,
        RoomSearchAnalysis.CandidateMatch semanticMatch,
        ProximityAdjustment proximityAdjustment
    ) {
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
        if (semanticMatch != null && semanticMatch.relevanceScore() > 0) {
            score += semanticMatch.relevanceScore();
            if (!semanticMatch.reason().isBlank()) {
                reasons.add(semanticMatch.reason());
            }
        }
        if (proximityAdjustment != null && proximityAdjustment.score() > 0) {
            score += proximityAdjustment.score();
            reasons.add(proximityAdjustment.reason());
        }
        RoomResponse response = dtoMapper.toRoom(room);
        return new IntelligentRoomSearchResponse.Recommendation(response, score, reasons);
    }

    private String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private record ProximityAdjustment(int score, String reason) {
    }
}
