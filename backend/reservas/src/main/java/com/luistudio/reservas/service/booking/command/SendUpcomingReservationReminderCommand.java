package com.luistudio.reservas.service.booking.command;

import com.luistudio.reservas.model.ReservationEntity;
import com.luistudio.reservas.repository.ReservationRepository;
import com.luistudio.reservas.service.EmailOutboxService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SendUpcomingReservationReminderCommand implements BookingReminderCommand {

    private final ReservationRepository reservationRepository;
    private final EmailOutboxService emailOutboxService;

    public SendUpcomingReservationReminderCommand(
        ReservationRepository reservationRepository,
        EmailOutboxService emailOutboxService
    ) {
        this.reservationRepository = reservationRepository;
        this.emailOutboxService = emailOutboxService;
    }

    @Override
    @Transactional
    public void execute() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        OffsetDateTime nextHour = OffsetDateTime.now(ZoneOffset.ofHours(-5)).plusMinutes(60);

        List<ReservationEntity> upcoming = reservationRepository.findUpcomingWindow(
            today,
            now,
            nextHour.toLocalDate(),
            nextHour.toLocalTime()
        );

        for (ReservationEntity booking : upcoming) {
            emailOutboxService.enqueueReminderOnce(
                booking.getUsuario(),
                "Recordatorio de reserva",
                "Tu reserva inicia en menos de 60 minutos en sala " + booking.getSala().getNombre(),
                booking.getId(),
                "UPCOMING_60M"
            );
        }
    }
}
