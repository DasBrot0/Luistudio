package com.luistudio.reservas.controller;

import com.luistudio.reservas.dto.common.MessageResponse;
import com.luistudio.reservas.dto.session.SessionListResponse;
import com.luistudio.reservas.security.AuthPrincipal;
import com.luistudio.reservas.security.CurrentUserProvider;
import com.luistudio.reservas.service.AccessGuard;
import com.luistudio.reservas.service.SessionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/sessions")
public class SessionController {

    private final AccessGuard accessGuard;
    private final SessionService sessionService;
    private final CurrentUserProvider currentUserProvider;

    public SessionController(
        AccessGuard accessGuard,
        SessionService sessionService,
        CurrentUserProvider currentUserProvider
    ) {
        this.accessGuard = accessGuard;
        this.sessionService = sessionService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping
    public SessionListResponse getSessions() {
        AuthPrincipal principal = accessGuard.requireUser();
        String currentJti = currentUserProvider.currentJti();
        return sessionService.getMySessions(principal.userId(), currentJti);
    }

    @DeleteMapping("/{sessionId}")
    public ResponseEntity<MessageResponse> revokeSession(@PathVariable Long sessionId) {
        AuthPrincipal principal = accessGuard.requireUser();
        sessionService.revokeSession(principal.userId(), sessionId);
        return ResponseEntity.ok(new MessageResponse("Sesión revocada"));
    }

    @DeleteMapping
    public ResponseEntity<MessageResponse> revokeAllSessions() {
        AuthPrincipal principal = accessGuard.requireUser();
        sessionService.revokeAllSessions(principal.userId());
        return ResponseEntity.ok(new MessageResponse("Todas las sesiones revocadas"));
    }
}
