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

@RestController
@RequestMapping("/api/me")
public class AccountController {

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
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size
    ) {
        AuthPrincipal principal = accessGuard.requireUser();
        UserEntity user = userService.getById(principal.userId());
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        Page<AuditLogEntity> result = auditLogRepository.findActivityByActor(user, PageRequest.of(safePage, safeSize));
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

    @GetMapping("/availability-subscriptions")
    public AvailabilitySubscriptionListResponse getMySubscriptions() {
        AuthPrincipal principal = accessGuard.requireUser();
        return subscriptionService.getMySubscriptions(principal.userId());
    }
}
