package com.luistudio.reservas.service.auth;

import com.luistudio.reservas.exception.BusinessException;
import com.luistudio.reservas.model.LoginAttemptEntity;
import com.luistudio.reservas.model.UserEntity;
import com.luistudio.reservas.repository.LoginAttemptRepository;
import com.luistudio.reservas.repository.UserRepository;
import com.luistudio.reservas.service.EmailOutboxService;
import com.luistudio.reservas.service.email.EmailTemplateService;
import java.time.OffsetDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

@Service
public class LoginAttemptService {

    private final LoginAttemptRepository loginAttemptRepository;
    private final UserRepository userRepository;
    private final EmailOutboxService emailOutboxService;
    private final EmailTemplateService emailTemplateService;

    public LoginAttemptService(
        LoginAttemptRepository loginAttemptRepository,
        UserRepository userRepository,
        EmailOutboxService emailOutboxService,
        EmailTemplateService emailTemplateService
    ) {
        this.loginAttemptRepository = loginAttemptRepository;
        this.userRepository = userRepository;
        this.emailOutboxService = emailOutboxService;
        this.emailTemplateService = emailTemplateService;
    }

    public void recordAttempt(UserEntity user, boolean success, String ipAddress, String userAgent) {
        LoginAttemptEntity attempt = new LoginAttemptEntity();
        attempt.setUsuario(user);
        attempt.setExito(success);
        attempt.setIpOrigen(ipAddress);
        attempt.setUserAgent(userAgent);
        loginAttemptRepository.save(attempt);
    }

    public void recordAttempt(UserEntity user, boolean success, String ipAddress) {
        recordAttempt(user, success, ipAddress, null);
    }

    public long countRecentFailures(UserEntity user, int minutes) {
        return loginAttemptRepository.countByUsuarioAndExitoFalseAndFechaIntentoAfter(
            user,
            OffsetDateTime.now().minusMinutes(minutes)
        );
    }

    public void registerFailedAttemptOrLock(UserEntity user, String ipAddress, String userAgent) {
        recordAttempt(user, false, ipAddress, userAgent);
        long failedLast15m = countRecentFailures(user, 15);

        if (failedLast15m >= 5) {
            user.setLockedUntil(OffsetDateTime.now().plusMinutes(15));
            userRepository.save(user);
            emailOutboxService.enqueue(
                user,
                "Intento no autorizado detectado",
                emailTemplateService.alert(
                    "Intento no autorizado detectado",
                    "Alguien intento ingresar a tu cuenta. Si no fuiste tu, reportalo."
                ),
                null
            );
            throw new BusinessException(HttpStatus.FORBIDDEN, "Cuenta bloqueada temporalmente");
        }
    }

    public void registerSuccessfulAttempt(UserEntity user, String ipAddress) {
        recordAttempt(user, true, ipAddress);
        if (user.getLockedUntil() != null) {
            user.setLockedUntil(null);
            userRepository.save(user);
        }
    }
}
