package com.luistudio.reservas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import com.luistudio.reservas.model.RoomEntity;
import com.luistudio.reservas.model.RoomState;
import com.luistudio.reservas.repository.CampusRepository;
import com.luistudio.reservas.repository.MaintenanceRepository;
import com.luistudio.reservas.repository.PabellonRepository;
import com.luistudio.reservas.repository.ReservationRepository;
import com.luistudio.reservas.repository.RoomRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RoomServiceInventoryTest {

    @Mock private RoomRepository roomRepository;
    @Mock private PabellonRepository pabellonRepository;
    @Mock private CampusRepository campusRepository;
    @Mock private ReservationRepository reservationRepository;
    @Mock private MaintenanceRepository maintenanceRepository;
    @Mock private DtoMapper dtoMapper;
    @Mock private RoomScheduleService roomScheduleService;
    @InjectMocks private RoomService service;

    private RoomEntity room;
    private final LocalDate date = LocalDate.of(2026, 7, 15);
    private final LocalTime start = LocalTime.of(10, 0);
    private final LocalTime end = LocalTime.of(11, 0);

    @BeforeEach
    void setUp() {
        room = new RoomEntity();
        room.setId(10L);
        room.setEstado(RoomState.DISPONIBLE);
        room.setCantidadUnidades(4);
        when(roomScheduleService.isTimeWithinSchedule(room, date, start, end)).thenReturn(true);
        when(maintenanceRepository.findOverlapping(any(), any(), any())).thenReturn(List.of());
    }

    @Test
    void remainsAvailableWhileAtLeastOnePhysicalUnitIsFree() {
        when(reservationRepository.countOverlapping(room, date, start, end, null)).thenReturn(3L);

        assertThat(service.isRoomAvailable(room, date, start, end, null)).isTrue();
    }

    @Test
    void becomesUnavailableOnlyWhenEveryPhysicalUnitIsOccupied() {
        when(reservationRepository.countOverlapping(room, date, start, end, null)).thenReturn(4L);

        assertThat(service.isRoomAvailable(room, date, start, end, null)).isFalse();
    }
}
