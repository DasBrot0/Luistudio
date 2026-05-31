package com.luistudio.reservas.service.factory;

import com.luistudio.reservas.dto.room.MaintenanceRequest;
import com.luistudio.reservas.model.MaintenanceEntity;
import com.luistudio.reservas.model.MaintenanceStatus;
import com.luistudio.reservas.model.RoomEntity;
import org.springframework.stereotype.Component;

@Component
public class MaintenanceFactory {

    public MaintenanceEntity createScheduled(RoomEntity room, MaintenanceRequest request) {
        MaintenanceEntity maintenance = new MaintenanceEntity();
        maintenance.setSala(room);
        maintenance.setInicio(request.start());
        maintenance.setFin(request.end());
        maintenance.setMotivo(request.reason());
        maintenance.setEstado(MaintenanceStatus.PROGRAMADO);
        return maintenance;
    }
}
