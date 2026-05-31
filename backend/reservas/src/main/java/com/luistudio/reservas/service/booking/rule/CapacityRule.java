package com.luistudio.reservas.service.booking.rule;

import com.luistudio.reservas.exception.BusinessException;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@Order(20)
public class CapacityRule implements BookingValidationRule {

    @Override
    public void validate(BookingRuleContext context) {
        if (context.request().people() > context.room().getCapacidad()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "La cantidad de personas supera la capacidad de la sala");
        }
    }
}
