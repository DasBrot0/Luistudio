package com.luistudio.reservas.service;

import com.luistudio.reservas.dto.admin.CampusMapResponse;
import com.luistudio.reservas.model.MaintenanceEntity;
import com.luistudio.reservas.model.PabellonEntity;
import com.luistudio.reservas.model.ReservationEntity;
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
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CampusMapService {
    private static final Logger log = LoggerFactory.getLogger(CampusMapService.class);

    private final PabellonRepository pabellonRepository;
    private final RoomRepository roomRepository;
    private final ReservationRepository reservationRepository;
    private final MaintenanceRepository maintenanceRepository;

    public CampusMapService(
        PabellonRepository pabellonRepository,
        RoomRepository roomRepository,
        ReservationRepository reservationRepository,
        MaintenanceRepository maintenanceRepository
    ) {
        this.pabellonRepository = pabellonRepository;
        this.roomRepository = roomRepository;
        this.reservationRepository = reservationRepository;
        this.maintenanceRepository = maintenanceRepository;
    }

    @Transactional(readOnly = true)
    public CampusMapResponse getCampusMap() {
        long startedAt = System.currentTimeMillis();
        LocalDate today = AppTime.today();
        LocalTime now = AppTime.nowTime();

        List<ReservationEntity> activeNow = reservationRepository.findActiveAt(today, now);
        List<MaintenanceEntity> activeMaintenances = maintenanceRepository.findActiveAt(OffsetDateTime.now());

        List<PabellonEntity> pabellones = pabellonRepository.findAll();
        List<RoomEntity> rooms = roomRepository.findByEstadoNot(RoomState.INACTIVA);
        Set<Long> occupiedRoomIds = activeNow.stream()
            .map(reservation -> reservation.getSala().getId())
            .collect(Collectors.toSet());
        Set<Long> maintenanceRoomIds = activeMaintenances.stream()
            .map(maintenance -> maintenance.getSala().getId())
            .collect(Collectors.toSet());
        Map<Long, List<RoomEntity>> roomsByBuilding = rooms.stream()
            .filter(room -> room.getPabellon() != null)
            .collect(Collectors.groupingBy(room -> room.getPabellon().getId()));

        List<CampusMapResponse.PabellonMapItem> mapped = pabellones.stream().map(p -> {
            List<CampusMapResponse.RoomMapItem> buildingRooms = roomsByBuilding.getOrDefault(p.getId(), List.of()).stream().map(room -> {
                String status = resolveStatus(room, occupiedRoomIds, maintenanceRoomIds);
                return new CampusMapResponse.RoomMapItem(
                    room.getId(),
                    room.getCodigo(),
                    room.getNombre(),
                    status,
                    room.getUbicacion(),
                    room.getCapacidad()
                );
            }).toList();

            return new CampusMapResponse.PabellonMapItem(p.getCodigo(), p.getNombre(), buildingRooms);
        }).toList();

        log.info(
            "campus_map_built rooms={} activeReservations={} activeMaintenances={} durationMs={}",
            rooms.size(),
            activeNow.size(),
            activeMaintenances.size(),
            System.currentTimeMillis() - startedAt
        );
        return new CampusMapResponse(mapped);
    }

    private String resolveStatus(RoomEntity room, Set<Long> occupiedRoomIds, Set<Long> maintenanceRoomIds) {
        if (maintenanceRoomIds.contains(room.getId())) {
            return "mantenimiento";
        }

        return occupiedRoomIds.contains(room.getId()) ? "ocupada" : "libre";
    }
}
