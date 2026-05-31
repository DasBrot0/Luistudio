package com.luistudio.reservas.service;

import com.luistudio.reservas.dto.admin.CampusMapResponse;
import com.luistudio.reservas.model.MaintenanceEntity;
import com.luistudio.reservas.model.PabellonEntity;
import com.luistudio.reservas.model.ReservationEntity;
import com.luistudio.reservas.model.RoomEntity;
import com.luistudio.reservas.repository.MaintenanceRepository;
import com.luistudio.reservas.repository.PabellonRepository;
import com.luistudio.reservas.repository.ReservationRepository;
import com.luistudio.reservas.repository.RoomRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CampusMapService {

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
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        List<ReservationEntity> activeNow = reservationRepository.findActiveAt(today, now);
        List<MaintenanceEntity> activeMaintenances = maintenanceRepository.findActiveAt(OffsetDateTime.now());

        List<PabellonEntity> pabellones = pabellonRepository.findAll();

        List<CampusMapResponse.PabellonMapItem> mapped = pabellones.stream().map(p -> {
            List<CampusMapResponse.RoomMapItem> rooms = roomRepository.findByPabellonAndEstadoNot(
                p,
                com.luistudio.reservas.model.RoomState.INACTIVA
            ).stream().map(room -> {
                String status = resolveStatus(room, activeNow, activeMaintenances);
                return new CampusMapResponse.RoomMapItem(
                    room.getId(),
                    room.getCodigo(),
                    room.getNombre(),
                    status,
                    room.getUbicacion(),
                    room.getCapacidad()
                );
            }).toList();

            return new CampusMapResponse.PabellonMapItem(p.getCodigo(), p.getNombre(), rooms);
        }).toList();

        return new CampusMapResponse(mapped);
    }

    private String resolveStatus(RoomEntity room, List<ReservationEntity> activeNow, List<MaintenanceEntity> activeMaintenances) {
        boolean inMaintenance = activeMaintenances.stream().anyMatch(m -> m.getSala().getId().equals(room.getId()));
        if (inMaintenance) {
            return "mantenimiento";
        }

        boolean occupied = activeNow.stream().anyMatch(r -> r.getSala().getId().equals(room.getId()));
        return occupied ? "ocupada" : "libre";
    }
}
