package com.luistudio.reservas.config;

import com.luistudio.reservas.service.CampusMapService;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/** Invalidates short-lived map entries only after a successful domain mutation. */
@Aspect
@Component
public class CampusMapCacheInvalidationAspect {
    private final CampusMapService campusMapService;
    public CampusMapCacheInvalidationAspect(CampusMapService campusMapService) { this.campusMapService = campusMapService; }

    @AfterReturning("execution(* com.luistudio.reservas.service.BookingService.createBooking(..)) || "
        + "execution(* com.luistudio.reservas.service.BookingService.updateBooking(..)) || "
        + "execution(* com.luistudio.reservas.service.BookingService.cancelBooking(..)) || "
        + "execution(* com.luistudio.reservas.service.RoomService.createRoom(..)) || "
        + "execution(* com.luistudio.reservas.service.RoomService.updateRoom(..)) || "
        + "execution(* com.luistudio.reservas.service.RoomService.deleteRoom(..))")
    public void evictAfterMapRelevantMutation() { campusMapService.evictAll(); }
}
