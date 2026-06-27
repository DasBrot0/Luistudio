package com.luistudio.reservas.controller;

import com.luistudio.reservas.dto.booking.BookingResponse;
import com.luistudio.reservas.dto.booking.BookingUpsertRequest;
import com.luistudio.reservas.dto.common.MessageResponse;
import com.luistudio.reservas.dto.common.PageResponse;
import com.luistudio.reservas.security.AuthPrincipal;
import com.luistudio.reservas.service.AccessGuard;
import com.luistudio.reservas.service.BookingService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
@RequestMapping("/api")
public class BookingController {

    private final BookingService bookingService;
    private final AccessGuard accessGuard;

    public BookingController(BookingService bookingService, AccessGuard accessGuard) {
        this.bookingService = bookingService;
        this.accessGuard = accessGuard;
    }

    @PostMapping("/bookings")
    public BookingResponse createBooking(@Valid @RequestBody BookingUpsertRequest request) {
        AuthPrincipal principal = accessGuard.requireUser();
        return bookingService.createBooking(principal.userId(), request);
    }

    @PutMapping("/bookings/{bookingId}")
    public BookingResponse updateBooking(@PathVariable Long bookingId, @Valid @RequestBody BookingUpsertRequest request) {
        AuthPrincipal principal = accessGuard.requireUser();
        return bookingService.updateBooking(bookingId, principal.userId(), request);
    }

    @PatchMapping("/bookings/{bookingId}/cancel")
    public BookingResponse cancelBooking(@PathVariable Long bookingId) {
        AuthPrincipal principal = accessGuard.requireUser();
        boolean admin = "ADMIN".equalsIgnoreCase(principal.role());
        return bookingService.cancelBooking(bookingId, principal.userId(), admin);
    }

    @GetMapping("/bookings/me")
    public PageResponse<BookingResponse> getMyBookings(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        AuthPrincipal principal = accessGuard.requireUser();
        return bookingService.listMyBookings(principal.userId(), page, size);
    }

    @GetMapping("/bookings/{bookingId}/ics")
    public ResponseEntity<String> getIcs(@PathVariable Long bookingId) {
        AuthPrincipal principal = accessGuard.requireUser();
        String ics = bookingService.getStudentIcsContent(bookingId, principal.userId());
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/calendar"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=booking-" + bookingId + ".ics")
            .body(ics);
    }

    @GetMapping("/admin/bookings")
    public PageResponse<BookingResponse> listAdminBookings(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) String status,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha
    ) {
        accessGuard.requireAdmin();
        return bookingService.listAdminBookings(page, size, status, fecha);
    }

    @PostMapping("/notifications/booking-confirmation")
    public MessageResponse sendConfirmation(@RequestParam Long bookingId) {
        accessGuard.requireAdmin();
        String ics = bookingService.getIcsContent(bookingId);
        return new MessageResponse("Confirmación generada para reserva " + bookingId + " (ICS length=" + ics.length() + ")");
    }
}
