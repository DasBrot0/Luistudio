package com.luistudio.reservas.service.auth;

import com.luistudio.reservas.model.PasswordResetEntity;
import com.luistudio.reservas.model.TwoFactorCodeEntity;
import com.luistudio.reservas.model.UserEntity;
import com.luistudio.reservas.security.SecretHashService;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class SecurityCodeService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final SecretHashService secretHashService;

    public SecurityCodeService(SecretHashService secretHashService) {
        this.secretHashService = secretHashService;
    }

    public GeneratedTwoFactorCode createTwoFactorCode(UserEntity user, int expiresInMinutes) {
        String rawCode = generateSixDigits();

        TwoFactorCodeEntity entity = new TwoFactorCodeEntity();
        entity.setUsuario(user);
        entity.setCode(secretHashService.hash(rawCode));
        entity.setExpiraAt(OffsetDateTime.now().plusMinutes(expiresInMinutes));
        entity.setUsado(false);

        return new GeneratedTwoFactorCode(entity, rawCode);
    }

    public GeneratedPasswordReset createPasswordReset(UserEntity user, int expiresInMinutes) {
        String rawToken = UUID.randomUUID().toString().replace("-", "");

        PasswordResetEntity entity = new PasswordResetEntity();
        entity.setUsuario(user);
        entity.setToken(secretHashService.hash(rawToken));
        entity.setExpiraEn(OffsetDateTime.now().plusMinutes(expiresInMinutes));
        entity.setUsado(false);

        return new GeneratedPasswordReset(entity, rawToken);
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
