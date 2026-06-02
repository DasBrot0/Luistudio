package com.luistudio.reservas.service.factory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.luistudio.reservas.model.UserEntity;
import com.luistudio.reservas.security.SecretHashService;
import org.junit.jupiter.api.Test;

class SecurityEntityFactoryTest {

    private final SecretHashService secretHashService = new SecretHashService("test-secret");
    private final SecurityEntityFactory securityEntityFactory = new SecurityEntityFactory(secretHashService);

    @Test
    void shouldHashTwoFactorCodesBeforePersisting() {
        SecurityEntityFactory.GeneratedTwoFactorCode generated = securityEntityFactory.newTwoFactorCode(new UserEntity(), 10);

        assertNotEquals(generated.rawCode(), generated.entity().getCode());
        assertTrue(secretHashService.matches(generated.rawCode(), generated.entity().getCode()));
        assertEquals(6, generated.rawCode().length());
    }

    @Test
    void shouldHashPasswordResetTokenBeforePersisting() {
        SecurityEntityFactory.GeneratedPasswordReset generated = securityEntityFactory.newPasswordReset(new UserEntity(), 30);

        assertNotEquals(generated.rawToken(), generated.entity().getToken());
        assertTrue(secretHashService.matches(generated.rawToken(), generated.entity().getToken()));
    }
}
