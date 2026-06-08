package com.luistudio.reservas.service.booking.command;

import com.luistudio.reservas.model.ReservationEntity;
import com.luistudio.reservas.repository.ReservationRepository;
import com.luistudio.reservas.service.EmailOutboxService;
import com.luistudio.reservas.service.email.EmailTemplateService;
import com.luistudio.reservas.util.AppTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SendEndingSoonReservationReminderCommand implements BookingReminderCommand {

    private final ReservationRepository reservationRepository;
    private final EmailOutboxService emailOutboxService;
    private final EmailTemplateService emailTemplateService;

    public SendEndingSoonReservationReminderCommand(
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
        LocalDate date = AppTime.today();
        LocalTime now = AppTime.nowTime();
        LocalTime min = now.minusMinutes(15);

        List<ReservationEntity> reservations = reservationRepository.findEndingSoon(
            date,
            now.plusMinutes(15),
            min
        );

        for (ReservationEntity booking : reservations) {
            emailOutboxService.enqueueReminderOnce(
                booking.getUsuario(),
                "Tu reserva termina pronto",
                emailTemplateService.bookingReminder(
                    booking,
                    "Tu reserva termina pronto",
                    "Tu reserva termina en menos de 15 minutos."
                ),
                booking.getId(),
                "ENDING_SOON_15M"
            );
        }
    }
}
