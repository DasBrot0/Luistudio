package com.luistudio.reservas.service;

import com.luistudio.reservas.model.AttendanceRecordEntity;
import com.luistudio.reservas.model.ReservationEntity;
import com.luistudio.reservas.repository.AttendanceRecordRepository;
import com.luistudio.reservas.repository.ReservationRepository;
import com.luistudio.reservas.service.email.EmailTemplateService;
import com.luistudio.reservas.util.AppTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttendanceService {

    private static final Logger log = LoggerFactory.getLogger(AttendanceService.class);
    private static final int TOLERANCE_MINUTES = 15;

    private final ReservationRepository reservationRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final EmailOutboxService emailOutboxService;
    private final EmailTemplateService emailTemplateService;

    public AttendanceService(
        ReservationRepository reservationRepository,
        AttendanceRecordRepository attendanceRecordRepository,
        EmailOutboxService emailOutboxService,
        EmailTemplateService emailTemplateService
    ) {
        this.reservationRepository = reservationRepository;
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.emailOutboxService = emailOutboxService;
        this.emailTemplateService = emailTemplateService;
    }

    @Transactional
    public void processMissedBookings() {
        LocalDate today = AppTime.today();
        LocalTime cutoff = AppTime.nowTime().minusMinutes(TOLERANCE_MINUTES);

        List<ReservationEntity> missed = reservationRepository.findActiveBookingsMissedBefore(today, cutoff);
        log.info("attendance_scheduler_run date={} cutoff={} candidates={}", today, cutoff, missed.size());

        for (ReservationEntity booking : missed) {
            if (attendanceRecordRepository.existsByReserva(booking)) {
                continue; // Idempotency
            }

            booking.setAttendanceStatus("INASISTIO");
            reservationRepository.save(booking);

            AttendanceRecordEntity record = new AttendanceRecordEntity();
            record.setReserva(booking);
            record.setUsuario(booking.getUsuario());
            record.setRecordedAt(OffsetDateTime.now());
            record.setToleranceMinutes(TOLERANCE_MINUTES);
            attendanceRecordRepository.save(record);

            emailOutboxService.enqueue(
                booking.getUsuario(),
                "Inasistencia registrada",
                emailTemplateService.absenceNotice(booking),
                "{\"notificationType\":\"ABSENCE_NOTICE\"}"
            );
            log.info("attendance_marked_absent bookingId={}", booking.getId());
        }
    }
}
