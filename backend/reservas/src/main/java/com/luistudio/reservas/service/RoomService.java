package com.luistudio.reservas.service;

import com.luistudio.reservas.dto.room.MaintenanceRequest;
import com.luistudio.reservas.dto.room.MaintenanceResponse;
import com.luistudio.reservas.dto.room.RoomResponse;
import com.luistudio.reservas.dto.common.PageResponse;
import com.luistudio.reservas.dto.room.RoomUpsertRequest;
import com.luistudio.reservas.exception.BusinessException;
import com.luistudio.reservas.exception.NotFoundException;
import com.luistudio.reservas.model.MaintenanceEntity;
import com.luistudio.reservas.model.MaintenanceStatus;
import com.luistudio.reservas.model.PabellonEntity;
import com.luistudio.reservas.model.RoomEntity;
import com.luistudio.reservas.model.RoomState;
import com.luistudio.reservas.repository.MaintenanceRepository;
import com.luistudio.reservas.repository.PabellonRepository;
import com.luistudio.reservas.repository.ReservationRepository;
import com.luistudio.reservas.repository.RoomRepository;
import com.luistudio.reservas.util.AppTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomService {
    private static final String ROOM_HAS_ACTIVE_RESERVATION_MESSAGE =
        "En esta sala hay reserva activa, así que no se puede eliminar";

    private final RoomRepository roomRepository;
    private final PabellonRepository pabellonRepository;
    private final ReservationRepository reservationRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final DtoMapper dtoMapper;
    private final RoomScheduleService roomScheduleService;

    public RoomService(
        RoomRepository roomRepository,
        PabellonRepository pabellonRepository,
        ReservationRepository reservationRepository,
        MaintenanceRepository maintenanceRepository,
        DtoMapper dtoMapper,
        RoomScheduleService roomScheduleService
    ) {
        this.roomRepository = roomRepository;
        this.pabellonRepository = pabellonRepository;
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

        return new PageResponse<>(
            roomPage.getContent().stream().map(room -> toRoomResponse(room, includeSchedule)).toList(),
            roomPage.getNumber(),
            roomPage.getSize(),
            roomPage.getTotalElements(),
            roomPage.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> listAvailableRooms(LocalDate date, LocalTime start, LocalTime end) {
        List<RoomEntity> rooms = roomRepository.findByEstadoNot(RoomState.INACTIVA);
        return rooms.stream().filter(room -> isRoomAvailable(room, date, start, end, null)).map(room -> toRoomResponse(room, true)).toList();
    }

    @Transactional
    public RoomResponse createRoom(RoomUpsertRequest request) {
        if (request.name().isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "El nombre de sala es obligatorio");
        }
        validatePeopleConstraints(request.capacity(), request.minPeople(), request.minPeopleRequired(), request.maxPeople());

        PabellonEntity pabellon = resolvePabellon(request.pabellonCode(), request.location());
        RoomEntity room = buildAvailableRoom(
            request.name(),
            request.campus(),
            request.location(),
            request.capacity(),
            resolveMinPeople(request.minPeople()),
            request.minPeopleRequired(),
            request.maxPeople(),
            request.location(),
            pabellon,
            generateRoomCode(request.campus(), request.location())
        );
        RoomEntity saved = roomRepository.save(room);
        roomScheduleService.saveRoomSchedule(saved, request.schedule());
        return toRoomResponse(saved, true);
    }

    @Transactional
    public RoomResponse updateRoom(Long roomId, RoomUpsertRequest request) {
        RoomEntity room = getRoomEntity(roomId);
        validatePeopleConstraints(request.capacity(), request.minPeople(), request.minPeopleRequired(), request.maxPeople());
        room.setNombre(request.name().trim());
        room.setCampus(request.campus().trim());
        room.setVenue(request.location().trim());
        room.setCapacidad(request.capacity());
        room.setUbicacion(request.location().trim());
        room.setMinimoPersonas(resolveMinPeople(request.minPeople()));
        room.setMinimoPersonasObligatorio(request.minPeopleRequired());
        room.setMaximoPersonas(request.maxPeople());
        room.setPabellon(resolvePabellon(request.pabellonCode(), request.location()));
        RoomState requestedStatus = request.status() == null ? room.getEstado() : request.status();
        if (requestedStatus == RoomState.INACTIVA) {
            ensureRoomCanBeInactivated(room);
        }
        room.setEstado(requestedStatus);
        RoomEntity saved = roomRepository.save(room);
        roomScheduleService.saveRoomSchedule(saved, request.schedule());
        return toRoomResponse(saved, true);
    }

    @Transactional
    public void deleteRoom(Long roomId) {
        RoomEntity room = getRoomEntity(roomId);
        ensureRoomCanBeInactivated(room);
        room.setEstado(RoomState.INACTIVA);
        roomRepository.save(room);
    }

    @Transactional(readOnly = true)
    public RoomEntity getRoomEntity(Long roomId) {
        return roomRepository.findById(roomId).orElseThrow(() -> new NotFoundException("Sala no encontrada"));
    }

    @Transactional
    public MaintenanceResponse createMaintenance(Long roomId, MaintenanceRequest request) {
        RoomEntity room = getRoomEntity(roomId);
        if (!request.end().isAfter(request.start())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "La fecha fin debe ser mayor que inicio");
        }

        MaintenanceEntity maintenance = buildScheduledMaintenance(room, request);
        room.setEstado(RoomState.EN_MANTENIMIENTO);
        roomRepository.save(room);

        return dtoMapper.toMaintenance(maintenanceRepository.save(maintenance));
    }

    @Transactional(readOnly = true)
    public List<MaintenanceResponse> getRoomUnavailability(Long roomId) {
        RoomEntity room = getRoomEntity(roomId);
        return maintenanceRepository.findBySalaOrderByInicioDesc(room).stream().map(dtoMapper::toMaintenance).toList();
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

        boolean overlapsBooking = reservationRepository.existsOverlapping(room, date, start, end, excludeBookingId);
        boolean overlapsMaintenance = !maintenanceRepository.findOverlapping(room, from, to).isEmpty();

        return !overlapsBooking && !overlapsMaintenance;
    }

    private PabellonEntity resolvePabellon(String requestedCode, String location) {
        String pabellonCode = (requestedCode == null || requestedCode.isBlank()) ? location : requestedCode;
        return pabellonRepository.findByCodigo(pabellonCode).orElseGet(() -> {
            PabellonEntity pabellon = new PabellonEntity();
            pabellon.setCodigo(pabellonCode);
            pabellon.setNombre(pabellonCode);
            pabellon.setUbicacion(location);
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
            base.pabellonCode()
        );
    }

    private int resolveMinPeople(Integer minPeople) {
        return minPeople == null ? 1 : minPeople;
    }

    private RoomEntity buildAvailableRoom(
        String name,
        String campus,
        String venue,
        Integer capacity,
        Integer minPeople,
        Boolean minPeopleRequired,
        Integer maxPeople,
        String location,
        PabellonEntity pabellon,
        String roomCode
    ) {
        RoomEntity room = new RoomEntity();
        room.setNombre(name.trim());
        room.setCampus(campus.trim());
        room.setVenue(venue.trim());
        room.setCapacidad(capacity);
        room.setMinimoPersonas(minPeople);
        room.setMinimoPersonasObligatorio(minPeopleRequired);
        room.setMaximoPersonas(maxPeople);
        room.setUbicacion(location.trim());
        room.setEstado(RoomState.DISPONIBLE);
        room.setPabellon(pabellon);
        room.setCodigo(roomCode);
        return room;
    }

    private MaintenanceEntity buildScheduledMaintenance(RoomEntity room, MaintenanceRequest request) {
        MaintenanceEntity maintenance = new MaintenanceEntity();
        maintenance.setSala(room);
        maintenance.setInicio(request.start());
        maintenance.setFin(request.end());
        maintenance.setMotivo(request.reason());
        maintenance.setEstado(MaintenanceStatus.PROGRAMADO);
        return maintenance;
    }

    private void ensureRoomCanBeInactivated(RoomEntity room) {
        boolean hasFuture = reservationRepository.existsFutureActiveReservations(room, AppTime.today(), AppTime.nowTime());
        if (hasFuture) {
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
