package com.luistudio.reservas.service.booking.rule;

import com.luistudio.reservas.exception.BusinessException;
import com.luistudio.reservas.service.RoomService;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
@Order(40)
public class RoomAvailabilityRule implements BookingValidationRule {

    private final RoomService roomService;

    public RoomAvailabilityRule(RoomService roomService) {
        this.roomService = roomService;
    }

    @Override
    public void validate(BookingRuleContext context) {
        boolean available = roomService.isRoomAvailable(
            context.room(),
            context.request().date(),
            context.request().start(),
            context.request().end(),
            context.excludeBookingId()
        );
        if (!available) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "La sala no está disponible para el horario seleccionado");
        }
    }
}
