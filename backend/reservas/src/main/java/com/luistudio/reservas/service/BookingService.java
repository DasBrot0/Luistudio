package com.luistudio.reservas.service;

import com.luistudio.reservas.dto.booking.BookingResponse;
import com.luistudio.reservas.dto.booking.BookingUpsertRequest;
import com.luistudio.reservas.dto.common.PageResponse;
import com.luistudio.reservas.exception.BusinessException;
import com.luistudio.reservas.exception.NotFoundException;
import com.luistudio.reservas.model.ReservationEntity;
import com.luistudio.reservas.model.ReservationStatus;
import com.luistudio.reservas.model.RoomEntity;
import com.luistudio.reservas.model.UserEntity;
import com.luistudio.reservas.repository.ReservationRepository;
import com.luistudio.reservas.service.booking.validation.BookingValidationService;
import com.luistudio.reservas.service.email.EmailTemplateService;
import com.luistudio.reservas.util.AppTime;
import com.luistudio.reservas.util.CalendarUtils;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
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
    private final BookingValidationService bookingValidationService;
    private final EmailTemplateService emailTemplateService;

    public BookingService(
        ReservationRepository reservationRepository,
        RoomService roomService,
        UserService userService,
        EmailOutboxService emailOutboxService,
        AuditService auditService,
        DtoMapper dtoMapper,
        BookingValidationService bookingValidationService,
        EmailTemplateService emailTemplateService
    ) {
        this.reservationRepository = reservationRepository;
        this.roomService = roomService;
        this.userService = userService;
        this.emailOutboxService = emailOutboxService;
        this.auditService = auditService;
        this.dtoMapper = dtoMapper;
        this.bookingValidationService = bookingValidationService;
        this.emailTemplateService = emailTemplateService;
    }

    @Transactional
    public BookingResponse createBooking(Long userId, BookingUpsertRequest request) {
        UserEntity user = userService.getById(userId);
        RoomEntity room = roomService.getRoomEntity(request.roomId());

        ReservationEntity saved = reservationRepository
            .findTopByUsuarioAndSalaAndFechaAndHoraInicioAndHoraFinOrderByIdDesc(
                user,
                room,
                request.date(),
                request.start(),
                request.end()
            )
            .map(existing -> {
                bookingValidationService.validate(user, room, request, existing.getId());
                existing.setEstado(ReservationStatus.ACTIVA);
                existing.setCantidadPersonas(request.people());
                existing.setObservacion(request.observation());
                existing.setUpdatedBy(userId);
                existing.setActualizadaEn(OffsetDateTime.now());
                return reservationRepository.save(existing);
            })
            .orElseGet(() -> {
                bookingValidationService.validate(user, room, request, null);
                ReservationEntity booking = buildActiveReservation(user, room, request);
                return reservationRepository.save(booking);
            });

        String title = "Reserva confirmada";
        String body = emailTemplateService.bookingStatus(saved, title, "confirmada", null);
        String ics = getIcsContent(saved.getId());
        emailOutboxService.enqueue(user, title, body, "{\"notificationType\":\"BOOKING_CONFIRMATION\",\"ics\":\"" + ics.replace("\n", "\\n") + "\"}");

        return dtoMapper.toBooking(saved);
    }

    @Transactional
    public BookingResponse updateBooking(Long bookingId, Long actorUserId, BookingUpsertRequest request) {
        ReservationEntity current = getBookingEntity(bookingId);
        RoomEntity room = roomService.getRoomEntity(request.roomId());
        UserEntity actor = userService.getById(actorUserId);

        bookingValidationService.validate(current.getUsuario(), room, request, bookingId);

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
            "Reserva modificada",
            emailTemplateService.bookingStatus(saved, "Reserva modificada", "editada", "Antes: " + previous + " | Ahora: " + next),
            "{\"notificationType\":\"BOOKING_UPDATE\"}"
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
        if (!booking.getFecha().atTime(booking.getHoraFin()).isAfter(AppTime.nowDateTime())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "No se puede cancelar una reserva que ya finalizo");
        }

        booking.setEstado(ReservationStatus.CANCELADA);
        booking.setUpdatedBy(actorUserId);
        booking.setActualizadaEn(OffsetDateTime.now());

        ReservationEntity saved = reservationRepository.save(booking);
        String reason = adminCancel ? "cancelada por administrador" : "cancelada por usuario";
        emailOutboxService.enqueue(
            saved.getUsuario(),
            "Reserva cancelada",
            emailTemplateService.bookingStatus(saved, "Reserva cancelada", "cancelada", "Estado: " + reason + ". Puedes reservar nuevamente."),
            "{\"notificationType\":\"BOOKING_CANCELLATION\"}"
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
    public List<BookingResponse> listRoomBookings(Long roomId, LocalDate fromDate, LocalDate toDate) {
        RoomEntity room = roomService.getRoomEntity(roomId);
        LocalDate start = fromDate == null ? AppTime.today() : fromDate;
        LocalDate end = toDate == null ? start : toDate;
        if (end.isBefore(start)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Rango de fechas inválido para listar reservas");
        }

        return reservationRepository.findActiveByRoomAndDateRange(room, start, end)
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

        Page<ReservationEntity> bookingPage;
        if (parsedStatus != null && date != null) {
            bookingPage = reservationRepository.findByEstadoAndFechaOrderByHoraInicioDesc(parsedStatus, date, PageRequest.of(page, size));
        } else if (parsedStatus != null) {
            bookingPage = reservationRepository.findByEstadoOrderByFechaDescHoraInicioDesc(parsedStatus, PageRequest.of(page, size));
        } else if (date != null) {
            bookingPage = reservationRepository.findByFechaOrderByHoraInicioDesc(date, PageRequest.of(page, size));
        } else {
            bookingPage = reservationRepository.findAllByOrderByFechaDescHoraInicioDesc(PageRequest.of(page, size));
        }
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
        return createIcsContent(reservation);
    }

    @Transactional(readOnly = true)
    public String getStudentIcsContent(Long bookingId, Long studentUserId) {
        UserEntity student = userService.getById(studentUserId);
        if (!"ESTUDIANTE".equalsIgnoreCase(student.getRol().getNombre())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Solo estudiantes pueden exportar reservas");
        }

        ReservationEntity reservation = getBookingEntity(bookingId);
        if (!reservation.getUsuario().getId().equals(studentUserId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Solo puedes exportar tus propias reservas");
        }
        if (reservation.getEstado() != ReservationStatus.ACTIVA) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Solo se pueden exportar reservas confirmadas");
        }

        return createIcsContent(reservation);
    }

    private String createIcsContent(ReservationEntity reservation) {
        return CalendarUtils.createIcs(
            "Reserva - " + reservation.getSala().getNombre(),
            "Reserva Luistudio",
            reservation.getSala().getUbicacion(),
            reservation.getFecha(),
            reservation.getHoraInicio(),
            reservation.getHoraFin()
        );
    }

    private ReservationEntity buildActiveReservation(
        UserEntity user,
        RoomEntity room,
        BookingUpsertRequest request
    ) {
        ReservationEntity booking = new ReservationEntity();
        booking.setUsuario(user);
        booking.setSala(room);
        booking.setFecha(request.date());
        booking.setHoraInicio(request.start());
        booking.setHoraFin(request.end());
        booking.setCantidadPersonas(request.people());
        booking.setObservacion(request.observation());
        booking.setEstado(ReservationStatus.ACTIVA);
        return booking;
    }
}
