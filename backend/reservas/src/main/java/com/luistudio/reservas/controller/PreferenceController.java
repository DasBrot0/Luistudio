package com.luistudio.reservas.controller;

import com.luistudio.reservas.dto.user.NotificationPreferencesResponse;
import com.luistudio.reservas.dto.user.NotificationPreferencesUpdateRequest;
import com.luistudio.reservas.security.AuthPrincipal;
import com.luistudio.reservas.service.AccessGuard;
import com.luistudio.reservas.service.PreferenceService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class PreferenceController {
    private static final Logger log = LoggerFactory.getLogger(PreferenceController.class);

    private final AccessGuard accessGuard;
    private final PreferenceService preferenceService;

    public PreferenceController(AccessGuard accessGuard, PreferenceService preferenceService) {
        this.accessGuard = accessGuard;
        this.preferenceService = preferenceService;
    }

    @GetMapping("/preferences")
    public NotificationPreferencesResponse getPreferences() {
        AuthPrincipal principal = accessGuard.requireUser();
        String userIdHash = hashId(principal.userId());
        log.info("preferences_get_started actorRole={} userIdHash={}", principal.role(), userIdHash);
        NotificationPreferencesResponse response = preferenceService.getPreferences(principal.userId());
        log.info("preferences_get_completed actorRole={} userIdHash={}", principal.role(), userIdHash);
        return response;
    }

    @PutMapping("/preferences")
    public NotificationPreferencesResponse updatePreferences(@Valid @RequestBody NotificationPreferencesUpdateRequest request) {
        AuthPrincipal principal = accessGuard.requireUser();
        String userIdHash = hashId(principal.userId());
        log.info("preferences_update_started actorRole={} userIdHash={}", principal.role(), userIdHash);
        NotificationPreferencesResponse response = preferenceService.updatePreferences(principal.userId(), request);
        log.info("preferences_update_completed actorRole={} userIdHash={}", principal.role(), userIdHash);
        return response;
    }

    private String hashId(Long userId) {
        return userId == null ? "anonymous" : Integer.toHexString(Long.hashCode(userId));
    }
}
