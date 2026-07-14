package com.luistudio.reservas.controller;

import com.luistudio.reservas.dto.booking.BookingResponse;
import com.luistudio.reservas.dto.common.MessageResponse;
import com.luistudio.reservas.dto.common.PageResponse;
import com.luistudio.reservas.dto.room.AvailabilitySubscriptionRequest;
import com.luistudio.reservas.dto.room.AvailabilitySubscriptionResponse;
import com.luistudio.reservas.dto.room.RoomResponse;
import com.luistudio.reservas.dto.room.RoomUpsertRequest;
import com.luistudio.reservas.dto.room.IntelligentRoomSearchRequest;
import com.luistudio.reservas.dto.room.IntelligentRoomSearchResponse;
import com.luistudio.reservas.security.AuthPrincipal;
import com.luistudio.reservas.service.AccessGuard;
import com.luistudio.reservas.service.AvailabilitySubscriptionService;
import com.luistudio.reservas.service.BookingService;
import com.luistudio.reservas.service.RoomService;
import com.luistudio.reservas.service.IntelligentRoomSearchService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomService roomService;
    private final BookingService bookingService;
    private final AccessGuard accessGuard;
    private final AvailabilitySubscriptionService subscriptionService;
    private final IntelligentRoomSearchService intelligentRoomSearchService;

    public RoomController(
        RoomService roomService,
        BookingService bookingService,
        AccessGuard accessGuard,
        AvailabilitySubscriptionService subscriptionService,
        IntelligentRoomSearchService intelligentRoomSearchService
    ) {
        this.roomService = roomService;
        this.bookingService = bookingService;
        this.accessGuard = accessGuard;
        this.subscriptionService = subscriptionService;
        this.intelligentRoomSearchService = intelligentRoomSearchService;
    }

    @GetMapping
    public PageResponse<RoomResponse> listRooms(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size,
        @RequestParam(defaultValue = "false") boolean includeSchedule,
        @RequestParam(required = false) String campus,
        @RequestParam(required = false) String recinto,
        @RequestParam(required = false) String ubicacion,
        @RequestParam(required = false, name = "q") String query
    ) {
        accessGuard.requireUser();
        return roomService.listRooms(page, size, includeSchedule, campus, recinto, ubicacion, query);
    }

    @PostMapping("/intelligent-search")
    public IntelligentRoomSearchResponse intelligentSearch(@Valid @RequestBody IntelligentRoomSearchRequest request) {
        accessGuard.requireUser();
        return intelligentRoomSearchService.search(request);
    }

    @GetMapping("/{roomId}/bookings")
    public List<BookingResponse> listRoomBookings(
        @PathVariable Long roomId,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta
    ) {
        accessGuard.requireUser();
        return bookingService.listRoomBookings(roomId, desde, hasta);
    }

    @PostMapping
    public RoomResponse createRoom(@Valid @RequestBody RoomUpsertRequest request) {
        accessGuard.requireAdmin();
        return roomService.createRoom(request);
    }

    @PutMapping("/{roomId}")
    public RoomResponse updateRoom(@PathVariable Long roomId, @Valid @RequestBody RoomUpsertRequest request) {
        accessGuard.requireAdmin();
        return roomService.updateRoom(roomId, request);
    }

    @DeleteMapping("/{roomId}")
    public void deleteRoom(@PathVariable Long roomId) {
        accessGuard.requireAdmin();
        roomService.deleteRoom(roomId);
    }

    @PostMapping("/{roomId}/availability-subscriptions")
    public AvailabilitySubscriptionResponse subscribeToRoom(
        @PathVariable Long roomId,
        @RequestBody AvailabilitySubscriptionRequest request
    ) {
        AuthPrincipal principal = accessGuard.requireUser();
        return subscriptionService.subscribe(principal.userId(), roomId, request);
    }

    @DeleteMapping("/{roomId}/availability-subscriptions/me")
    public ResponseEntity<MessageResponse> unsubscribeFromRoom(@PathVariable Long roomId) {
        AuthPrincipal principal = accessGuard.requireUser();
        subscriptionService.unsubscribe(principal.userId(), roomId);
        return ResponseEntity.ok(new MessageResponse("Suscripción cancelada"));
    }
}
