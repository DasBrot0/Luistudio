package com.luistudio.reservas.service.booking.command;

import com.luistudio.reservas.model.ReservationEntity;
import com.luistudio.reservas.repository.ReservationRepository;
import com.luistudio.reservas.service.EmailOutboxService;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SendEndingSoonReservationReminderCommand implements BookingReminderCommand {

    private final ReservationRepository reservationRepository;
    private final EmailOutboxService emailOutboxService;

    public SendEndingSoonReservationReminderCommand(
        ReservationRepository reservationRepository,
        EmailOutboxService emailOutboxService
    ) {
        this.reservationRepository = reservationRepository;
        this.emailOutboxService = emailOutboxService;
    }

    @Override
    @Transactional
    public void execute() {
        LocalDate date = LocalDate.now();
        LocalTime now = LocalTime.now();
        LocalTime min = now.minusMinutes(15);

        List<ReservationEntity> endingSoon = reservationRepository.findEndingSoon(date, now.plusMinutes(15), min);
        for (ReservationEntity booking : endingSoon) {
            emailOutboxService.enqueueReminderOnce(
                booking.getUsuario(),
                "Tu reserva termina pronto",
                "Tu reserva termina en menos de 15 minutos.",
                booking.getId(),
                "ENDING_SOON_15M"
            );
        }
    }
}
