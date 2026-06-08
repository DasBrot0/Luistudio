package com.luistudio.reservas.controller;

import com.luistudio.reservas.dto.booking.BookingResponse;
import com.luistudio.reservas.dto.common.PageResponse;
import com.luistudio.reservas.dto.room.MaintenanceRequest;
import com.luistudio.reservas.dto.room.MaintenanceResponse;
import com.luistudio.reservas.dto.room.RoomResponse;
import com.luistudio.reservas.dto.room.RoomUpsertRequest;
import com.luistudio.reservas.service.AccessGuard;
import com.luistudio.reservas.service.BookingService;
import com.luistudio.reservas.service.RoomService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
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

    public RoomController(RoomService roomService, BookingService bookingService, AccessGuard accessGuard) {
        this.roomService = roomService;
        this.bookingService = bookingService;
        this.accessGuard = accessGuard;
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

    @GetMapping("/available")
    public List<RoomResponse> listAvailable(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha,
        @RequestParam(name = "horaInicio") @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime horaInicio,
        @RequestParam(name = "horaFin") @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime horaFin
    ) {
        accessGuard.requireUser();
        return roomService.listAvailableRooms(fecha, horaInicio, horaFin);
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

    @PatchMapping("/{roomId}")
    public RoomResponse patchRoom(@PathVariable Long roomId, @Valid @RequestBody RoomUpsertRequest request) {
        accessGuard.requireAdmin();
        return roomService.updateRoom(roomId, request);
    }

    @DeleteMapping("/{roomId}")
    public void deleteRoom(@PathVariable Long roomId) {
        accessGuard.requireAdmin();
        roomService.deleteRoom(roomId);
    }

    @PostMapping("/{roomId}/unavailability")
    public MaintenanceResponse createUnavailability(
        @PathVariable Long roomId,
        @Valid @RequestBody MaintenanceRequest request
    ) {
        accessGuard.requireAdmin();
        return roomService.createMaintenance(roomId, request);
    }

    @GetMapping("/{roomId}/unavailability")
    public List<MaintenanceResponse> listUnavailability(@PathVariable Long roomId) {
        accessGuard.requireUser();
        return roomService.getRoomUnavailability(roomId);
    }
}
