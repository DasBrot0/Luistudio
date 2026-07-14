package com.luistudio.reservas.service;

import com.luistudio.reservas.dto.room.RoomResponse;
import com.luistudio.reservas.dto.common.PageResponse;
import com.luistudio.reservas.dto.room.RoomUpsertRequest;
import com.luistudio.reservas.exception.BusinessException;
import com.luistudio.reservas.exception.NotFoundException;
import com.luistudio.reservas.model.CampusEntity;
import com.luistudio.reservas.model.PabellonEntity;
import com.luistudio.reservas.model.RoomEntity;
import com.luistudio.reservas.model.RoomNoiseLevel;
import com.luistudio.reservas.model.RoomState;
import com.luistudio.reservas.model.RoomType;
import com.luistudio.reservas.repository.MaintenanceRepository;
import com.luistudio.reservas.repository.PabellonRepository;
import com.luistudio.reservas.repository.CampusRepository;
import com.luistudio.reservas.repository.ReservationRepository;
import com.luistudio.reservas.repository.RoomRepository;
import com.luistudio.reservas.util.AppTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomService {
    private static final Logger log = LoggerFactory.getLogger(RoomService.class);
    private static final String ROOM_HAS_ACTIVE_RESERVATION_MESSAGE =
        "En esta sala hay reserva activa, así que no se puede eliminar";

    private final RoomRepository roomRepository;
    private final PabellonRepository pabellonRepository;
    private final CampusRepository campusRepository;
    private final ReservationRepository reservationRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final DtoMapper dtoMapper;
    private final RoomScheduleService roomScheduleService;

    public RoomService(
        RoomRepository roomRepository,
        PabellonRepository pabellonRepository,
        CampusRepository campusRepository,
        ReservationRepository reservationRepository,
        MaintenanceRepository maintenanceRepository,
        DtoMapper dtoMapper,
        RoomScheduleService roomScheduleService
    ) {
        this.roomRepository = roomRepository;
        this.pabellonRepository = pabellonRepository;
        this.campusRepository = campusRepository;
        this.reservationRepository = reservationRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.dtoMapper = dtoMapper;
        this.roomScheduleService = roomScheduleService;
    }

    @Transactional(readOnly = true)
    public PageResponse<RoomResponse> listRooms(
        int page,
        int size,
        boolean includeSchedule,
        String campus,
        String venue,
        String location,
        String query
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        PageRequest pageRequest = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.ASC, "codigo"));
        Page<RoomEntity> roomPage = roomRepository.searchActiveRooms(
            campus,
            venue,
            location,
            query,
            RoomState.INACTIVA,
            pageRequest
        );

        List<RoomResponse> items = roomPage.getContent().stream().map(room -> toRoomResponse(room, includeSchedule)).toList();
        log.info(
            "rooms_listed page={} size={} includeSchedule={} returned={} total={}",
            safePage,
            safeSize,
            includeSchedule,
            items.size(),
            roomPage.getTotalElements()
        );
        return new PageResponse<>(
            items,
            roomPage.getNumber(),
            roomPage.getSize(),
            roomPage.getTotalElements(),
            roomPage.getTotalPages()
        );
    }

    @Transactional
    public RoomResponse createRoom(RoomUpsertRequest request) {
        if (request.name().isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "El nombre de sala es obligatorio");
        }
        validatePeopleConstraints(request.capacity(), request.minPeople(), request.minPeopleRequired(), request.maxPeople());

        PabellonEntity pabellon = resolvePabellon(request.pabellonCode(), request.campus());
        RoomEntity room = buildAvailableRoom(
            request.name(),
            request.capacity(),
            resolveMinPeople(request.minPeople()),
            request.minPeopleRequired(),
            request.maxPeople(),
            request.location(),
            pabellon,
            generateRoomCode(request.campus(), request.location()),
            request.noiseLevel(),
            request.supportsConcentration(),
            request.roomType(),
            request.equipment(),
            request.description(),
            request.allowedActivities(),
            request.nearbyServices(),
            request.accessibilityFeatures()
        );
        RoomEntity saved = roomRepository.save(room);
        roomScheduleService.saveRoomSchedule(saved, request.schedule());
        log.info("room_created roomId={} roomCode={} status={}", saved.getId(), saved.getCodigo(), saved.getEstado());
        return toRoomResponse(saved, true);
    }

    @Transactional
    public RoomResponse updateRoom(Long roomId, RoomUpsertRequest request) {
        RoomEntity room = getRoomEntity(roomId);
        validatePeopleConstraints(request.capacity(), request.minPeople(), request.minPeopleRequired(), request.maxPeople());
        room.setNombre(request.name().trim());
        room.setCapacidad(request.capacity());
        room.setUbicacion(request.location().trim());
        room.setMinimoPersonas(resolveMinPeople(request.minPeople()));
        room.setMinimoPersonasObligatorio(request.minPeopleRequired());
        room.setMaximoPersonas(request.maxPeople());
        room.setPabellon(resolvePabellon(request.pabellonCode(), request.campus()));
        if (request.noiseLevel() != null) {
            room.setNivelRuido(request.noiseLevel());
        }
        if (request.supportsConcentration() != null) {
            room.setPermiteConcentracion(request.supportsConcentration());
        }
        if (request.roomType() != null) {
            room.setTipo(request.roomType());
        }
        if (request.equipment() != null) {
            room.setEquipamiento(normalizeEquipment(request.equipment()));
        }
        if (request.description() != null) {
            room.setDescripcion(normalizeDescription(request.description()));
        }
        if (request.allowedActivities() != null) {
            room.setActividadesPermitidas(normalizeTags(request.allowedActivities()));
        }
        if (request.nearbyServices() != null) {
            room.setServiciosCercanos(normalizeTags(request.nearbyServices()));
        }
        if (request.accessibilityFeatures() != null) {
            room.setCaracteristicasAccesibilidad(normalizeTags(request.accessibilityFeatures()));
        }
        RoomState requestedStatus = request.status() == null ? room.getEstado() : request.status();
        if (requestedStatus == RoomState.INACTIVA) {
            ensureRoomCanBeInactivated(room);
        }
        room.setEstado(requestedStatus);
        RoomEntity saved = roomRepository.save(room);
        roomScheduleService.saveRoomSchedule(saved, request.schedule());
        log.info("room_updated roomId={} roomCode={} status={}", saved.getId(), saved.getCodigo(), saved.getEstado());
        return toRoomResponse(saved, true);
    }

    @Transactional
    public void deleteRoom(Long roomId) {
        RoomEntity room = getRoomEntity(roomId);
        ensureRoomCanBeInactivated(room);
        room.setEstado(RoomState.INACTIVA);
        roomRepository.save(room);
        log.info("room_deleted roomId={} roomCode={} status={}", room.getId(), room.getCodigo(), room.getEstado());
    }

    @Transactional(readOnly = true)
    public RoomEntity getRoomEntity(Long roomId) {
        return roomRepository.findById(roomId).orElseThrow(() -> new NotFoundException("Sala no encontrada"));
    }

    public void lockRoomInventory(Long roomId) {
        roomRepository.findByIdForUpdate(roomId).orElseThrow(() -> new NotFoundException("Sala no encontrada"));
    }

    @Transactional(readOnly = true)
    public boolean isRoomAvailable(RoomEntity room, LocalDate date, LocalTime start, LocalTime end, Long excludeBookingId) {
        if (room.getEstado() == RoomState.INACTIVA) {
            return false;
        }
        if (!roomScheduleService.isTimeWithinSchedule(room, date, start, end)) {
            return false;
        }

        OffsetDateTime from = date.atTime(start).atZone(AppTime.ZONE).toOffsetDateTime();
        OffsetDateTime to = date.atTime(end).atZone(AppTime.ZONE).toOffsetDateTime();

        long overlappingBookings = reservationRepository.countOverlapping(room, date, start, end, excludeBookingId);
        int inventoryCount = room.getCantidadUnidades() == null ? 1 : room.getCantidadUnidades();
        boolean overlapsMaintenance = !maintenanceRepository.findOverlapping(room, from, to).isEmpty();

        return overlappingBookings < inventoryCount && !overlapsMaintenance;
    }

    private PabellonEntity resolvePabellon(String requestedCode, String campusName) {
        String pabellonCode = requestedCode == null || requestedCode.isBlank() ? null : requestedCode.trim();
        if (pabellonCode == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "El código de pabellón es obligatorio");
        }
        PabellonEntity existing = pabellonRepository.findByCodigo(pabellonCode).orElse(null);
        if (existing != null) {
            if (!existing.getCampus().getNombre().equalsIgnoreCase(campusName.trim())) {
                throw new BusinessException(HttpStatus.BAD_REQUEST, "El pabellón no pertenece al campus indicado");
            }
            return existing;
        }
        return pabellonRepository.findByCodigo(pabellonCode).orElseGet(() -> {
            CampusEntity campus = campusRepository.findByNombreIgnoreCase(campusName.trim()).orElseThrow(
                () -> new BusinessException(HttpStatus.BAD_REQUEST, "Campus no encontrado")
            );
            PabellonEntity pabellon = new PabellonEntity();
            pabellon.setCodigo(pabellonCode);
            pabellon.setNombre(pabellonCode);
            pabellon.setCampus(campus);
            return pabellonRepository.save(pabellon);
        });
    }

    private String generateRoomCode(String campus, String location) {
        long seed = System.currentTimeMillis() % 10000;
        String base = (campus == null ? "" : campus) + "-" + (location == null ? "" : location);
        String prefix = base.isBlank() ? "R" : base.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        int maxPrefixLength = 15; // rooms.code tiene límite VARCHAR(20) y usamos "-NNNN"
        if (prefix.length() > maxPrefixLength) {
            prefix = prefix.substring(0, maxPrefixLength);
        }
        return prefix + "-" + String.format("%04d", seed);
    }

    private RoomResponse toRoomResponse(RoomEntity room, boolean includeSchedule) {
        RoomResponse base = dtoMapper.toRoom(room);
        int slotMinutes = includeSchedule ? roomScheduleService.getCampusSlotMinutes(room.getCampus()) : 0;
        return new RoomResponse(
            base.id(),
            base.code(),
            base.name(),
            base.resourceLabel(),
            base.campus(),
            base.campusLabel(),
            base.venue(),
            base.venueLabel(),
            base.capacity(),
            base.location(),
            base.minPeople(),
            base.minPeopleRequired(),
            base.maxPeople(),
            slotMinutes,
            includeSchedule ? roomScheduleService.getEffectiveWeeklySchedule(room) : List.of(),
            base.status(),
            base.pabellonCode(),
            base.noiseLevel(),
            base.supportsConcentration(),
            base.roomType(),
            base.equipment(),
            base.inventoryCount(),
            base.description(),
            base.allowedActivities(),
            base.nearbyServices(),
            base.accessibilityFeatures()
        );
    }

    private int resolveMinPeople(Integer minPeople) {
        return minPeople == null ? 1 : minPeople;
    }

    private RoomEntity buildAvailableRoom(
        String name,
        Integer capacity,
        Integer minPeople,
        Boolean minPeopleRequired,
        Integer maxPeople,
        String location,
        PabellonEntity pabellon,
        String roomCode,
        RoomNoiseLevel noiseLevel,
        Boolean supportsConcentration,
        RoomType roomType,
        Set<String> equipment,
        String description,
        Set<String> allowedActivities,
        Set<String> nearbyServices,
        Set<String> accessibilityFeatures
    ) {
        RoomEntity room = new RoomEntity();
        room.setNombre(name.trim());
        room.setCapacidad(capacity);
        room.setMinimoPersonas(minPeople);
        room.setMinimoPersonasObligatorio(minPeopleRequired);
        room.setMaximoPersonas(maxPeople);
        room.setUbicacion(location.trim());
        room.setEstado(RoomState.DISPONIBLE);
        room.setPabellon(pabellon);
        room.setCodigo(roomCode);
        room.setNivelRuido(noiseLevel == null ? RoomNoiseLevel.MEDIO : noiseLevel);
        room.setPermiteConcentracion(Boolean.TRUE.equals(supportsConcentration));
        room.setTipo(roomType == null ? RoomType.GENERAL : roomType);
        room.setEquipamiento(normalizeEquipment(equipment));
        room.setDescripcion(normalizeDescription(description));
        room.setActividadesPermitidas(normalizeTags(allowedActivities));
        room.setServiciosCercanos(normalizeTags(nearbyServices));
        room.setCaracteristicasAccesibilidad(normalizeTags(accessibilityFeatures));
        return room;
    }

    private Set<String> normalizeEquipment(Set<String> equipment) {
        if (equipment == null) {
            return new LinkedHashSet<>();
        }
        return equipment.stream()
            .filter(item -> item != null && !item.isBlank())
            .map(String::trim)
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> normalizeTags(Set<String> values) {
        if (values == null) {
            return new LinkedHashSet<>();
        }
        return values.stream()
            .filter(item -> item != null && !item.isBlank())
            .map(String::trim)
            .map(item -> item.toLowerCase(java.util.Locale.ROOT))
            .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private String normalizeDescription(String description) {
        return description == null || description.isBlank() ? null : description.trim();
    }

    private void ensureRoomCanBeInactivated(RoomEntity room) {
        boolean hasFuture = reservationRepository.existsFutureActiveReservations(room, AppTime.today(), AppTime.nowTime());
        if (hasFuture) {
            log.warn("room_inactivation_blocked roomId={} roomCode={}", room.getId(), room.getCodigo());
            throw new BusinessException(HttpStatus.BAD_REQUEST, ROOM_HAS_ACTIVE_RESERVATION_MESSAGE);
        }
    }

    private void validatePeopleConstraints(int capacity, Integer minPeople, boolean minRequired, int maxPeople) {
        int normalizedMin = minPeople == null ? 1 : minPeople;
        if (normalizedMin <= 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "El mínimo de personas debe ser mayor que cero");
        }
        if (maxPeople <= 0) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "El máximo de personas debe ser mayor que cero");
        }
        if (maxPeople > capacity) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "El máximo de personas no puede superar la capacidad de la sala");
        }
        if (normalizedMin > maxPeople) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "El mínimo de personas no puede superar el máximo");
        }
        if (minRequired && normalizedMin < 1) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "El mínimo obligatorio de personas es inválido");
        }
    }
}
