package com.luistudio.reservas.service.booking.command;

import com.luistudio.reservas.service.RoomMaintenanceService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RoomMaintenanceScheduler {

    private final RoomMaintenanceService maintenanceService;

    public RoomMaintenanceScheduler(RoomMaintenanceService maintenanceService) {
        this.maintenanceService = maintenanceService;
    }

    @Scheduled(fixedDelay = 7000)
    public void run() {
        maintenanceService.reconcileStatuses();
    }
}
