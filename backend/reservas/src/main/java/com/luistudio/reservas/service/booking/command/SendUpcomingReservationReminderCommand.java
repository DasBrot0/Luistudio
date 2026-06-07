package com.luistudio.reservas.service.booking.command;

import com.luistudio.reservas.model.ReservationEntity;
import com.luistudio.reservas.repository.ReservationRepository;
import com.luistudio.reservas.service.EmailOutboxService;
import com.luistudio.reservas.service.email.EmailTemplateService;
import com.luistudio.reservas.util.AppTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SendUpcomingReservationReminderCommand implements BookingReminderCommand {

    private final ReservationRepository reservationRepository;
    private final EmailOutboxService emailOutboxService;
    private final EmailTemplateService emailTemplateService;

    public SendUpcomingReservationReminderCommand(
        ReservationRepository reservationRepository,
        EmailOutboxService emailOutboxService,
        EmailTemplateService emailTemplateService
    ) {
        this.reservationRepository = reservationRepository;
        this.emailOutboxService = emailOutboxService;
        this.emailTemplateService = emailTemplateService;
    }

    @Override
    public void ejecutar() {
        LocalDate today = AppTime.today();
        LocalTime now = AppTime.nowTime();
        OffsetDateTime nextHour = AppTime.nowDateTime()
            .atZone(AppTime.ZONE)
            .toOffsetDateTime()
            .plusMinutes(60);

        List<ReservationEntity> reservations = reservationRepository.findUpcomingWindow(
            today,
            now,
            nextHour.toLocalDate(),
            nextHour.toLocalTime()
        );

        for (ReservationEntity booking : reservations) {
            emailOutboxService.enqueueReminderOnce(
                booking.getUsuario(),
                "Recordatorio de reserva",
                emailTemplateService.bookingReminder(
                    booking,
                    "Recordatorio de reserva",
                    "Tu reserva inicia en menos de 60 minutos."
                ),
                booking.getId(),
                "UPCOMING_60M"
            );
        }
    }

    @Override
    public void deshacer() {
        // No aplica reversion: el comando solo encola recordatorios si todavia no existen.
    }
}
