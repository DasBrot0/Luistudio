package com.luistudio.reservas.service.booking.rule;

import com.luistudio.reservas.exception.BusinessException;
import com.luistudio.reservas.service.SystemConfigService;
import java.time.Duration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@Order(30)
public class MaxDurationRule implements BookingValidationRule {

    private final SystemConfigService systemConfigService;

    public MaxDurationRule(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    @Override
    public void validate(BookingRuleContext context) {
        int durationMinutes = (int) Duration.between(context.request().start(), context.request().end()).toMinutes();
        int maxDuration = systemConfigService.getMaxDurationMinutes();
        if (durationMinutes > maxDuration) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "La duracion maxima permitida es " + maxDuration + " minutos");
        }
    }
}
