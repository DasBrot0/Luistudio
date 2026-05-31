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
import com.luistudio.reservas.util.CalendarUtils;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BookingService {

    private final ReservationRepository reservationRepository;
    private final RoomService roomService;
    private final UserService userService;
    private final SystemConfigService systemConfigService;
    private final EmailOutboxService emailOutboxService;
    private final AuditService auditService;
    private final DtoMapper dtoMapper;

    public BookingService(
        ReservationRepository reservationRepository,
        RoomService roomService,
        UserService userService,
        SystemConfigService systemConfigService,
        EmailOutboxService emailOutboxService,
        AuditService auditService,
        DtoMapper dtoMapper
    ) {
        this.reservationRepository = reservationRepository;
        this.roomService = roomService;
        this.userService = userService;
        this.systemConfigService = systemConfigService;
        this.emailOutboxService = emailOutboxService;
        this.auditService = auditService;
        this.dtoMapper = dtoMapper;
    }

    @Transactional
    public BookingResponse createBooking(Long userId, BookingUpsertRequest request) {
        UserEntity user = userService.getById(userId);
        RoomEntity room = roomService.getRoomEntity(request.roomId());

        validateBookingRules(user, room, request, null);

        ReservationEntity booking = new ReservationEntity();
        booking.setUsuario(user);
        booking.setSala(room);
        booking.setFecha(request.date());
        booking.setHoraInicio(request.start());
        booking.setHoraFin(request.end());
        booking.setCantidadPersonas(request.people());
        booking.setObservacion(request.observation());
        booking.setEstado(ReservationStatus.ACTIVA);

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
        if (!request.end().isAfter(request.start())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "La hora fin debe ser mayor a la hora inicio");
        }

        if (request.people() > room.getCapacidad()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "La cantidad de personas supera la capacidad de la sala");
        }

        int durationMinutes = (int) Duration.between(request.start(), request.end()).toMinutes();
        if (durationMinutes > systemConfigService.getMaxDurationMinutes()) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "La duracion maxima permitida es " + systemConfigService.getMaxDurationMinutes() + " minutos"
            );
        }

        if (!roomService.isRoomAvailable(room, request.date(), request.start(), request.end(), excludeBookingId)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "La sala no esta disponible para el horario seleccionado");
        }

        long activeCount = reservationRepository.countCurrentActiveForUser(user, LocalDate.now(), LocalTime.now());
        if (excludeBookingId == null && activeCount >= systemConfigService.getMaxActiveBookings()) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "Alcanzaste el limite de reservas activas (" + systemConfigService.getMaxActiveBookings() + ")"
            );
        }
    }

    @Transactional
    @Scheduled(fixedDelay = 300000)
    public void sendReminderEmails() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        OffsetDateTime nextHour = OffsetDateTime.now(ZoneOffset.ofHours(-5)).plusMinutes(60);

        List<ReservationEntity> upcoming = reservationRepository.findUpcomingWindow(
            today,
            now,
            nextHour.toLocalDate(),
            nextHour.toLocalTime()
        );

        for (ReservationEntity booking : upcoming) {
            emailOutboxService.enqueue(
                booking.getUsuario(),
                "Recordatorio de reserva #" + booking.getId(),
                "Tu reserva inicia en menos de 60 minutos en sala " + booking.getSala().getNombre(),
                null
            );
        }
    }

    @Transactional
    @Scheduled(fixedDelay = 300000)
    public void sendEndingSoonReminders() {
        LocalDate date = LocalDate.now();
        LocalTime now = LocalTime.now();
        LocalTime min = now.minusMinutes(15);

        List<ReservationEntity> endingSoon = reservationRepository.findEndingSoon(date, now.plusMinutes(15), min);
        for (ReservationEntity booking : endingSoon) {
            emailOutboxService.enqueue(
                booking.getUsuario(),
                "Tu reserva termina pronto #" + booking.getId(),
                "Tu reserva termina en menos de 15 minutos.",
                null
            );
        }
    }
}
