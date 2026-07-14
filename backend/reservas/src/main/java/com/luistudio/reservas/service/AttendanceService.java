package com.luistudio.reservas.service;

import com.luistudio.reservas.dto.admin.AdminAttendanceResponse;
import com.luistudio.reservas.dto.admin.AttendanceStatusUpdateRequest;
import com.luistudio.reservas.dto.common.PageResponse;
import com.luistudio.reservas.exception.BusinessException;
import com.luistudio.reservas.exception.NotFoundException;
import com.luistudio.reservas.model.AttendanceRecordEntity;
import com.luistudio.reservas.model.ReservationEntity;
import com.luistudio.reservas.model.ReservationStatus;
import com.luistudio.reservas.model.UserEntity;
import com.luistudio.reservas.repository.AttendanceRecordRepository;
import com.luistudio.reservas.repository.ReservationRepository;
import com.luistudio.reservas.service.email.EmailTemplateService;
import com.luistudio.reservas.util.AppTime;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.criteria.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AttendanceService {

    private static final Logger log = LoggerFactory.getLogger(AttendanceService.class);
    private static final int TOLERANCE_MINUTES = 15;

    private final ReservationRepository reservationRepository;
    private final AttendanceRecordRepository attendanceRecordRepository;
    private final EmailOutboxService emailOutboxService;
    private final EmailTemplateService emailTemplateService;
    private final UserService userService;
    private final AuditService auditService;

    public AttendanceService(
        ReservationRepository reservationRepository,
        AttendanceRecordRepository attendanceRecordRepository,
        EmailOutboxService emailOutboxService,
        EmailTemplateService emailTemplateService,
        UserService userService,
        AuditService auditService
    ) {
        this.reservationRepository = reservationRepository;
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.emailOutboxService = emailOutboxService;
        this.emailTemplateService = emailTemplateService;
        this.userService = userService;
        this.auditService = auditService;
    }

    @Transactional
    public void processMissedBookings() {
        LocalDateTime cutoffDateTime = AppTime.nowDateTime().minusMinutes(TOLERANCE_MINUTES);
        LocalDate cutoffDate = cutoffDateTime.toLocalDate();
        LocalTime cutoffTime = cutoffDateTime.toLocalTime();

        List<ReservationEntity> missed = reservationRepository.findActiveBookingsMissedBefore(cutoffDate, cutoffTime);
        log.info("attendance_scheduler_run cutoffDate={} cutoffTime={} candidates={}", cutoffDate, cutoffTime, missed.size());

        for (ReservationEntity booking : missed) {
            booking.setAttendanceStatus("INASISTIO");
            reservationRepository.save(booking);
            saveAttendanceRecord(booking, "INASISTIO", null, TOLERANCE_MINUTES);

            emailOutboxService.enqueue(
                booking.getUsuario(),
                "Inasistencia registrada",
                emailTemplateService.absenceNotice(booking),
                "{\"notificationType\":\"ABSENCE_NOTICE\"}"
            );
            log.info("attendance_marked_absent bookingId={}", booking.getId());
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminAttendanceResponse> listAttendance(
        int page,
        int size,
        String search,
        String campus,
        String pavilion,
        String status,
        LocalDate from,
        LocalDate to,
        String sortBy,
        String sortDir
    ) {
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "La fecha desde no puede ser posterior a la fecha hasta");
        }
        int safePage = Math.max(0, page);
        int safeSize = Math.min(50, Math.max(1, size));
        String normalizedStatus = normalizeOptionalStatus(status);
        Specification<ReservationEntity> specification = (root, criteriaQuery, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            var room = root.join("sala");
            var building = room.join("pabellon");
            var campusJoin = building.join("campus");
            var user = root.join("usuario");

            predicates.add(cb.notEqual(root.get("estado"), ReservationStatus.CANCELADA));
            if (search != null && !search.isBlank()) {
                String value = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(user.get("codigo")), value),
                    cb.like(cb.lower(user.get("nombres")), value),
                    cb.like(cb.lower(user.get("apellidos")), value),
                    cb.like(cb.lower(user.get("correo")), value),
                    cb.like(cb.lower(room.get("codigo")), value),
                    cb.like(cb.lower(room.get("nombre")), value)
                ));
            }
            if (campus != null && !campus.isBlank()) {
                predicates.add(cb.equal(cb.lower(campusJoin.get("nombre")), campus.trim().toLowerCase()));
            }
            if (pavilion != null && !pavilion.isBlank()) {
                String value = pavilion.trim().toLowerCase();
                predicates.add(cb.or(
                    cb.equal(cb.lower(building.get("codigo")), value),
                    cb.equal(cb.lower(building.get("nombre")), value)
                ));
            }
            if ("PENDIENTE".equals(normalizedStatus)) {
                predicates.add(cb.isNull(root.get("attendanceStatus")));
            } else if (normalizedStatus != null) {
                predicates.add(cb.equal(root.get("attendanceStatus"), normalizedStatus));
            }
            if (from != null) predicates.add(cb.greaterThanOrEqualTo(root.get("fecha"), from));
            if (to != null) predicates.add(cb.lessThanOrEqualTo(root.get("fecha"), to));
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Sort.Direction direction = "asc".equalsIgnoreCase(sortDir) ? Sort.Direction.ASC : Sort.Direction.DESC;
        String property = switch (sortBy == null ? "" : sortBy.toLowerCase()) {
            case "student" -> "usuario.codigo";
            case "pavilion" -> "sala.pabellon.codigo";
            case "room" -> "sala.nombre";
            case "status" -> "attendanceStatus";
            default -> "fecha";
        };
        Sort sort = Sort.by(new Sort.Order(direction, property).nullsLast())
            .and(Sort.by(direction, "horaInicio"))
            .and(Sort.by(Sort.Direction.ASC, "id"));
        Page<ReservationEntity> result = reservationRepository.findAll(
            specification,
            PageRequest.of(safePage, safeSize, sort)
        );
        return new PageResponse<>(
            result.getContent().stream().map(this::toAdminResponse).toList(),
            result.getNumber(),
            result.getSize(),
            result.getTotalElements(),
            result.getTotalPages()
        );
    }

    @Transactional
    public AdminAttendanceResponse updateAttendance(Long bookingId, Long actorUserId, AttendanceStatusUpdateRequest request) {
        ReservationEntity booking = reservationRepository.findById(bookingId)
            .orElseThrow(() -> new NotFoundException("Reserva no encontrada"));
        if (booking.getEstado() == ReservationStatus.CANCELADA) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "No se puede registrar asistencia en una reserva cancelada");
        }
        if (booking.getFecha().atTime(booking.getHoraInicio()).isAfter(AppTime.nowDateTime())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "La reserva todavía no ha iniciado");
        }

        String nextStatus = request.status().trim().toUpperCase();
        String previousStatus = booking.getAttendanceStatus();
        UserEntity actor = userService.getById(actorUserId);
        booking.setAttendanceStatus(nextStatus);
        booking.setUpdatedBy(actorUserId);
        booking.setActualizadaEn(OffsetDateTime.now());
        reservationRepository.save(booking);
        saveAttendanceRecord(booking, nextStatus, actor, 0);

        if ("INASISTIO".equals(nextStatus) && !"INASISTIO".equals(previousStatus)) {
            emailOutboxService.enqueue(
                booking.getUsuario(),
                "Inasistencia registrada",
                emailTemplateService.absenceNotice(booking),
                "{\"notificationType\":\"ABSENCE_NOTICE\"}"
            );
        }
        auditService.record(
            actor,
            "ATTENDANCE_UPDATED",
            "reserva",
            String.valueOf(bookingId),
            "from=" + (previousStatus == null ? "PENDIENTE" : previousStatus) + ";to=" + nextStatus
        );
        return toAdminResponse(booking);
    }

    private void saveAttendanceRecord(
        ReservationEntity booking,
        String status,
        UserEntity recordedBy,
        int toleranceMinutes
    ) {
        AttendanceRecordEntity record = attendanceRecordRepository.findByReserva(booking)
            .orElseGet(AttendanceRecordEntity::new);
        record.setReserva(booking);
        record.setUsuario(booking.getUsuario());
        record.setStatus(status);
        record.setRecordedBy(recordedBy);
        record.setRecordedAt(OffsetDateTime.now());
        record.setToleranceMinutes(toleranceMinutes);
        attendanceRecordRepository.save(record);
    }

    private String normalizeOptionalStatus(String status) {
        if (status == null || status.isBlank() || "TODOS".equalsIgnoreCase(status)) return null;
        String normalized = status.trim().toUpperCase();
        if (!List.of("PENDIENTE", "ASISTIO", "INASISTIO").contains(normalized)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Estado de asistencia inválido");
        }
        return normalized;
    }

    private AdminAttendanceResponse toAdminResponse(ReservationEntity booking) {
        var user = booking.getUsuario();
        var room = booking.getSala();
        var building = room.getPabellon();
        return new AdminAttendanceResponse(
            booking.getId(),
            user.getId(),
            user.getCodigo(),
            user.getNombres() + " " + user.getApellidos(),
            user.getCorreo(),
            room.getId(),
            room.getCodigo(),
            room.getNombre(),
            building.getCampus().getNombre(),
            building.getCodigo(),
            building.getNombre(),
            room.getUbicacion(),
            booking.getFecha(),
            booking.getHoraInicio(),
            booking.getHoraFin(),
            booking.getEstado().name(),
            booking.getAttendanceStatus() == null ? "PENDIENTE" : booking.getAttendanceStatus()
        );
    }
}
