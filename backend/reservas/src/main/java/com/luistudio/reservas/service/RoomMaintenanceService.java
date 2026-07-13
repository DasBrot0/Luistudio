package com.luistudio.reservas.service;

import com.luistudio.reservas.model.MaintenanceStatus;
import com.luistudio.reservas.model.RoomState;
import com.luistudio.reservas.repository.MaintenanceRepository;
import com.luistudio.reservas.repository.RoomRepository;
import com.luistudio.reservas.util.AppTime;
import java.time.OffsetDateTime;
import java.util.EnumSet;
import java.util.HashSet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RoomMaintenanceService {

    private final MaintenanceRepository maintenanceRepository;
    private final RoomRepository roomRepository;

    public RoomMaintenanceService(MaintenanceRepository maintenanceRepository, RoomRepository roomRepository) {
        this.maintenanceRepository = maintenanceRepository;
        this.roomRepository = roomRepository;
    }

    @Transactional
    public void reconcileStatuses() {
        OffsetDateTime now = AppTime.nowOffset();
        var candidates = maintenanceRepository.findByEstadoIn(
            EnumSet.of(MaintenanceStatus.PROGRAMADO, MaintenanceStatus.EN_CURSO).stream().toList()
        );
        var activeRoomIds = new HashSet<Long>();

        for (var maintenance : candidates) {
            if (!maintenance.getInicio().isAfter(now) && maintenance.getFin().isAfter(now)) {
                maintenance.setEstado(MaintenanceStatus.EN_CURSO);
                activeRoomIds.add(maintenance.getSala().getId());
                if (maintenance.getSala().getEstado() != RoomState.INACTIVA) {
                    maintenance.getSala().setEstado(RoomState.EN_MANTENIMIENTO);
                    roomRepository.save(maintenance.getSala());
                }
            } else if (!maintenance.getFin().isAfter(now)) {
                maintenance.setEstado(MaintenanceStatus.FINALIZADO);
            } else {
                maintenance.setEstado(MaintenanceStatus.PROGRAMADO);
            }
            maintenanceRepository.save(maintenance);
        }

        candidates.stream()
            .map(item -> item.getSala())
            .filter(room -> room.getEstado() == RoomState.EN_MANTENIMIENTO)
            .filter(room -> !activeRoomIds.contains(room.getId()))
            .distinct()
            .forEach(room -> {
                room.setEstado(RoomState.DISPONIBLE);
                roomRepository.save(room);
            });
    }
}
