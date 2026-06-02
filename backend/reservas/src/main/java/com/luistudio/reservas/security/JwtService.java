package com.luistudio.reservas.security;

import com.luistudio.reservas.exception.BusinessException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class JwtService {

    private static final String AES = "AES";
    private static final String AES_GCM = "AES/GCM/NoPadding";
    private static final int GCM_TAG_BITS = 128;
    private static final int IV_LENGTH_BYTES = 12;
    private static final String TOKEN_VERSION = "v1";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final SecretKeySpec encryptionKey;
    private final long expirationSeconds;
    private final long provisionalExpirationSeconds;

    public JwtService(
        @Value("${security.jwt.secret}") String secret,
        @Value("${security.jwt.expiration-minutes}") long expirationMinutes,
        @Value("${security.jwt.provisional-expiration-minutes}") long provisionalExpirationMinutes
    ) {
        this.encryptionKey = buildKey(secret);
        this.expirationSeconds = expirationMinutes * 60;
        this.provisionalExpirationSeconds = provisionalExpirationMinutes * 60;
    }

    public String generateToken(Long userId, String role) {
        return generate(userId, role, false, expirationSeconds);
    }

    public String generateProvisionalToken(Long userId, String role) {
        return generate(userId, role, true, provisionalExpirationSeconds);
    }

    public ParsedToken validate(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3 || !TOKEN_VERSION.equals(parts[0])) {
                throw new BusinessException(HttpStatus.UNAUTHORIZED, "Token inválido");
            }

            byte[] iv = Base64.getUrlDecoder().decode(parts[1]);
            byte[] encryptedPayload = Base64.getUrlDecoder().decode(parts[2]);
            String payload = decrypt(iv, encryptedPayload);
            String[] values = payload.split("\\|");
            if (values.length != 4) {
                throw new BusinessException(HttpStatus.UNAUTHORIZED, "Token inválido");
            }

            Long userId = Long.parseLong(values[0]);
            String role = values[1];
            boolean provisional = Boolean.parseBoolean(values[2]);
            long exp = Long.parseLong(values[3]);

            if (Instant.now().getEpochSecond() > exp) {
                throw new BusinessException(HttpStatus.UNAUTHORIZED, "Token expirado");
            }

            return new ParsedToken(userId, role, provisional);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Token inválido");
        }
    }

    private String generate(Long userId, String role, boolean provisional, long ttlSeconds) {
        long exp = Instant.now().getEpochSecond() + ttlSeconds;
        String payload = userId + "|" + role + "|" + provisional + "|" + exp;
        byte[] iv = new byte[IV_LENGTH_BYTES];
        RANDOM.nextBytes(iv);
        byte[] encryptedPayload = encrypt(payload, iv);
        return TOKEN_VERSION + "."
            + Base64.getUrlEncoder().withoutPadding().encodeToString(iv)
            + "."
            + Base64.getUrlEncoder().withoutPadding().encodeToString(encryptedPayload);
    }

    private SecretKeySpec buildKey(String secret) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(digest, AES);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo preparar la clave del token", ex);
        }
    }

    private byte[] encrypt(String payload, byte[] iv) {
        try {
            Cipher cipher = Cipher.getInstance(AES_GCM);
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            return cipher.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo cifrar token", ex);
        }
    }

    private String decrypt(byte[] iv, byte[] encryptedPayload) {
        try {
            Cipher cipher = Cipher.getInstance(AES_GCM);
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
            byte[] payload = cipher.doFinal(encryptedPayload);
            return new String(payload, StandardCharsets.UTF_8);
        } catch (Exception ex) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Token inválido");
        }
    }

    public record ParsedToken(Long userId, String role, boolean provisional) {
    }
}
