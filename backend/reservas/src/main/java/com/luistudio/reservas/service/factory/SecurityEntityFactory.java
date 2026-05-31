package com.luistudio.reservas.service.factory;

import com.luistudio.reservas.model.LoginAttemptEntity;
import com.luistudio.reservas.model.PasswordResetEntity;
import com.luistudio.reservas.model.TwoFactorCodeEntity;
import com.luistudio.reservas.model.UserEntity;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class SecurityEntityFactory {

    private static final SecureRandom RANDOM = new SecureRandom();

    public LoginAttemptEntity newLoginAttempt(UserEntity user, boolean success, String ipAddress) {
        LoginAttemptEntity attempt = new LoginAttemptEntity();
        attempt.setUsuario(user);
        attempt.setExito(success);
        attempt.setIpOrigen(ipAddress);
        return attempt;
    }

    public TwoFactorCodeEntity newTwoFactorCode(UserEntity user, int expiresInMinutes) {
        TwoFactorCodeEntity twoFactorCode = new TwoFactorCodeEntity();
        twoFactorCode.setUsuario(user);
        twoFactorCode.setCode(generateSixDigits());
        twoFactorCode.setExpiraAt(OffsetDateTime.now().plusMinutes(expiresInMinutes));
        twoFactorCode.setUsado(false);
        return twoFactorCode;
    }

    public PasswordResetEntity newPasswordReset(UserEntity user, int expiresInMinutes) {
        PasswordResetEntity reset = new PasswordResetEntity();
        reset.setUsuario(user);
        reset.setToken(UUID.randomUUID().toString().replace("-", ""));
        reset.setExpiraEn(OffsetDateTime.now().plusMinutes(expiresInMinutes));
        reset.setUsado(false);
        return reset;
    }

    private String generateSixDigits() {
        int value = RANDOM.nextInt(900000) + 100000;
        return String.valueOf(value);
    }
}
