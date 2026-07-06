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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
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
    public LoginResponse login(LoginRequest request, String ipAddress, String userAgent) {
        String email = request.email().trim();

        long t0 = System.currentTimeMillis();
        UserEntity user = userRepository.findByCorreoIgnoreCase(email)
            .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas"));
        log.info("[PERF][LOGIN] find user: {} ms", System.currentTimeMillis() - t0);

        if (user.getEstado() == UserStatus.DESHABILITADO) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Cuenta deshabilitada");
        }

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(OffsetDateTime.now())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "Cuenta bloqueada temporalmente");
        }

        long t1 = System.currentTimeMillis();
        boolean passwordOk = passwordEncoder.matches(request.password(), user.getPasswordHash());
        log.info("[PERF][LOGIN] bcrypt matches: {} ms", System.currentTimeMillis() - t1);

        if (!passwordOk) {
            loginAttemptService.registerFailedAttemptOrLock(user, ipAddress, userAgent);
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Credenciales incorrectas");
        }

        long t2 = System.currentTimeMillis();
        loginAttemptService.registerSuccessfulAttempt(user, ipAddress);
        log.info("[PERF][LOGIN] success attempt handling: {} ms", System.currentTimeMillis() - t2);

        LoginContext loginContext = new LoginContext();
        if (Boolean.TRUE.equals(user.getHas2fa())) {
            loginContext.setLoginStrategy(twoFactorLoginStrategy);
        } else {
            loginContext.setLoginStrategy(standardLoginStrategy);
        }

        long t3 = System.currentTimeMillis();
        LoginResponse response = loginContext.login(user, ipAddress, userAgent);
        log.info("[PERF][LOGIN] strategy response: {} ms", System.currentTimeMillis() - t3);

        return response;
    }
}
