package com.luistudio.reservas.security;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class AuthCookieService {

    private final String cookieName;
    private final boolean secureCookie;
    private final String sameSite;
    private final long authExpirationMinutes;
    private final long provisionalExpirationMinutes;

    public AuthCookieService(
        @Value("${app.auth.cookie-name}") String cookieName,
        @Value("${app.auth.cookie-secure}") boolean secureCookie,
        @Value("${app.auth.cookie-same-site}") String sameSite,
        @Value("${security.jwt.expiration-minutes}") long authExpirationMinutes,
        @Value("${security.jwt.provisional-expiration-minutes}") long provisionalExpirationMinutes
    ) {
        this.cookieName = cookieName;
        this.secureCookie = secureCookie;
        this.sameSite = sameSite;
        this.authExpirationMinutes = authExpirationMinutes;
        this.provisionalExpirationMinutes = provisionalExpirationMinutes;
    }

    public String getCookieName() {
        return cookieName;
    }

    public ResponseCookie buildSessionCookie(String token, boolean rememberMe) {
        ResponseCookie.ResponseCookieBuilder builder = baseCookie(token);
        if (rememberMe) {
            builder.maxAge(Duration.ofMinutes(authExpirationMinutes));
        }
        return builder.build();
    }

    public ResponseCookie buildProvisionalCookie(String token) {
        return baseCookie(token)
            .maxAge(Duration.ofMinutes(provisionalExpirationMinutes))
            .build();
    }

    public ResponseCookie clearCookie() {
        return baseCookie("")
            .maxAge(Duration.ZERO)
            .build();
    }

    private ResponseCookie.ResponseCookieBuilder baseCookie(String value) {
        return ResponseCookie.from(cookieName, value)
            .httpOnly(true)
            .secure(secureCookie)
            .sameSite(sameSite)
            .path("/");
    }
}
