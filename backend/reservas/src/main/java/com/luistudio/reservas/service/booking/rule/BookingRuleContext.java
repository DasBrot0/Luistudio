package com.luistudio.reservas.service.booking.rule;

import com.luistudio.reservas.dto.booking.BookingUpsertRequest;
import com.luistudio.reservas.model.RoomEntity;
import com.luistudio.reservas.model.UserEntity;

public record BookingRuleContext(
    UserEntity user,
    RoomEntity room,
    BookingUpsertRequest request,
    Long excludeBookingId
) {
}
