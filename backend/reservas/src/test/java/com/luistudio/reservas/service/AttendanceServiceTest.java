package com.luistudio.reservas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.luistudio.reservas.dto.admin.AttendanceStatusUpdateRequest;
import com.luistudio.reservas.model.AttendanceRecordEntity;
import com.luistudio.reservas.model.CampusEntity;
import com.luistudio.reservas.model.PabellonEntity;
import com.luistudio.reservas.model.ReservationEntity;
import com.luistudio.reservas.model.ReservationStatus;
import com.luistudio.reservas.model.RoomEntity;
import com.luistudio.reservas.model.UserEntity;
import com.luistudio.reservas.repository.AttendanceRecordRepository;
import com.luistudio.reservas.repository.ReservationRepository;
import com.luistudio.reservas.service.email.EmailTemplateService;
import com.luistudio.reservas.util.AppTime;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock private ReservationRepository reservationRepository;
    @Mock private AttendanceRecordRepository attendanceRecordRepository;
    @Mock private EmailOutboxService emailOutboxService;
    @Mock private EmailTemplateService emailTemplateService;
    @Mock private UserService userService;
    @Mock private AuditService auditService;

    private AttendanceService service;
    private ReservationEntity booking;
    private UserEntity student;

    @BeforeEach
    void setUp() {
        service = new AttendanceService(
            reservationRepository,
            attendanceRecordRepository,
            emailOutboxService,
            emailTemplateService,
            userService,
            auditService
        );
        student = new UserEntity();
        student.setId(10L);
        student.setCodigo("20260001");
        student.setNombres("Ana");
        student.setApellidos("Torres");
        student.setCorreo("ana@aloe.ulima.edu.pe");

        CampusEntity campus = new CampusEntity();
        campus.setNombre("Monterrico");
        PabellonEntity pavilion = new PabellonEntity();
        pavilion.setCodigo("M");
        pavilion.setNombre("Biblioteca Antonio Pinilla");
        pavilion.setCampus(campus);
        RoomEntity room = new RoomEntity();
        room.setId(20L);
        room.setCodigo("M-2-GRU-06");
        room.setNombre("Sala grupal");
        room.setUbicacion("2do piso");
        room.setPabellon(pavilion);

        booking = new ReservationEntity();
        booking.setId(30L);
        booking.setUsuario(student);
        booking.setSala(room);
        booking.setFecha(AppTime.today());
        booking.setHoraInicio(LocalTime.now().minusMinutes(20));
        booking.setHoraFin(LocalTime.now().plusMinutes(40));
        booking.setEstado(ReservationStatus.ACTIVA);
        booking.setCantidadPersonas(1);
    }

    @Test
    void recordsAndNotifiesAbsenceAfterTolerance() {
        LocalDateTime beforeCutoff = AppTime.nowDateTime().minusMinutes(15).minusSeconds(1);
        when(reservationRepository.findActiveBookingsMissedBefore(any(), any())).thenReturn(List.of(booking));
        when(attendanceRecordRepository.findByReserva(booking)).thenReturn(Optional.empty());
        when(emailTemplateService.absenceNotice(booking)).thenReturn("Aviso");

        service.processMissedBookings();

        LocalDateTime afterCutoff = AppTime.nowDateTime().minusMinutes(15).plusSeconds(1);
        ArgumentCaptor<LocalDate> cutoffDate = ArgumentCaptor.forClass(LocalDate.class);
        ArgumentCaptor<LocalTime> cutoffTime = ArgumentCaptor.forClass(LocalTime.class);
        verify(reservationRepository).findActiveBookingsMissedBefore(cutoffDate.capture(), cutoffTime.capture());
        LocalDateTime actualCutoff = LocalDateTime.of(cutoffDate.getValue(), cutoffTime.getValue());
        assertThat(actualCutoff).isBetween(beforeCutoff, afterCutoff);
        assertThat(booking.getAttendanceStatus()).isEqualTo("INASISTIO");
        ArgumentCaptor<AttendanceRecordEntity> record = ArgumentCaptor.forClass(AttendanceRecordEntity.class);
        verify(attendanceRecordRepository).save(record.capture());
        assertThat(record.getValue().getStatus()).isEqualTo("INASISTIO");
        assertThat(record.getValue().getToleranceMinutes()).isEqualTo(15);
        assertThat(record.getValue().getRecordedBy()).isNull();
        verify(emailOutboxService).enqueue(eq(student), eq("Inasistencia registrada"), eq("Aviso"), any());
    }

    @Test
    void administratorCanCorrectAbsenceToAttendance() {
        UserEntity admin = new UserEntity();
        admin.setId(99L);
        AttendanceRecordEntity record = new AttendanceRecordEntity();
        record.setReserva(booking);
        record.setUsuario(student);
        record.setStatus("INASISTIO");
        booking.setAttendanceStatus("INASISTIO");
        booking.setFecha(AppTime.today().minusDays(1));
        when(reservationRepository.findById(30L)).thenReturn(Optional.of(booking));
        when(userService.getById(99L)).thenReturn(admin);
        when(attendanceRecordRepository.findByReserva(booking)).thenReturn(Optional.of(record));

        var response = service.updateAttendance(30L, 99L, new AttendanceStatusUpdateRequest("ASISTIO"));

        assertThat(response.attendanceStatus()).isEqualTo("ASISTIO");
        assertThat(record.getStatus()).isEqualTo("ASISTIO");
        assertThat(record.getRecordedBy()).isSameAs(admin);
        assertThat(record.getToleranceMinutes()).isZero();
        verify(auditService).record(eq(admin), eq("ATTENDANCE_UPDATED"), eq("reserva"), eq("30"), any());
    }
}
