package com.luistudio.reservas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.luistudio.reservas.model.MaintenanceEntity;
import com.luistudio.reservas.model.MaintenanceStatus;
import com.luistudio.reservas.model.RoomEntity;
import com.luistudio.reservas.model.RoomState;
import com.luistudio.reservas.repository.MaintenanceRepository;
import com.luistudio.reservas.repository.RoomRepository;
import com.luistudio.reservas.util.AppTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoomMaintenanceServiceTest {

    @Mock MaintenanceRepository maintenanceRepository;
    @Mock RoomRepository roomRepository;

    @Test
    void finishesExpiredMaintenanceAndReleasesRoom() {
        RoomEntity room = room(RoomState.EN_MANTENIMIENTO);
        MaintenanceEntity maintenance = maintenance(room, MaintenanceStatus.EN_CURSO, -120, -60);
        when(maintenanceRepository.findByEstadoIn(anyList())).thenReturn(List.of(maintenance));

        new RoomMaintenanceService(maintenanceRepository, roomRepository).reconcileStatuses();

        assertThat(maintenance.getEstado()).isEqualTo(MaintenanceStatus.FINALIZADO);
        assertThat(room.getEstado()).isEqualTo(RoomState.DISPONIBLE);
        verify(maintenanceRepository).save(maintenance);
        verify(roomRepository).save(room);
    }

    @Test
    void activatesCurrentMaintenanceAndBlocksRoom() {
        RoomEntity room = room(RoomState.DISPONIBLE);
        MaintenanceEntity maintenance = maintenance(room, MaintenanceStatus.PROGRAMADO, -15, 60);
        when(maintenanceRepository.findByEstadoIn(anyList())).thenReturn(List.of(maintenance));

        new RoomMaintenanceService(maintenanceRepository, roomRepository).reconcileStatuses();

        assertThat(maintenance.getEstado()).isEqualTo(MaintenanceStatus.EN_CURSO);
        assertThat(room.getEstado()).isEqualTo(RoomState.EN_MANTENIMIENTO);
        verify(maintenanceRepository).save(maintenance);
        verify(roomRepository).save(room);
    }

    private RoomEntity room(RoomState state) {
        RoomEntity room = new RoomEntity();
        room.setId(10L);
        room.setEstado(state);
        return room;
    }

    private MaintenanceEntity maintenance(RoomEntity room, MaintenanceStatus status, long startMinutes, long endMinutes) {
        MaintenanceEntity maintenance = new MaintenanceEntity();
        maintenance.setId(20L);
        maintenance.setSala(room);
        maintenance.setEstado(status);
        maintenance.setInicio(AppTime.nowOffset().plusMinutes(startMinutes));
        maintenance.setFin(AppTime.nowOffset().plusMinutes(endMinutes));
        return maintenance;
    }
}
