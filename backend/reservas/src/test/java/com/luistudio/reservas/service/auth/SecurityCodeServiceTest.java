package com.luistudio.reservas.service.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.luistudio.reservas.model.UserEntity;
import com.luistudio.reservas.security.SecretHashService;
import org.junit.jupiter.api.Test;

class SecurityCodeServiceTest {

    private final SecretHashService secretHashService = new SecretHashService("test-secret");
    private final SecurityCodeService securityCodeService = new SecurityCodeService(secretHashService);

    @Test
    void shouldHashTwoFactorCodesBeforePersisting() {
        SecurityCodeService.GeneratedTwoFactorCode generated = securityCodeService.createTwoFactorCode(new UserEntity(), 10);

        assertNotEquals(generated.rawCode(), generated.entity().getCode());
        assertTrue(secretHashService.matches(generated.rawCode(), generated.entity().getCode()));
        assertEquals(6, generated.rawCode().length());
    }

    @Test
    void shouldHashPasswordResetTokenBeforePersisting() {
        SecurityCodeService.GeneratedPasswordReset generated = securityCodeService.createPasswordReset(new UserEntity(), 30);

        assertNotEquals(generated.rawToken(), generated.entity().getToken());
        assertTrue(secretHashService.matches(generated.rawToken(), generated.entity().getToken()));
    }
}
