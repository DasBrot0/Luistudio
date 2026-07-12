package com.luistudio.reservas.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;

/**
 * Pruebas unitarias de {@link AuthCookieService}.
 *
 * Verifica que las cookies de autenticación (sesión, provisional y de
 * limpieza) se generen con los atributos de seguridad esperados (HttpOnly,
 * Secure, SameSite, Path) y con el ciclo de vida (maxAge) correcto según
 * el caso: recordar sesión, 2FA pendiente o logout.
 */
class AuthCookieServiceTest {

    private static final String COOKIE_NAME = "auth_token";
    private static final long AUTH_EXPIRATION_MINUTES = 60L;
    private static final long PROVISIONAL_EXPIRATION_MINUTES = 5L;

    private AuthCookieService authCookieService;

    @BeforeEach
    void setUp() {
        authCookieService = new AuthCookieService(
            COOKIE_NAME,
            true,
            "Strict",
            AUTH_EXPIRATION_MINUTES,
            PROVISIONAL_EXPIRATION_MINUTES
        );
    }

    @Test
    @DisplayName("buildSessionCookie con rememberMe=true fija maxAge según la expiración configurada")
    void buildSessionCookieWithRememberMeSetsMaxAge() {
        ResponseCookie cookie = authCookieService.buildSessionCookie("jwt-token-value", true);

        assertEquals(COOKIE_NAME, cookie.getName());
        assertEquals("jwt-token-value", cookie.getValue());
        assertEquals(Duration.ofMinutes(AUTH_EXPIRATION_MINUTES), cookie.getMaxAge());
        assertTrue(cookie.isHttpOnly(), "La cookie de sesión debe ser HttpOnly");
        assertTrue(cookie.isSecure(), "La cookie de sesión debe ser Secure");
        assertEquals("Strict", cookie.getSameSite());
        assertEquals("/", cookie.getPath());
    }

    @Test
    @DisplayName("buildSessionCookie con rememberMe=false genera una cookie de sesión sin maxAge explícito")
    void buildSessionCookieWithoutRememberMeIsSessionCookie() {
        ResponseCookie cookie = authCookieService.buildSessionCookie("jwt-token-value", false);

        assertEquals("jwt-token-value", cookie.getValue());
        // Sin maxAge explícito, Spring usa -1 (cookie de sesión: expira al cerrar el navegador)
        assertEquals(Duration.ofSeconds(-1), cookie.getMaxAge());
    }

    @Test
    @DisplayName("buildProvisionalCookie fija maxAge según la expiración provisional configurada")
    void buildProvisionalCookieSetsProvisionalMaxAge() {
        ResponseCookie cookie = authCookieService.buildProvisionalCookie("provisional-token-value");

        assertEquals(COOKIE_NAME, cookie.getName());
        assertEquals("provisional-token-value", cookie.getValue());
        assertEquals(Duration.ofMinutes(PROVISIONAL_EXPIRATION_MINUTES), cookie.getMaxAge());
        assertTrue(cookie.isHttpOnly(), "La cookie provisional debe ser HttpOnly");
        assertTrue(cookie.isSecure(), "La cookie provisional debe ser Secure");
    }

    @Test
    @DisplayName("clearCookie genera una cookie vacía con maxAge cero para forzar su eliminación")
    void clearCookieProducesEmptyImmediatelyExpiredCookie() {
        ResponseCookie cookie = authCookieService.clearCookie();

        assertEquals(COOKIE_NAME, cookie.getName());
        assertEquals("", cookie.getValue());
        assertEquals(Duration.ZERO, cookie.getMaxAge());
    }

    @Test
    @DisplayName("getCookieName expone el nombre de cookie configurado")
    void getCookieNameReturnsConfiguredName() {
        assertEquals(COOKIE_NAME, authCookieService.getCookieName());
    }
}
