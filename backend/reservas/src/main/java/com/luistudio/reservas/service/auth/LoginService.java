package com.luistudio.reservas.service.auth;

import com.luistudio.reservas.dto.auth.LoginRequest;
import com.luistudio.reservas.dto.auth.LoginResponse;
import com.luistudio.reservas.exception.BusinessException;
import com.luistudio.reservas.model.UserEntity;
import com.luistudio.reservas.model.UserStatus;
import com.luistudio.reservas.repository.UserRepository;
import com.luistudio.reservas.service.auth.strategy.LoginContext;
import com.luistudio.reservas.service.auth.strategy.StandardLoginStrategy;
import com.luistudio.reservas.service.auth.strategy.TwoFactorLoginStrategy;
import java.time.OffsetDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LoginService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;
    private final StandardLoginStrategy standardLoginStrategy;
    private final TwoFactorLoginStrategy twoFactorLoginStrategy;

    public LoginService(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder,
        LoginAttemptService loginAttemptService,
        StandardLoginStrategy standardLoginStrategy,
        TwoFactorLoginStrategy twoFactorLoginStrategy
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.loginAttemptService = loginAttemptService;
        this.standardLoginStrategy = standardLoginStrategy;
        this.twoFactorLoginStrategy = twoFactorLoginStrategy;
    }

    @Transactional(noRollbackFor = BusinessException.class)
    public LoginResponse login(LoginRequest request, String ipAddress) {
        UserEntity user = userRepository.findByCorreoIgnoreCase(request.email())
            .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas"));

        if (user.getEstado() == UserStatus.DESHABILITADO) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Cuenta deshabilitada");
        }

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(OffsetDateTime.now())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Cuenta bloqueada temporalmente");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            loginAttemptService.registerFailedAttemptOrLock(user, ipAddress);
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas");
        }

        loginAttemptService.registerSuccessfulAttempt(user, ipAddress);
        LoginContext loginContext = new LoginContext();
        if (Boolean.TRUE.equals(user.getHas2fa())) {
            loginContext.setLoginStrategy(twoFactorLoginStrategy);
        } else {
            loginContext.setLoginStrategy(standardLoginStrategy);
        }

        return loginContext.login(user);
    }
}
