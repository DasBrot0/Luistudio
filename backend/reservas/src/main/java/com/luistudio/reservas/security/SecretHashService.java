package com.luistudio.reservas.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SecretHashService {

    private static final String HMAC_SHA256 = "HmacSHA256";

    private final byte[] secretKey;

    public SecretHashService(@Value("${security.jwt.secret}") String secret) {
        this.secretKey = secret.getBytes(StandardCharsets.UTF_8);
    }

    public String hash(String rawValue) {
        try {
            Mac hmac = Mac.getInstance(HMAC_SHA256);
            hmac.init(new SecretKeySpec(secretKey, HMAC_SHA256));
            byte[] digest = hmac.doFinal(rawValue.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo calcular el hash del secreto", ex);
        }
    }

    public boolean matches(String rawValue, String storedHash) {
        byte[] expected = hash(rawValue).getBytes(StandardCharsets.UTF_8);
        byte[] current = storedHash.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(expected, current);
    }
}
