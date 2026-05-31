package com.luistudio.reservas.service.booking.rule;

import com.luistudio.reservas.exception.BusinessException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@Order(10)
public class EndTimeAfterStartRule implements BookingValidationRule {

    @Override
    public void validate(BookingRuleContext context) {
        if (!context.request().end().isAfter(context.request().start())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "La hora fin debe ser mayor a la hora inicio");
        }
    }
}
