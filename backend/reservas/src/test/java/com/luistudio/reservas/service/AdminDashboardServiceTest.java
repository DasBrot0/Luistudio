package com.luistudio.reservas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.luistudio.reservas.exception.BusinessException;
import com.luistudio.reservas.model.CampusEntity;
import com.luistudio.reservas.model.CampusScheduleEntity;
import com.luistudio.reservas.model.PabellonEntity;
import com.luistudio.reservas.model.ReservationEntity;
import com.luistudio.reservas.model.ReservationStatus;
import com.luistudio.reservas.model.RoleEntity;
import com.luistudio.reservas.model.RoomEntity;
import com.luistudio.reservas.model.RoomScheduleEntity;
import com.luistudio.reservas.model.RoomState;
import com.luistudio.reservas.model.UserEntity;
import com.luistudio.reservas.repository.ReservationRepository;
import com.luistudio.reservas.repository.CampusScheduleRepository;
import com.luistudio.reservas.repository.RoomRepository;
import com.luistudio.reservas.repository.RoomScheduleRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {
    @Mock ReservationRepository reservationRepository;
    @Mock RoomRepository roomRepository;
    @Mock RoomScheduleRepository roomScheduleRepository;
    @Mock CampusScheduleRepository campusScheduleRepository;
    private AdminDashboardService service;

    @BeforeEach
    void setUp() {
        service = new AdminDashboardService(reservationRepository, roomRepository, roomScheduleRepository, campusScheduleRepository);
    }

    @Test
    void calculatesOccupancyPeakRankingAndAbsenceRate() {
        LocalDate monday = LocalDate.of(2026, 7, 6);
        RoomEntity room = room(1L, "A-101", "Sala A");
        UserEntity student = student(8L, "20201234", "Ana", "Ramos");
        ReservationEntity attended = booking(room, student, monday, LocalTime.of(9, 0), LocalTime.of(11, 0), null);
        ReservationEntity absent = booking(room, student, monday, LocalTime.of(14, 0), LocalTime.of(15, 0), "INASISTIO");
        ReservationEntity cancelled = booking(room, student, monday, LocalTime.of(15, 0), LocalTime.of(16, 0), null);
        cancelled.setEstado(ReservationStatus.CANCELADA);
        RoomScheduleEntity schedule = new RoomScheduleEntity();
        schedule.setSala(room);
        schedule.setDiaSemana(1);
        schedule.setHoraApertura(LocalTime.of(8, 0));
        schedule.setHoraCierre(LocalTime.of(18, 0));
        schedule.setCerrado(false);

        when(reservationRepository.findForDashboard(monday, monday)).thenReturn(List.of(attended, absent, cancelled));
        when(roomRepository.findByEstadoNot(RoomState.INACTIVA)).thenReturn(List.of(room));
        when(roomScheduleRepository.findBySalaIdIn(List.of(1L))).thenReturn(List.of(schedule));

        var result = service.getDashboard(monday, monday);

        assertThat(result.totalReservations()).isEqualTo(2);
        assertThat(result.absenceRate()).isEqualTo(50.0);
        assertThat(result.occupancyByRoom().getFirst().occupancyRate()).isEqualTo(30.0);
        assertThat(result.peakHours().getFirst().hour()).isEqualTo(9);
        assertThat(result.topStudents().getFirst().reservationCount()).isEqualTo(2);
        assertThat(result.topStudents().getFirst().absenceCount()).isEqualTo(1);
        assertThat(result.dailyOccupancy()).hasSize(1);
        assertThat(result.dailyOccupancy().getFirst().occupancyRate()).isEqualTo(30.0);
        assertThat(result.weeklyHeatmap()).anySatisfy(cell -> {
            assertThat(cell.dayOfWeek()).isEqualTo(1);
            assertThat(cell.hour()).isEqualTo(9);
            assertThat(cell.occupancyRate()).isEqualTo(100.0);
        });
    }

    @Test
    void rejectsInvertedRange() {
        assertThatThrownBy(() -> service.getDashboard(LocalDate.of(2026, 7, 2), LocalDate.of(2026, 7, 1)))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("rango");
    }

    @Test
    void usesCampusScheduleWhenRoomHasNoSpecificSchedule() {
        LocalDate monday = LocalDate.of(2026, 7, 6);
        CampusEntity campus = new CampusEntity();
        campus.setId(10L);
        campus.setNombre("Monterrico");
        PabellonEntity building = new PabellonEntity();
        building.setId(20L);
        building.setCampus(campus);
        RoomEntity room = room(1L, "A-101", "Sala A");
        room.setPabellon(building);
        ReservationEntity booking = booking(room, student(8L, "20201234", "Ana", "Ramos"), monday,
            LocalTime.of(9, 0), LocalTime.of(10, 0), null);
        CampusScheduleEntity campusSchedule = new CampusScheduleEntity();
        campusSchedule.setCampus(campus);
        campusSchedule.setDiaSemana(1);
        campusSchedule.setHoraApertura(LocalTime.of(8, 0));
        campusSchedule.setHoraCierre(LocalTime.of(18, 0));
        campusSchedule.setCerrado(false);

        when(reservationRepository.findForDashboard(monday, monday)).thenReturn(List.of(booking));
        when(roomRepository.findByEstadoNot(RoomState.INACTIVA)).thenReturn(List.of(room));
        when(roomScheduleRepository.findBySalaIdIn(List.of(1L))).thenReturn(List.of());
        when(campusScheduleRepository.findByCampus_IdIn(List.of(10L))).thenReturn(List.of(campusSchedule));

        var result = service.getDashboard(monday, monday);

        assertThat(result.occupancyByRoom().getFirst().availableMinutes()).isEqualTo(600);
        assertThat(result.occupancyByRoom().getFirst().occupancyRate()).isEqualTo(10.0);
        assertThat(result.dailyOccupancy().getFirst().occupancyRate()).isEqualTo(10.0);
        assertThat(result.weeklyHeatmap()).isNotEmpty();
    }

    private RoomEntity room(Long id, String code, String name) {
        RoomEntity room = new RoomEntity();
        room.setId(id);
        room.setCodigo(code);
        room.setNombre(name);
        room.setEstado(RoomState.DISPONIBLE);
        return room;
    }

    private UserEntity student(Long id, String code, String firstName, String lastName) {
        RoleEntity role = new RoleEntity();
        role.setNombre("ESTUDIANTE");
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setRol(role);
        user.setCodigo(code);
        user.setNombres(firstName);
        user.setApellidos(lastName);
        user.setCorreo("ana@ulima.edu.pe");
        return user;
    }

    private ReservationEntity booking(RoomEntity room, UserEntity user, LocalDate date, LocalTime start, LocalTime end, String attendance) {
        ReservationEntity booking = new ReservationEntity();
        booking.setSala(room);
        booking.setUsuario(user);
        booking.setFecha(date);
        booking.setHoraInicio(start);
        booking.setHoraFin(end);
        booking.setEstado(ReservationStatus.COMPLETADA);
        booking.setAttendanceStatus(attendance);
        return booking;
    }
}
