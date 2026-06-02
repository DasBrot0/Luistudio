package com.luistudio.reservas.service.factory;

import com.luistudio.reservas.model.LoginAttemptEntity;
import com.luistudio.reservas.model.PasswordResetEntity;
import com.luistudio.reservas.model.TwoFactorCodeEntity;
import com.luistudio.reservas.model.UserEntity;
import com.luistudio.reservas.security.SecretHashService;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class SecurityEntityFactory {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final SecretHashService secretHashService;

    public SecurityEntityFactory(SecretHashService secretHashService) {
        this.secretHashService = secretHashService;
    }

    public LoginAttemptEntity newLoginAttempt(UserEntity user, boolean success, String ipAddress) {
        LoginAttemptEntity attempt = new LoginAttemptEntity();
        attempt.setUsuario(user);
        attempt.setExito(success);
        attempt.setIpOrigen(ipAddress);
        return attempt;
    }

    public GeneratedTwoFactorCode newTwoFactorCode(UserEntity user, int expiresInMinutes) {
        String rawCode = generateSixDigits();
        TwoFactorCodeEntity twoFactorCode = new TwoFactorCodeEntity();
        twoFactorCode.setUsuario(user);
        twoFactorCode.setCode(secretHashService.hash(rawCode));
        twoFactorCode.setExpiraAt(OffsetDateTime.now().plusMinutes(expiresInMinutes));
        twoFactorCode.setUsado(false);
        return new GeneratedTwoFactorCode(twoFactorCode, rawCode);
    }

    public GeneratedPasswordReset newPasswordReset(UserEntity user, int expiresInMinutes) {
        String rawToken = UUID.randomUUID().toString().replace("-", "");
        PasswordResetEntity reset = new PasswordResetEntity();
        reset.setUsuario(user);
        reset.setToken(secretHashService.hash(rawToken));
        reset.setExpiraEn(OffsetDateTime.now().plusMinutes(expiresInMinutes));
        reset.setUsado(false);
        return new GeneratedPasswordReset(reset, rawToken);
    }

    private String generateSixDigits() {
        int value = RANDOM.nextInt(900000) + 100000;
        return String.valueOf(value);
    }

    public record GeneratedTwoFactorCode(TwoFactorCodeEntity entity, String rawCode) {
    }

    public record GeneratedPasswordReset(PasswordResetEntity entity, String rawToken) {
    }
}
