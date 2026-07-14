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
import com.luistudio.reservas.util.RoomLocationFormatter;
import com.luistudio.reservas.util.CalendarUtils;
import org.springframework.context.annotation.Lazy;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {
    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final ReservationRepository reservationRepository;
    private final RoomService roomService;
    private final UserService userService;
    private final EmailOutboxService emailOutboxService;
    private final AuditService auditService;
    private final DtoMapper dtoMapper;
    private final BookingValidationService bookingValidationService;
    private final EmailTemplateService emailTemplateService;
    private final AvailabilitySubscriptionService availabilitySubscriptionService;

    public BookingService(
        ReservationRepository reservationRepository,
        RoomService roomService,
        UserService userService,
        EmailOutboxService emailOutboxService,
        AuditService auditService,
        DtoMapper dtoMapper,
        BookingValidationService bookingValidationService,
        EmailTemplateService emailTemplateService,
        @Lazy AvailabilitySubscriptionService availabilitySubscriptionService
    ) {
        this.reservationRepository = reservationRepository;
        this.roomService = roomService;
        this.userService = userService;
        this.emailOutboxService = emailOutboxService;
        this.auditService = auditService;
        this.dtoMapper = dtoMapper;
        this.bookingValidationService = bookingValidationService;
        this.emailTemplateService = emailTemplateService;
        this.availabilitySubscriptionService = availabilitySubscriptionService;
    }

    @Transactional
    public BookingResponse createBooking(Long userId, BookingUpsertRequest request) {
        UserEntity user = userService.getById(userId);
        RoomEntity room = roomService.getRoomEntity(request.roomId());
        roomService.lockRoomInventory(room.getId());
        log.info(
            "booking_create_started roomId={} date={} start={} durationMinutes={}",
            room.getId(),
            request.date(),
            request.start(),
            durationMinutes(request)
        );

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
                existing.setNumeroUnidad(resolveAvailableUnit(room, request, existing.getId(), existing.getNumeroUnidad()));
                existing.setUpdatedBy(userId);
                existing.setActualizadaEn(OffsetDateTime.now());
                return reservationRepository.save(existing);
            })
            .orElseGet(() -> {
                bookingValidationService.validate(user, room, request, null);
                ReservationEntity booking = buildActiveReservation(
                    user,
                    room,
                    request,
                    resolveAvailableUnit(room, request, null, null)
                );
                return reservationRepository.save(booking);
            });

        String title = "Reserva confirmada";
        String body = emailTemplateService.bookingStatus(saved, title, "confirmada", null);
        String ics = getIcsContent(saved.getId());
        emailOutboxService.enqueue(user, title, body, "{\"notificationType\":\"BOOKING_CONFIRMATION\",\"ics\":\"" + ics.replace("\n", "\\n") + "\"}");
        log.info(
            "booking_create_completed bookingId={} roomId={} status={} date={} start={} durationMinutes={}",
            saved.getId(),
            saved.getSala().getId(),
            saved.getEstado(),
            saved.getFecha(),
            saved.getHoraInicio(),
            Duration.between(saved.getHoraInicio(), saved.getHoraFin()).toMinutes()
        );

        return dtoMapper.toBooking(saved);
    }

    @Transactional
    public BookingResponse updateBooking(Long bookingId, Long actorUserId, BookingUpsertRequest request) {
        ReservationEntity current = getBookingEntity(bookingId);
        RoomEntity previousRoom = current.getSala();
        LocalDate previousDate = current.getFecha();
        LocalTime previousStart = current.getHoraInicio();
        LocalTime previousEnd = current.getHoraFin();
        RoomEntity room = roomService.getRoomEntity(request.roomId());
        roomService.lockRoomInventory(room.getId());
        UserEntity actor = userService.getById(actorUserId);
        log.info(
            "booking_update_started bookingId={} roomId={} date={} start={} durationMinutes={}",
            bookingId,
            room.getId(),
            request.date(),
            request.start(),
            durationMinutes(request)
        );

        bookingValidationService.validate(current.getUsuario(), room, request, bookingId);

        String previous = current.getSala().getCodigo() + " " + current.getFecha() + " " + current.getHoraInicio() + "-" + current.getHoraFin();

        current.setSala(room);
        current.setFecha(request.date());
        current.setHoraInicio(request.start());
        current.setHoraFin(request.end());
        current.setCantidadPersonas(request.people());
        current.setObservacion(request.observation());
        current.setNumeroUnidad(resolveAvailableUnit(room, request, bookingId, current.getNumeroUnidad()));
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
        boolean releasedPreviousSlot = !previousRoom.getId().equals(saved.getSala().getId())
            || !previousDate.equals(saved.getFecha())
            || !previousStart.equals(saved.getHoraInicio())
            || !previousEnd.equals(saved.getHoraFin());
        if (releasedPreviousSlot) {
            availabilitySubscriptionService.notifySubscribers(previousRoom, previousDate, previousStart, previousEnd);
        }
        log.info(
            "booking_update_completed bookingId={} roomId={} status={} date={} start={} durationMinutes={}",
            saved.getId(),
            saved.getSala().getId(),
            saved.getEstado(),
            saved.getFecha(),
            saved.getHoraInicio(),
            Duration.between(saved.getHoraInicio(), saved.getHoraFin()).toMinutes()
        );

        return dtoMapper.toBooking(saved);
    }

    @Transactional
    public BookingResponse cancelBooking(Long bookingId, Long actorUserId, boolean adminCancel) {
        ReservationEntity booking = getBookingEntity(bookingId);
        log.info(
            "booking_cancel_started bookingId={} roomId={} status={} adminCancel={}",
            booking.getId(),
            booking.getSala().getId(),
            booking.getEstado(),
            adminCancel
        );
        if (booking.getEstado() == ReservationStatus.CANCELADA) {
            log.info("booking_cancel_skipped bookingId={} status={}", booking.getId(), booking.getEstado());
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
        // Notify availability subscribers
        availabilitySubscriptionService.notifySubscribers(
            saved.getSala(), saved.getFecha(), saved.getHoraInicio(), saved.getHoraFin()
        );
        log.info(
            "booking_cancel_completed bookingId={} roomId={} status={} date={} start={} durationMinutes={}",
            saved.getId(),
            saved.getSala().getId(),
            saved.getEstado(),
            saved.getFecha(),
            saved.getHoraInicio(),
            Duration.between(saved.getHoraInicio(), saved.getHoraFin()).toMinutes()
        );
        return dtoMapper.toBooking(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> listMyBookings(Long userId, int page, int size) {
        UserEntity user = userService.getById(userId);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        Page<ReservationEntity> bookingPage = reservationRepository.findByUsuarioOrderByFechaDescHoraInicioDesc(
            user,
            PageRequest.of(safePage, safeSize)
        );
        return new PageResponse<>(
            bookingPage.getContent().stream().map(dtoMapper::toBooking).toList(),
            bookingPage.getNumber(),
            bookingPage.getSize(),
            bookingPage.getTotalElements(),
            bookingPage.getTotalPages()
        );
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
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        ReservationStatus parsedStatus = null;
        if (status != null && !status.isBlank()) {
            parsedStatus = ReservationStatus.valueOf(status.toUpperCase());
        }

        Page<ReservationEntity> bookingPage;
        if (parsedStatus != null && date != null) {
            bookingPage = reservationRepository.findByEstadoAndFechaOrderByHoraInicioDesc(parsedStatus, date, PageRequest.of(safePage, safeSize));
        } else if (parsedStatus != null) {
            bookingPage = reservationRepository.findByEstadoOrderByFechaDescHoraInicioDesc(parsedStatus, PageRequest.of(safePage, safeSize));
        } else if (date != null) {
            bookingPage = reservationRepository.findByFechaOrderByHoraInicioDesc(date, PageRequest.of(safePage, safeSize));
        } else {
            bookingPage = reservationRepository.findAllByOrderByFechaDescHoraInicioDesc(PageRequest.of(safePage, safeSize));
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
        RoomEntity room = reservation.getSala();
        return CalendarUtils.createIcs(
            "booking-" + reservation.getId(),
            "Reserva - " + room.getNombre(),
            "Reserva Luistudio",
            RoomLocationFormatter.format(room),
            RoomLocationFormatter.latitude(room),
            RoomLocationFormatter.longitude(room),
            reservation.getFecha(),
            reservation.getHoraInicio(),
            reservation.getHoraFin()
        );
    }

    private ReservationEntity buildActiveReservation(
        UserEntity user,
        RoomEntity room,
        BookingUpsertRequest request,
        Integer roomUnitNumber
    ) {
        ReservationEntity booking = new ReservationEntity();
        booking.setUsuario(user);
        booking.setSala(room);
        booking.setFecha(request.date());
        booking.setHoraInicio(request.start());
        booking.setHoraFin(request.end());
        booking.setCantidadPersonas(request.people());
        booking.setObservacion(request.observation());
        booking.setNumeroUnidad(roomUnitNumber);
        booking.setEstado(ReservationStatus.ACTIVA);
        return booking;
    }

    private int resolveAvailableUnit(
        RoomEntity room,
        BookingUpsertRequest request,
        Long excludeBookingId,
        Integer preferredUnit
    ) {
        int inventoryCount = room.getCantidadUnidades() == null ? 1 : room.getCantidadUnidades();
        List<Integer> occupiedNumbers = reservationRepository.findOccupiedUnitNumbers(
            room,
            request.date(),
            request.start(),
            request.end(),
            excludeBookingId
        );
        Set<Integer> occupied = occupiedNumbers == null ? new HashSet<>() : new HashSet<>(occupiedNumbers);
        if (preferredUnit != null && preferredUnit <= inventoryCount && !occupied.contains(preferredUnit)) {
            return preferredUnit;
        }
        for (int unit = 1; unit <= inventoryCount; unit++) {
            if (!occupied.contains(unit)) {
                return unit;
            }
        }
        throw new BusinessException(HttpStatus.BAD_REQUEST, "Se agotaron todas las unidades de esta sala para el horario seleccionado");
    }

    private long durationMinutes(BookingUpsertRequest request) {
        return Duration.between(request.start(), request.end()).toMinutes();
    }
}
