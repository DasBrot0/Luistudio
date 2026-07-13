package com.luistudio.reservas.controller;

import com.luistudio.reservas.dto.admin.AdminConfigResponse;
import com.luistudio.reservas.dto.admin.AdminConfigUpdateRequest;
import com.luistudio.reservas.dto.admin.AdminDashboardResponse;
import com.luistudio.reservas.dto.admin.AnnouncementRequest;
import com.luistudio.reservas.dto.admin.AnnouncementResponse;
import com.luistudio.reservas.dto.admin.CampusScheduleListResponse;
import com.luistudio.reservas.dto.admin.CampusScheduleResponse;
import com.luistudio.reservas.dto.admin.CampusScheduleUpdateRequest;
import com.luistudio.reservas.dto.admin.LoginAttemptAdminResponse;
import com.luistudio.reservas.dto.common.PageResponse;
import com.luistudio.reservas.dto.user.UserResponse;
import com.luistudio.reservas.dto.user.UserStatusUpdateRequest;
import com.luistudio.reservas.model.LoginAttemptEntity;
import com.luistudio.reservas.repository.LoginAttemptRepository;
import com.luistudio.reservas.service.AccessGuard;
import com.luistudio.reservas.service.AnnouncementService;
import com.luistudio.reservas.service.AdminDashboardService;
import com.luistudio.reservas.service.RoomScheduleService;
import com.luistudio.reservas.service.SystemConfigService;
import com.luistudio.reservas.service.UserService;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.Valid;
import java.time.OffsetDateTime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
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
public class AdminController {

    private final AccessGuard accessGuard;
    private final UserService userService;
    private final SystemConfigService systemConfigService;
    private final RoomScheduleService roomScheduleService;
    private final LoginAttemptRepository loginAttemptRepository;
    private final AnnouncementService announcementService;
    private final AdminDashboardService adminDashboardService;

    public AdminController(
        AccessGuard accessGuard,
        UserService userService,
        SystemConfigService systemConfigService,
        RoomScheduleService roomScheduleService,
        LoginAttemptRepository loginAttemptRepository,
        AnnouncementService announcementService,
        AdminDashboardService adminDashboardService
    ) {
        this.accessGuard = accessGuard;
        this.userService = userService;
        this.systemConfigService = systemConfigService;
        this.roomScheduleService = roomScheduleService;
        this.loginAttemptRepository = loginAttemptRepository;
        this.announcementService = announcementService;
        this.adminDashboardService = adminDashboardService;
    }

    @GetMapping("/admin/dashboard")
    public AdminDashboardResponse getDashboard(
        @RequestParam LocalDate from,
        @RequestParam LocalDate to
    ) {
        accessGuard.requireAdmin();
        return adminDashboardService.getDashboard(from, to);
    }

    @GetMapping("/admin/users")
    public PageResponse<UserResponse> listUsers(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) String query,
        @RequestParam(required = false) String year,
        @RequestParam(required = false) String status,
        @RequestParam(defaultValue = "id") String sortBy,
        @RequestParam(defaultValue = "asc") String sortDir
    ) {
        accessGuard.requireAdmin();
        return userService.listUsers(page, size, query, year, status, sortBy, sortDir);
    }

    @PatchMapping("/admin/users/{userId}/estado")
    public UserResponse updateUserStatus(
        @PathVariable Long userId,
        @Valid @RequestBody UserStatusUpdateRequest request
    ) {
        var principal = accessGuard.requireAdmin();
        return userService.updateStatus(userId, principal.userId(), request);
    }

    @GetMapping("/admin/config")
    public AdminConfigResponse getConfig() {
        accessGuard.requireAdmin();
        return systemConfigService.getConfig();
    }

    @PutMapping("/admin/config")
    public AdminConfigResponse updateConfig(@Valid @RequestBody AdminConfigUpdateRequest request) {
        accessGuard.requireAdmin();
        return systemConfigService.updateConfig(request);
    }

    @GetMapping("/admin/campus-schedules")
    public CampusScheduleListResponse getCampusSchedules() {
        accessGuard.requireAdmin();
        return roomScheduleService.listCampusSchedules();
    }

    @PutMapping("/admin/campus-schedules")
    public CampusScheduleResponse updateCampusSchedule(@Valid @RequestBody CampusScheduleUpdateRequest request) {
        accessGuard.requireAdmin();
        return roomScheduleService.updateCampusSchedule(request);
    }

    @GetMapping("/admin/security/login-attempts")
    public PageResponse<LoginAttemptAdminResponse> getLoginAttempts(
        @RequestParam(required = false) String user,
        @RequestParam(required = false) String email,
        @RequestParam(required = false) String from,
        @RequestParam(required = false) String to,
        @RequestParam(required = false) Boolean success,
        @RequestParam(required = false) Boolean blocked,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        accessGuard.requireAdmin();
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        OffsetDateTime fromDt = from != null && !from.isBlank() ? OffsetDateTime.parse(from) : null;
        OffsetDateTime toDt = to != null && !to.isBlank() ? OffsetDateTime.parse(to) : null;

        Specification<LoginAttemptEntity> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            var usuario = root.join("usuario");
            if (query != null && Long.class != query.getResultType()) {
                root.fetch("usuario");
            }
            if (email != null && !email.isBlank()) {
                predicates.add(cb.like(cb.lower(usuario.get("correo")), "%" + email.toLowerCase() + "%"));
            }
            if (user != null && !user.isBlank()) {
                String normalized = "%" + user.toLowerCase() + "%";
                predicates.add(cb.or(
                    cb.like(cb.lower(usuario.get("codigo")), normalized),
                    cb.like(cb.lower(usuario.get("nombres")), normalized),
                    cb.like(cb.lower(usuario.get("apellidos")), normalized)
                ));
            }
            if (fromDt != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("fechaIntento"), fromDt));
            }
            if (toDt != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("fechaIntento"), toDt));
            }
            if (success != null) {
                predicates.add(cb.equal(root.get("exito"), success));
            }
            if (blocked != null) {
                OffsetDateTime now = OffsetDateTime.now();
                predicates.add(blocked
                    ? cb.greaterThan(usuario.get("lockedUntil"), now)
                    : cb.or(cb.isNull(usuario.get("lockedUntil")), cb.lessThanOrEqualTo(usuario.get("lockedUntil"), now)));
            }
            if (query != null) {
                query.orderBy(cb.desc(root.get("fechaIntento")));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<LoginAttemptEntity> result = loginAttemptRepository.findAll(spec, PageRequest.of(safePage, safeSize));
        return new PageResponse<>(
            result.getContent().stream().map(a -> new LoginAttemptAdminResponse(
                a.getId(),
                a.getUsuario().getId(),
                a.getUsuario().getCorreo(),
                a.getIpOrigen(),
                a.getUserAgent(),
                a.getFechaIntento(),
                Boolean.TRUE.equals(a.getExito()),
                a.getUsuario().getLockedUntil()
            )).toList(),
            result.getNumber(),
            result.getSize(),
            result.getTotalElements(),
            result.getTotalPages()
        );
    }

    @PostMapping("/admin/announcements")
    public AnnouncementResponse publishAnnouncement(@Valid @RequestBody AnnouncementRequest request) {
        var principal = accessGuard.requireAdmin();
        return announcementService.publish(principal.userId(), request);
    }
}
