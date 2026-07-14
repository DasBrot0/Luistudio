package com.luistudio.reservas.controller;

import com.luistudio.reservas.dto.account.ActivityEventResponse;
import com.luistudio.reservas.dto.common.PageResponse;
import com.luistudio.reservas.dto.room.AvailabilitySubscriptionListResponse;
import com.luistudio.reservas.model.AuditLogEntity;
import com.luistudio.reservas.model.UserEntity;
import com.luistudio.reservas.repository.AuditLogRepository;
import com.luistudio.reservas.security.AuthPrincipal;
import com.luistudio.reservas.service.AccessGuard;
import com.luistudio.reservas.service.AvailabilitySubscriptionService;
import com.luistudio.reservas.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/me")
public class AccountController {

    private static final List<String> ACTIVITY_ACTIONS = List.of(
        "LOGIN_SUCCESS",
        "LOGOUT_CURRENT",
        "LOGOUT_REMOTE",
        "LOGOUT_ALL",
        "SENSITIVE_CHANGE_CONFIRMED",
        "LOGIN_UNUSUAL_ACCESS"
    );

    private final AccessGuard accessGuard;
    private final UserService userService;
    private final AuditLogRepository auditLogRepository;
    private final AvailabilitySubscriptionService subscriptionService;

    public AccountController(
        AccessGuard accessGuard,
        UserService userService,
        AuditLogRepository auditLogRepository,
        AvailabilitySubscriptionService subscriptionService
    ) {
        this.accessGuard = accessGuard;
        this.userService = userService;
        this.auditLogRepository = auditLogRepository;
        this.subscriptionService = subscriptionService;
    }

    @GetMapping("/activity")
    public PageResponse<ActivityEventResponse> getMyActivity(
        @RequestParam(required = false) String from,
        @RequestParam(required = false) String to,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        AuthPrincipal principal = accessGuard.requireUser();
        UserEntity user = userService.getById(principal.userId());
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        OffsetDateTime fromDt = from != null && !from.isBlank() ? OffsetDateTime.parse(from) : null;
        OffsetDateTime toDt = to != null && !to.isBlank() ? OffsetDateTime.parse(to) : null;
        PageRequest pageable = PageRequest.of(safePage, safeSize);
        Page<AuditLogEntity> result = findActivity(user, fromDt, toDt, pageable);
        return new PageResponse<>(
            result.getContent().stream().map(a -> new ActivityEventResponse(
                a.getId(),
                a.getAccion(),
                a.getDetalle(),
                a.getCreadoEn()
            )).toList(),
            result.getNumber(),
            result.getSize(),
            result.getTotalElements(),
            result.getTotalPages()
        );
    }

    private Page<AuditLogEntity> findActivity(
        UserEntity user,
        OffsetDateTime from,
        OffsetDateTime to,
        PageRequest pageable
    ) {
        if (from == null && to == null) {
            return auditLogRepository.findByActorAndAccionInOrderByCreadoEnDesc(user, ACTIVITY_ACTIONS, pageable);
        }
        if (from == null) {
            return auditLogRepository.findByActorAndAccionInAndCreadoEnLessThanEqualOrderByCreadoEnDesc(user, ACTIVITY_ACTIONS, to, pageable);
        }
        if (to == null) {
            return auditLogRepository.findByActorAndAccionInAndCreadoEnGreaterThanEqualOrderByCreadoEnDesc(user, ACTIVITY_ACTIONS, from, pageable);
        }
        return auditLogRepository.findByActorAndAccionInAndCreadoEnBetweenOrderByCreadoEnDesc(user, ACTIVITY_ACTIONS, from, to, pageable);
    }

    @GetMapping("/availability-subscriptions")
    public AvailabilitySubscriptionListResponse getMySubscriptions() {
        AuthPrincipal principal = accessGuard.requireUser();
        return subscriptionService.getMySubscriptions(principal.userId());
    }
}
