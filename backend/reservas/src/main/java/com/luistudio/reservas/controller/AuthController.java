package com.luistudio.reservas.controller;

import com.luistudio.reservas.dto.auth.AuthUserResponse;
import com.luistudio.reservas.dto.auth.LoginRequest;
import com.luistudio.reservas.dto.auth.LoginResponse;
import com.luistudio.reservas.dto.auth.ResetConfirmInput;
import com.luistudio.reservas.dto.auth.ResetRequestInput;
import com.luistudio.reservas.dto.auth.TwoFactorCodeInput;
import com.luistudio.reservas.dto.auth.TwoFactorVerifyInput;
import com.luistudio.reservas.dto.common.MessageResponse;
import com.luistudio.reservas.security.AuthPrincipal;
import com.luistudio.reservas.security.CurrentUserProvider;
import com.luistudio.reservas.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final CurrentUserProvider currentUserProvider;

    public AuthController(AuthService authService, CurrentUserProvider currentUserProvider) {
        this.authService = authService;
        this.currentUserProvider = currentUserProvider;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpServletRequest) {
        return authService.login(request, httpServletRequest.getRemoteAddr());
    }

    @PostMapping("/2fa/verify")
    public LoginResponse verify2fa(@Valid @RequestBody TwoFactorVerifyInput request) {
        return authService.verify2fa(request);
    }

    @GetMapping("/me")
    public AuthUserResponse me() {
        AuthPrincipal principal = currentUserProvider.requireCurrentUser();
        currentUserProvider.requireNotProvisionalToken();
        return authService.me(principal.userId());
    }

    @PostMapping("/reset-request")
    public ResponseEntity<MessageResponse> requestReset(@Valid @RequestBody ResetRequestInput request) {
        authService.requestReset(request);
        return ResponseEntity.ok(new MessageResponse("Si el correo existe, se envió un enlace de recuperación"));
    }

    @PostMapping("/reset-confirm")
    public ResponseEntity<MessageResponse> confirmReset(@Valid @RequestBody ResetConfirmInput request) {
        authService.confirmReset(request);
        return ResponseEntity.ok(new MessageResponse("Contraseña actualizada"));
    }

    @PostMapping("/2fa/enroll")
    public ResponseEntity<MessageResponse> enroll2fa() {
        AuthPrincipal principal = currentUserProvider.requireCurrentUser();
        currentUserProvider.requireNotProvisionalToken();
        authService.enroll2fa(principal.userId());
        return ResponseEntity.ok(new MessageResponse("Código 2FA enviado"));
    }

    @PostMapping("/2fa/confirm")
    public ResponseEntity<MessageResponse> confirm2fa(@Valid @RequestBody TwoFactorCodeInput request) {
        AuthPrincipal principal = currentUserProvider.requireCurrentUser();
        currentUserProvider.requireNotProvisionalToken();
        authService.verify2faEnrollment(principal.userId(), request);
        return ResponseEntity.ok(new MessageResponse("2FA activado"));
    }

    @PostMapping("/2fa/disable")
    public ResponseEntity<MessageResponse> disable2fa() {
        AuthPrincipal principal = currentUserProvider.requireCurrentUser();
        currentUserProvider.requireNotProvisionalToken();
        authService.disable2fa(principal.userId());
        return ResponseEntity.ok(new MessageResponse("Código de confirmación enviado para desactivar 2FA"));
    }

    @PostMapping("/2fa/disable/confirm")
    public ResponseEntity<MessageResponse> confirmDisable2fa(@Valid @RequestBody TwoFactorCodeInput request) {
        AuthPrincipal principal = currentUserProvider.requireCurrentUser();
        currentUserProvider.requireNotProvisionalToken();
        authService.confirmDisable2fa(principal.userId(), request);
        return ResponseEntity.ok(new MessageResponse("2FA desactivado"));
    }
}
