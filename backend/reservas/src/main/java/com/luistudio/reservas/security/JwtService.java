package com.luistudio.reservas.security;

import com.luistudio.reservas.exception.BusinessException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class JwtService {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private final String secret;
    private final long expirationSeconds;
    private final long provisionalExpirationSeconds;

    public JwtService(
        @Value("${security.jwt.secret}") String secret,
        @Value("${security.jwt.expiration-minutes}") long expirationMinutes,
        @Value("${security.jwt.provisional-expiration-minutes}") long provisionalExpirationMinutes
    ) {
        this.secret = secret;
        this.expirationSeconds = expirationMinutes * 60;
        this.provisionalExpirationSeconds = provisionalExpirationMinutes * 60;
    }

    public String generateToken(Long userId, String email, String role) {
        return generate(userId, email, role, false, expirationSeconds);
    }

    public String generateProvisionalToken(Long userId, String email, String role) {
        return generate(userId, email, role, true, provisionalExpirationSeconds);
    }

    public ParsedToken validate(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 2) {
                throw new BusinessException(HttpStatus.UNAUTHORIZED, "Token invalido");
            }

            String payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
            String expected = sign(parts[0]);
            if (!expected.equals(parts[1])) {
                throw new BusinessException(HttpStatus.UNAUTHORIZED, "Token invalido");
            }

            String[] values = payload.split("\\|");
            if (values.length != 5) {
                throw new BusinessException(HttpStatus.UNAUTHORIZED, "Token invalido");
            }

            Long userId = Long.parseLong(values[0]);
            String email = values[1];
            String role = values[2];
            boolean provisional = Boolean.parseBoolean(values[3]);
            long exp = Long.parseLong(values[4]);

            if (Instant.now().getEpochSecond() > exp) {
                throw new BusinessException(HttpStatus.UNAUTHORIZED, "Token expirado");
            }

            return new ParsedToken(userId, email, role, provisional);
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "Token invalido");
        }
    }

    private String generate(Long userId, String email, String role, boolean provisional, long ttlSeconds) {
        long exp = Instant.now().getEpochSecond() + ttlSeconds;
        String payload = userId + "|" + email + "|" + role + "|" + provisional + "|" + exp;
        String payloadB64 = Base64.getUrlEncoder().withoutPadding()
            .encodeToString(payload.getBytes(StandardCharsets.UTF_8));
        return payloadB64 + "." + sign(payloadB64);
    }

    private String sign(String data) {
        try {
            Mac hmac = Mac.getInstance(HMAC_SHA256);
            SecretKeySpec key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_SHA256);
            hmac.init(key);
            byte[] hash = hmac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo firmar token", ex);
        }
    }

    public record ParsedToken(Long userId, String email, String role, boolean provisional) {
    }
}
