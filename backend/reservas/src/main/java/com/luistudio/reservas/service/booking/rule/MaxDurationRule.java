package com.luistudio.reservas.service.booking.rule;

import com.luistudio.reservas.exception.BusinessException;
import com.luistudio.reservas.service.RoomScheduleService;
import java.time.Duration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@Order(30)
public class MaxDurationRule implements BookingValidationRule {

    private final RoomScheduleService roomScheduleService;

    public MaxDurationRule(RoomScheduleService roomScheduleService) {
        this.roomScheduleService = roomScheduleService;
    }

    @Override
    public void validate(BookingRuleContext context) {
        int durationMinutes = (int) Duration.between(context.request().start(), context.request().end()).toMinutes();
        int maxDuration = roomScheduleService.getCampusSlotMinutes(context.room().getCampus());
        if (durationMinutes > maxDuration) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "La duración máxima permitida para esta sala/campus es " + maxDuration + " minutos"
            );
        }
    }
}
