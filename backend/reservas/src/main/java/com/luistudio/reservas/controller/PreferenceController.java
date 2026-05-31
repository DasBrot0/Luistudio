package com.luistudio.reservas.controller;

import com.luistudio.reservas.dto.user.NotificationPreferencesResponse;
import com.luistudio.reservas.dto.user.NotificationPreferencesUpdateRequest;
import com.luistudio.reservas.security.AuthPrincipal;
import com.luistudio.reservas.service.AccessGuard;
import com.luistudio.reservas.service.PreferenceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class PreferenceController {

    private final AccessGuard accessGuard;
    private final PreferenceService preferenceService;

    public PreferenceController(AccessGuard accessGuard, PreferenceService preferenceService) {
        this.accessGuard = accessGuard;
        this.preferenceService = preferenceService;
    }

    @GetMapping("/preferences")
    public NotificationPreferencesResponse getPreferences() {
        AuthPrincipal principal = accessGuard.requireUser();
        return preferenceService.getPreferences(principal.userId());
    }

    @PutMapping("/preferences")
    public NotificationPreferencesResponse updatePreferences(@RequestBody NotificationPreferencesUpdateRequest request) {
        AuthPrincipal principal = accessGuard.requireUser();
        return preferenceService.updatePreferences(principal.userId(), request);
    }
}
