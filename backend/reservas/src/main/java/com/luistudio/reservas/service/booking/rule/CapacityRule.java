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
        int people = context.request().people();
        int roomCapacity = context.room().getCapacidad();
        int roomMax = context.room().getMaximoPersonas() == null ? roomCapacity : context.room().getMaximoPersonas();
        int roomMin = context.room().getMinimoPersonas() == null ? 1 : context.room().getMinimoPersonas();
        boolean minRequired = Boolean.TRUE.equals(context.room().getMinimoPersonasObligatorio());

        if (people > roomCapacity || people > roomMax) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "La cantidad de personas supera el maximo permitido para la sala");
        }
        if (minRequired && people < roomMin) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "La reserva requiere al menos " + roomMin + " personas para esta sala");
        }
    }
}
