package com.luistudio.reservas.service.booking.rule;

import com.luistudio.reservas.exception.BusinessException;
import com.luistudio.reservas.repository.ReservationRepository;
import com.luistudio.reservas.service.SystemConfigService;
import com.luistudio.reservas.util.AppTime;
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
        long activeCount = reservationRepository.countCurrentActiveForUser(context.user(), AppTime.today(), AppTime.nowTime());
        if (activeCount >= maxAllowed) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "Alcanzaste el límite de reservas activas (" + maxAllowed + ")"
            );
        }
    }
}
