package com.luistudio.reservas.service;

import com.luistudio.reservas.dto.auth.AuthUserResponse;
import com.luistudio.reservas.dto.auth.LoginRequest;
import com.luistudio.reservas.dto.auth.LoginResponse;
import com.luistudio.reservas.dto.auth.ResetConfirmInput;
import com.luistudio.reservas.dto.auth.ResetRequestInput;
import com.luistudio.reservas.dto.auth.TwoFactorCodeInput;
import com.luistudio.reservas.exception.NotFoundException;
import com.luistudio.reservas.model.UserEntity;
import com.luistudio.reservas.repository.UserRepository;
import com.luistudio.reservas.service.auth.LoginService;
import com.luistudio.reservas.service.auth.PasswordResetService;
import com.luistudio.reservas.service.auth.TwoFactorService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
public class AuthService {

    private final LoginService loginService;
    private final PasswordResetService passwordResetService;
    private final TwoFactorService twoFactorService;
    private final UserRepository userRepository;
    private final DtoMapper dtoMapper;
    private final SessionService sessionService;

    public AuthService(
        LoginService loginService,
        PasswordResetService passwordResetService,
        TwoFactorService twoFactorService,
        UserRepository userRepository,
        DtoMapper dtoMapper,
        SessionService sessionService
    ) {
        this.loginService = loginService;
        this.passwordResetService = passwordResetService;
        this.twoFactorService = twoFactorService;
        this.userRepository = userRepository;
        this.dtoMapper = dtoMapper;
        this.sessionService = sessionService;
    }

    public LoginResponse login(LoginRequest request, String ipAddress, String userAgent) {
        return loginService.login(request, ipAddress, userAgent);
    }

    public LoginResponse verify2fa(Long userId, String code, String ip, String userAgent) {
        return twoFactorService.verifyLoginCode(userId, code, ip, userAgent);
    }

    @Transactional
    public void logoutCurrent(Long userId, String jti) {
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
        sessionService.revokeCurrentSession(user, jti);
    }

    @Transactional(readOnly = true)
    public AuthUserResponse me(Long userId) {
        UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
        return dtoMapper.toAuthUser(user);
    }

    public void requestReset(ResetRequestInput request) {
        passwordResetService.requestReset(request);
    }

    public void confirmReset(ResetConfirmInput request) {
        passwordResetService.confirmReset(request);
    }

    public void enroll2fa(Long userId) {
        twoFactorService.enroll(userId);
    }

    public void verify2faEnrollment(Long userId, TwoFactorCodeInput request) {
        twoFactorService.verifyEnrollment(userId, request);
    }

    public void disable2fa(Long userId) {
        twoFactorService.requestDisable(userId);
    }

    public void confirmDisable2fa(Long userId, TwoFactorCodeInput request) {
        twoFactorService.confirmDisable(userId, request);
    }
}
