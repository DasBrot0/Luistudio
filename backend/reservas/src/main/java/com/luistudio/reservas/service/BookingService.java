package com.luistudio.reservas.service;

import com.luistudio.reservas.dto.booking.BookingResponse;
import com.luistudio.reservas.dto.booking.BookingUpsertRequest;
import com.luistudio.reservas.dto.common.PageResponse;
import com.luistudio.reservas.exception.NotFoundException;
import com.luistudio.reservas.model.ReservationEntity;
import com.luistudio.reservas.model.ReservationStatus;
import com.luistudio.reservas.model.RoomEntity;
import com.luistudio.reservas.model.UserEntity;
import com.luistudio.reservas.repository.ReservationRepository;
import com.luistudio.reservas.service.booking.rule.BookingRuleContext;
import com.luistudio.reservas.service.booking.rule.BookingValidationRule;
import com.luistudio.reservas.service.factory.ReservationFactory;
import com.luistudio.reservas.util.CalendarUtils;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {

    private final ReservationRepository reservationRepository;
    private final RoomService roomService;
    private final UserService userService;
    private final EmailOutboxService emailOutboxService;
    private final AuditService auditService;
    private final DtoMapper dtoMapper;
    private final ReservationFactory reservationFactory;
    private final List<BookingValidationRule> bookingValidationRules;

    public BookingService(
        ReservationRepository reservationRepository,
        RoomService roomService,
        UserService userService,
        EmailOutboxService emailOutboxService,
        AuditService auditService,
        DtoMapper dtoMapper,
        ReservationFactory reservationFactory,
        List<BookingValidationRule> bookingValidationRules
    ) {
        this.reservationRepository = reservationRepository;
        this.roomService = roomService;
        this.userService = userService;
        this.emailOutboxService = emailOutboxService;
        this.auditService = auditService;
        this.dtoMapper = dtoMapper;
        this.reservationFactory = reservationFactory;
        this.bookingValidationRules = bookingValidationRules;
    }

    @Transactional
    public BookingResponse createBooking(Long userId, BookingUpsertRequest request) {
        UserEntity user = userService.getById(userId);
        RoomEntity room = roomService.getRoomEntity(request.roomId());

        validateBookingRules(user, room, request, null);

        ReservationEntity booking = reservationFactory.createActiveReservation(user, room, request);
        ReservationEntity saved = reservationRepository.save(booking);

        String title = "Reserva confirmada #" + saved.getId();
        String body = "Sala " + room.getNombre() + " | " + saved.getFecha() + " " + saved.getHoraInicio() + "-" + saved.getHoraFin();
        String ics = getIcsContent(saved.getId());
        emailOutboxService.enqueue(user, title, body, "{\"ics\":\"" + ics.replace("\n", "\\n") + "\"}");

        return dtoMapper.toBooking(saved);
    }

    @Transactional
    public BookingResponse updateBooking(Long bookingId, Long actorUserId, BookingUpsertRequest request) {
        ReservationEntity current = getBookingEntity(bookingId);
        RoomEntity room = roomService.getRoomEntity(request.roomId());
        UserEntity actor = userService.getById(actorUserId);

        validateBookingRules(current.getUsuario(), room, request, bookingId);

        String previous = current.getSala().getCodigo() + " " + current.getFecha() + " " + current.getHoraInicio() + "-" + current.getHoraFin();

        current.setSala(room);
        current.setFecha(request.date());
        current.setHoraInicio(request.start());
        current.setHoraFin(request.end());
        current.setCantidadPersonas(request.people());
        current.setObservacion(request.observation());
        current.setUpdatedBy(actorUserId);
        current.setActualizadaEn(OffsetDateTime.now());

        ReservationEntity saved = reservationRepository.save(current);
        String next = saved.getSala().getCodigo() + " " + saved.getFecha() + " " + saved.getHoraInicio() + "-" + saved.getHoraFin();

        emailOutboxService.enqueue(
            saved.getUsuario(),
            "Reserva modificada #" + saved.getId(),
            "Anterior: " + previous + " | Nuevo: " + next,
            null
        );
        auditService.record(actor, "BOOKING_UPDATED", "reserva", String.valueOf(saved.getId()), "from=" + previous + ";to=" + next);

        return dtoMapper.toBooking(saved);
    }

    @Transactional
    public BookingResponse cancelBooking(Long bookingId, Long actorUserId, boolean adminCancel) {
        ReservationEntity booking = getBookingEntity(bookingId);
        if (booking.getEstado() == ReservationStatus.CANCELADA) {
            return dtoMapper.toBooking(booking);
        }

        booking.setEstado(ReservationStatus.CANCELADA);
        booking.setUpdatedBy(actorUserId);
        booking.setActualizadaEn(OffsetDateTime.now());

        ReservationEntity saved = reservationRepository.save(booking);
        String reason = adminCancel ? "cancelada por administrador" : "cancelada por usuario";
        emailOutboxService.enqueue(
            saved.getUsuario(),
            "Reserva cancelada #" + saved.getId(),
            "Tu reserva fue " + reason + ". Puedes reservar nuevamente.",
            null
        );
        return dtoMapper.toBooking(saved);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> listMyBookings(Long userId) {
        UserEntity user = userService.getById(userId);
        return reservationRepository.findByUsuarioOrderByFechaDescHoraInicioDesc(user)
            .stream()
            .map(dtoMapper::toBooking)
            .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> listAdminBookings(int page, int size, String status, LocalDate date) {
        ReservationStatus parsedStatus = null;
        if (status != null && !status.isBlank()) {
            parsedStatus = ReservationStatus.valueOf(status.toUpperCase());
        }

        Page<ReservationEntity> bookingPage = reservationRepository.findForAdmin(parsedStatus, date, PageRequest.of(page, size));
        return new PageResponse<>(
            bookingPage.getContent().stream().map(dtoMapper::toBooking).toList(),
            bookingPage.getNumber(),
            bookingPage.getSize(),
            bookingPage.getTotalElements(),
            bookingPage.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public ReservationEntity getBookingEntity(Long id) {
        return reservationRepository.findById(id).orElseThrow(() -> new NotFoundException("Reserva no encontrada"));
    }

    @Transactional(readOnly = true)
    public String getIcsContent(Long bookingId) {
        ReservationEntity reservation = getBookingEntity(bookingId);
        return CalendarUtils.createIcs(
            "Reserva - " + reservation.getSala().getNombre(),
            "Reserva Luistudio #" + reservation.getId(),
            reservation.getSala().getUbicacion(),
            reservation.getFecha(),
            reservation.getHoraInicio(),
            reservation.getHoraFin()
        );
    }

    private void validateBookingRules(UserEntity user, RoomEntity room, BookingUpsertRequest request, Long excludeBookingId) {
        BookingRuleContext context = new BookingRuleContext(user, room, request, excludeBookingId);
        for (BookingValidationRule rule : bookingValidationRules) {
            rule.validate(context);
        }
    }
}
