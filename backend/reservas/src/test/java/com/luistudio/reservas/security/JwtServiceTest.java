package com.luistudio.reservas.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.luistudio.reservas.exception.BusinessException;
import java.util.Base64;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService("test-secret", 30, 5);

    @Test
    void shouldGenerateEncryptedAuthToken() {
        String token = jwtService.generateToken(42L, "ADMIN");

        JwtService.ParsedToken parsed = jwtService.validate(token);

        assertEquals(42L, parsed.userId());
        assertEquals("ADMIN", parsed.role());
        assertFalse(parsed.provisional());
        assertFalse(token.contains("|"));

        String encryptedPayload = token.split("\\.")[2];
        String visiblePayload = new String(Base64.getUrlDecoder().decode(encryptedPayload));
        assertFalse(visiblePayload.contains("ADMIN"));
        assertFalse(visiblePayload.contains("42"));
    }

    @Test
    void shouldGenerateEncryptedProvisionalToken() {
        String token = jwtService.generateProvisionalToken(7L, "ESTUDIANTE");

        JwtService.ParsedToken parsed = jwtService.validate(token);

        assertEquals(7L, parsed.userId());
        assertEquals("ESTUDIANTE", parsed.role());
        assertTrue(parsed.provisional());
    }

    @Test
    void shouldRejectTamperedToken() {
        String token = jwtService.generateToken(10L, "ADMIN");
        String[] parts = token.split("\\.");
        String encryptedPayload = parts[2];
        int changedIndex = encryptedPayload.length() / 2;
        char replacement = encryptedPayload.charAt(changedIndex) == 'A' ? 'B' : 'A';
        parts[2] = encryptedPayload.substring(0, changedIndex)
            + replacement
            + encryptedPayload.substring(changedIndex + 1);
        String tampered = String.join(".", parts);

        assertThrows(BusinessException.class, () -> jwtService.validate(tampered));
    }
}
