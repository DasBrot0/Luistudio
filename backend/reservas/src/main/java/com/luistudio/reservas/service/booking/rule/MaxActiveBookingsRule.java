package com.luistudio.reservas.service.booking.rule;

import com.luistudio.reservas.exception.BusinessException;
import com.luistudio.reservas.repository.ReservationRepository;
import com.luistudio.reservas.service.SystemConfigService;
import java.time.LocalDate;
import java.time.LocalTime;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@Order(50)
public class MaxActiveBookingsRule implements BookingValidationRule {

    private final ReservationRepository reservationRepository;
    private final SystemConfigService systemConfigService;

    public MaxActiveBookingsRule(
        ReservationRepository reservationRepository,
        SystemConfigService systemConfigService
    ) {
        this.reservationRepository = reservationRepository;
        this.systemConfigService = systemConfigService;
    }

    @Override
    public void validate(BookingRuleContext context) {
        if (context.excludeBookingId() != null) {
            return;
        }

        int maxAllowed = systemConfigService.getMaxActiveBookings();
        long activeCount = reservationRepository.countCurrentActiveForUser(context.user(), LocalDate.now(), LocalTime.now());
        if (activeCount >= maxAllowed) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "Alcanzaste el limite de reservas activas (" + maxAllowed + ")"
            );
        }
    }
}
