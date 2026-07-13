package com.luistudio.reservas.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luistudio.reservas.dto.admin.CampusMapResponse;
import com.luistudio.reservas.model.*;
import com.luistudio.reservas.repository.*;
import com.luistudio.reservas.util.AppTime;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CampusMapService {
    private static final Logger log = LoggerFactory.getLogger(CampusMapService.class);
    private static final BigDecimal MON_LAT = new BigDecimal("-12.0848128");
    private static final BigDecimal MON_LON = new BigDecimal("-76.9716488");
    private final PabellonRepository buildings;
    private final RoomRepository rooms;
    private final ReservationRepository reservations;
    private final MaintenanceRepository maintenances;
    private final RoomScheduleRepository schedules;
    private final StringRedisTemplate redis;
    private final ObjectMapper json;
    private final int ttl;
    private final int refresh;

    public CampusMapService(PabellonRepository buildings, RoomRepository rooms, ReservationRepository reservations,
        MaintenanceRepository maintenances, RoomScheduleRepository schedules, StringRedisTemplate redis,
        ObjectMapper json, @Value("${app.campus-map.cache-ttl-seconds:10}") int ttl,
        @Value("${app.campus-map.refresh-after-seconds:15}") int refresh) {
        this.buildings = buildings; this.rooms = rooms; this.reservations = reservations;
        this.maintenances = maintenances; this.schedules = schedules; this.redis = redis; this.json = json;
        this.ttl = ttl; this.refresh = refresh;
    }

    @Transactional(readOnly = true)
    public CampusMapResponse getCampusMap(String campus) {
        String normalized = campus == null || campus.isBlank() ? null : campus.trim();
        String key = "campus-map:v1:" + (normalized == null ? "all" : normalized);
        CampusMapResponse cached = readCache(key);
        if (cached != null) return cached;
        CampusMapResponse response = build(normalized);
        writeCache(key, response);
        return response;
    }

    public void evictAll() {
        try {
            Set<String> keys = redis.keys("campus-map:v1:*");
            if (keys != null && !keys.isEmpty()) redis.delete(keys);
        } catch (RuntimeException ex) { log.warn("campus_map_cache_evict_failed fallback=postgres", ex); }
    }

    private CampusMapResponse build(String campusFilter) {
        var now = AppTime.nowOffset();
        List<PabellonEntity> enabled = buildings.findMapEnabledOrdered().stream()
            .filter(b -> campusFilter == null || campusFilter.equalsIgnoreCase(b.getCampus().getNombre())).toList();
        Set<Long> buildingIds = enabled.stream().map(PabellonEntity::getId).collect(Collectors.toSet());
        List<RoomEntity> activeRooms = rooms.findByEstadoNot(RoomState.INACTIVA).stream()
            .filter(r -> r.getPabellon() != null && buildingIds.contains(r.getPabellon().getId())).toList();
        Set<Long> occupied = reservations.findActiveAt(AppTime.today(), AppTime.nowTime()).stream()
            .map(r -> r.getSala().getId()).collect(Collectors.toSet());
        Set<Long> maintained = maintenances.findActiveAt(now).stream()
            .map(m -> m.getSala().getId()).collect(Collectors.toSet());
        Map<Long, List<RoomScheduleEntity>> scheduleByRoom = activeRooms.isEmpty() ? Map.of() :
            schedules.findBySalaIdIn(activeRooms.stream().map(RoomEntity::getId).toList()).stream()
                .collect(Collectors.groupingBy(s -> s.getSala().getId()));
        Map<Long, List<RoomEntity>> byBuilding = activeRooms.stream().collect(Collectors.groupingBy(r -> r.getPabellon().getId()));

        Map<String, List<PabellonEntity>> byCampus = enabled.stream().collect(Collectors.groupingBy(
            b -> b.getCampus().getNombre(), TreeMap::new, Collectors.toList()));
        List<CampusMapResponse.Campus> campusItems = byCampus.entrySet().stream().map(entry -> {
            List<CampusMapResponse.Pavilion> pavilions = entry.getValue().stream().map(b -> mapBuilding(
                b, byBuilding.getOrDefault(b.getId(), List.of()), occupied, maintained, scheduleByRoom)).toList();
            BigDecimal lat = average(entry.getValue(), PabellonEntity::getLatitude, entry.getKey().equalsIgnoreCase("Monterrico") ? MON_LAT : null);
            BigDecimal lon = average(entry.getValue(), PabellonEntity::getLongitude, entry.getKey().equalsIgnoreCase("Monterrico") ? MON_LON : null);
            return new CampusMapResponse.Campus(code(entry.getKey()), entry.getKey(), new CampusMapResponse.Coordinate(lat, lon), 16.5, pavilions);
        }).toList();
        return new CampusMapResponse(now, refresh, campusItems);
    }

    private CampusMapResponse.Pavilion mapBuilding(PabellonEntity b, List<RoomEntity> buildingRooms, Set<Long> occupied,
        Set<Long> maintained, Map<Long, List<RoomScheduleEntity>> schedules) {
        List<RoomView> views = buildingRooms.stream().map(room -> {
            String status = (room.getEstado() == RoomState.EN_MANTENIMIENTO || maintained.contains(room.getId())) ? "MANTENIMIENTO"
                : occupied.contains(room.getId()) ? "OCUPADA" : "LIBRE";
            boolean within = withinSchedule(schedules.getOrDefault(room.getId(), List.of()));
            return new RoomView(room, status, within);
        }).sorted(Comparator.comparing(v -> v.room.getCodigo())).toList();
        long free = views.stream().filter(v -> v.status.equals("LIBRE")).count();
        long occupiedCount = views.stream().filter(v -> v.status.equals("OCUPADA")).count();
        long maintenance = views.stream().filter(v -> v.status.equals("MANTENIMIENTO")).count();
        String aggregate = views.isEmpty() ? "SIN_ESPACIOS" : free > 0 ? "CON_DISPONIBILIDAD"
            : maintenance == views.size() ? "MANTENIMIENTO_TOTAL" : "SIN_DISPONIBILIDAD";
        List<CampusMapResponse.Location> locations = views.stream().collect(Collectors.groupingBy(
            v -> v.room.getUbicacion() == null || v.room.getUbicacion().isBlank() ? "Sin ubicación" : v.room.getUbicacion(), TreeMap::new, Collectors.toList()))
            .entrySet().stream().map(e -> new CampusMapResponse.Location(e.getKey(), e.getValue().stream().map(v ->
                new CampusMapResponse.Room(v.room.getId(), v.room.getCodigo(), v.room.getNombre(), v.room.getVenue(),
                    v.room.getCapacidad(), v.status, v.within, v.within && v.status.equals("LIBRE"))).toList())).toList();
        return new CampusMapResponse.Pavilion(b.getId(), b.getCodigo(), b.getNombre(), b.getLatitude(), b.getLongitude(),
            aggregate, new CampusMapResponse.Summary(free, occupiedCount, maintenance, views.size()), locations);
    }

    private boolean withinSchedule(List<RoomScheduleEntity> values) {
        if (values.isEmpty()) return true;
        int day = DayOfWeek.from(AppTime.today()).getValue();
        LocalTime time = AppTime.nowTime();
        return values.stream().filter(s -> s.getDiaSemana() == day).findFirst()
            .map(s -> !Boolean.TRUE.equals(s.getCerrado()) && s.getHoraApertura() != null && s.getHoraCierre() != null
                && !time.isBefore(s.getHoraApertura()) && time.isBefore(s.getHoraCierre())).orElse(false);
    }

    private BigDecimal average(List<PabellonEntity> items, Function<PabellonEntity, BigDecimal> fn, BigDecimal fallback) {
        List<BigDecimal> values = items.stream().map(fn).filter(Objects::nonNull).toList();
        if (values.isEmpty()) return fallback;
        return values.stream().reduce(BigDecimal.ZERO, BigDecimal::add).divide(BigDecimal.valueOf(values.size()), 7, java.math.RoundingMode.HALF_UP);
    }
    private String code(String value) { return value.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]+", "_"); }
    private CampusMapResponse readCache(String key) { try { String value = redis.opsForValue().get(key); return value == null ? null : json.readValue(value, CampusMapResponse.class); } catch (RuntimeException | JsonProcessingException ex) { log.warn("campus_map_cache_read_failed key={} fallback=postgres", key); return null; } }
    private void writeCache(String key, CampusMapResponse value) { try { redis.opsForValue().set(key, json.writeValueAsString(value), Duration.ofSeconds(ttl)); } catch (RuntimeException | JsonProcessingException ex) { log.warn("campus_map_cache_write_failed key={} fallback=postgres", key); } }
    private record RoomView(RoomEntity room, String status, boolean within) {}
}
