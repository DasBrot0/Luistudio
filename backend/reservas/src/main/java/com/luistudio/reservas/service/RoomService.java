package com.luistudio.reservas.service;

import com.luistudio.reservas.dto.room.MaintenanceRequest;
import com.luistudio.reservas.dto.room.MaintenanceResponse;
import com.luistudio.reservas.dto.room.RoomResponse;
import com.luistudio.reservas.dto.room.RoomUpsertRequest;
import com.luistudio.reservas.exception.BusinessException;
import com.luistudio.reservas.exception.NotFoundException;
import com.luistudio.reservas.model.MaintenanceEntity;
import com.luistudio.reservas.model.MaintenanceStatus;
import com.luistudio.reservas.model.PabellonEntity;
import com.luistudio.reservas.model.ReservationStatus;
import com.luistudio.reservas.model.RoomEntity;
import com.luistudio.reservas.model.RoomState;
import com.luistudio.reservas.repository.MaintenanceRepository;
import com.luistudio.reservas.repository.PabellonRepository;
import com.luistudio.reservas.repository.ReservationRepository;
import com.luistudio.reservas.repository.RoomRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomService {

    private final RoomRepository roomRepository;
    private final PabellonRepository pabellonRepository;
    private final ReservationRepository reservationRepository;
    private final MaintenanceRepository maintenanceRepository;
    private final DtoMapper dtoMapper;

    public RoomService(
        RoomRepository roomRepository,
        PabellonRepository pabellonRepository,
        ReservationRepository reservationRepository,
        MaintenanceRepository maintenanceRepository,
        DtoMapper dtoMapper
    ) {
        this.roomRepository = roomRepository;
        this.pabellonRepository = pabellonRepository;
        this.reservationRepository = reservationRepository;
        this.maintenanceRepository = maintenanceRepository;
        this.dtoMapper = dtoMapper;
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> listRooms(String location) {
        List<RoomEntity> rooms = (location == null || location.isBlank())
            ? roomRepository.findByEstadoNot(RoomState.INACTIVA)
            : roomRepository.findByUbicacionIgnoreCaseAndEstadoNot(location, RoomState.INACTIVA);
        return rooms.stream().map(dtoMapper::toRoom).toList();
    }

    @Transactional(readOnly = true)
    public List<RoomResponse> listAvailableRooms(LocalDate date, LocalTime start, LocalTime end) {
        List<RoomEntity> rooms = roomRepository.findByEstadoNot(RoomState.INACTIVA);
        return rooms.stream().filter(room -> isRoomAvailable(room, date, start, end, null)).map(dtoMapper::toRoom).toList();
    }

    @Transactional
    public RoomResponse createRoom(RoomUpsertRequest request) {
        if (request.name().isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "El nombre de sala es obligatorio");
        }

        PabellonEntity pabellon = resolvePabellon(request.pabellonCode(), request.location());

        RoomEntity room = new RoomEntity();
        room.setNombre(request.name().trim());
        room.setCapacidad(request.capacity());
        room.setUbicacion(request.location().trim());
        room.setEstado(RoomState.DISPONIBLE);
        room.setPabellon(pabellon);
        room.setCodigo(generateRoomCode(request.location()));
        return dtoMapper.toRoom(roomRepository.save(room));
    }

    @Transactional
    public RoomResponse updateRoom(Long roomId, RoomUpsertRequest request) {
        RoomEntity room = getRoomEntity(roomId);
        room.setNombre(request.name().trim());
        room.setCapacidad(request.capacity());
        room.setUbicacion(request.location().trim());
        room.setPabellon(resolvePabellon(request.pabellonCode(), request.location()));
        return dtoMapper.toRoom(roomRepository.save(room));
    }

    @Transactional
    public void deleteRoom(Long roomId) {
        RoomEntity room = getRoomEntity(roomId);
        boolean hasFuture = reservationRepository.existsFutureActiveReservations(room, LocalDate.now(), LocalTime.now());
        if (hasFuture) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "No se puede eliminar: la sala tiene reservas futuras activas");
        }
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

        MaintenanceEntity maintenance = new MaintenanceEntity();
        maintenance.setSala(room);
        maintenance.setInicio(request.start());
        maintenance.setFin(request.end());
        maintenance.setMotivo(request.reason());
        maintenance.setEstado(MaintenanceStatus.PROGRAMADO);
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

        OffsetDateTime from = date.atTime(start).atOffset(OffsetDateTime.now().getOffset());
        OffsetDateTime to = date.atTime(end).atOffset(OffsetDateTime.now().getOffset());

        boolean overlapsBooking = reservationRepository.existsOverlapping(room, date, start, end, excludeBookingId);
        boolean overlapsMaintenance = !maintenanceRepository.findOverlapping(room, from, to).isEmpty();

        return !overlapsBooking && !overlapsMaintenance;
    }

    private PabellonEntity resolvePabellon(String requestedCode, String location) {
        String pabellonCode = (requestedCode == null || requestedCode.isBlank()) ? location : requestedCode;
        return pabellonRepository.findByCodigo(pabellonCode).orElseGet(() -> {
            PabellonEntity pabellon = new PabellonEntity();
            pabellon.setCodigo(pabellonCode);
            pabellon.setNombre("Pabellon " + pabellonCode);
            pabellon.setUbicacion(location);
            return pabellonRepository.save(pabellon);
        });
    }

    private String generateRoomCode(String location) {
        long seed = System.currentTimeMillis() % 10000;
        String prefix = (location == null || location.isBlank()) ? "R" : location.replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        return prefix + "-" + String.format("%04d", seed);
    }
}
