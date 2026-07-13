package com.luistudio.reservas.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.luistudio.reservas.dto.auth.AuthUserResponse;
import com.luistudio.reservas.dto.auth.LoginRequest;
import com.luistudio.reservas.dto.auth.LoginResponse;
import com.luistudio.reservas.dto.common.MessageResponse;
import com.luistudio.reservas.security.AuthCookieService;
import com.luistudio.reservas.security.AuthPrincipal;
import com.luistudio.reservas.security.CurrentUserProvider;
import com.luistudio.reservas.service.AccessGuard;
import com.luistudio.reservas.service.AuthService;
import com.luistudio.reservas.service.SensitiveChangeService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Pruebas unitarias de sanitización de login y gestión de cookies en
 * {@link AuthController}.
 *
 * Verifica que:
 *  - El token definitivo y el token provisional nunca se expongan en el
 *    cuerpo JSON de la respuesta (se transmiten únicamente vía cookie
 *    HttpOnly, generada por {@link AuthCookieService}).
 *  - La cookie de sesión se agrega cuando el servicio devuelve un token
 *    definitivo, y la cookie provisional cuando devuelve un token
 *    provisional (flujo 2FA).
 *  - El logout agrega la cookie de limpieza generada por
 *    {@link AuthCookieService#clearCookie()}.
 */
@ExtendWith(MockitoExtension.class)
class AuthLoginCookieSanitizationTest {

    @Mock private AuthService authService;
    @Mock private CurrentUserProvider currentUserProvider;
    @Mock private AuthCookieService authCookieService;
    @Mock private AccessGuard accessGuard;
    @Mock private SensitiveChangeService sensitiveChangeService;

    @InjectMocks
    private AuthController controller;

    private HttpServletRequest httpServletRequest;
    private HttpServletResponse httpServletResponse;

    @BeforeEach
    void setUp() {
        httpServletRequest = new MockHttpServletRequest();
        httpServletResponse = mock(HttpServletResponse.class);
    }

    private AuthUserResponse studentUser() {
        return new AuthUserResponse(
            1L, "STU-001", "Luis", "García", "user@test.com", "ESTUDIANTE", "HABILITADO", false
        );
    }

    // -----------------------------------------------------------------
    // Caso 1: Login exitoso devuelve respuesta sin token en el body
    // -----------------------------------------------------------------
    @Test
    @DisplayName("Login exitoso: la respuesta JSON no expone el token")
    void loginSuccess_doesNotExposeTokenInBody() {
        LoginResponse serviceResponse = new LoginResponse(
            "real-jwt-token", null, false, studentUser(), null
        );
        when(authService.login(any(LoginRequest.class), any(), any())).thenReturn(serviceResponse);
        when(authCookieService.buildSessionCookie(eq("real-jwt-token"), eq(false)))
            .thenReturn(ResponseCookie.from("auth_token", "real-jwt-token").build());

        LoginRequest request = new LoginRequest("user@test.com", "Abcd1234!", false);
        ResponseEntity<LoginResponse> result = controller.login(request, httpServletRequest, httpServletResponse);

        assertEquals(200, result.getStatusCode().value());
        LoginResponse body = result.getBody();
        assertNull(body.token(), "El token no debe exponerse en el cuerpo de la respuesta");
        assertNull(body.provisionalToken(), "El provisionalToken no debe exponerse en el cuerpo de la respuesta");
        assertEquals(false, body.twoFactorRequired());
        assertEquals("user@test.com", body.user().email());
    }

    // -----------------------------------------------------------------
    // Caso 2: Login con 2FA requerido devuelve respuesta sin provisional
    // token en el body
    // -----------------------------------------------------------------
    @Test
    @DisplayName("Login con 2FA requerido: la respuesta JSON no expone el provisionalToken")
    void loginWith2faRequired_doesNotExposeProvisionalTokenInBody() {
        LoginResponse serviceResponse = new LoginResponse(
            null, "provisional-token-value", true, null, "Código 2FA requerido"
        );
        when(authService.login(any(LoginRequest.class), any(), any())).thenReturn(serviceResponse);
        when(authCookieService.buildProvisionalCookie(eq("provisional-token-value")))
            .thenReturn(ResponseCookie.from("auth_token", "provisional-token-value").build());

        LoginRequest request = new LoginRequest("user@test.com", "Abcd1234!", false);
        ResponseEntity<LoginResponse> result = controller.login(request, httpServletRequest, httpServletResponse);

        assertEquals(200, result.getStatusCode().value());
        LoginResponse body = result.getBody();
        assertNull(body.token(), "El token no debe exponerse en el cuerpo de la respuesta");
        assertNull(body.provisionalToken(), "El provisionalToken no debe exponerse en el cuerpo de la respuesta");
        assertEquals(true, body.twoFactorRequired());
    }

    // -----------------------------------------------------------------
    // Caso 3: Cookie de sesión se agrega cuando existe token definitivo
    // -----------------------------------------------------------------
    @Test
    @DisplayName("Cuando el servicio devuelve token definitivo, se agrega la cookie de sesión")
    void loginWithDefinitiveToken_addsSessionCookie() {
        LoginResponse serviceResponse = new LoginResponse(
            "real-jwt-token", null, false, studentUser(), null
        );
        when(authService.login(any(LoginRequest.class), any(), any())).thenReturn(serviceResponse);
        ResponseCookie sessionCookie = ResponseCookie.from("auth_token", "real-jwt-token").build();
        when(authCookieService.buildSessionCookie(eq("real-jwt-token"), eq(true)))
            .thenReturn(sessionCookie);

        LoginRequest request = new LoginRequest("user@test.com", "Abcd1234!", true);
        controller.login(request, httpServletRequest, httpServletResponse);

        verify(authCookieService).buildSessionCookie("real-jwt-token", true);
        verify(authCookieService, never()).buildProvisionalCookie(any());
        verify(httpServletResponse).addHeader("Set-Cookie", sessionCookie.toString());
    }

    // -----------------------------------------------------------------
    // Caso 4: Cookie provisional se agrega cuando existe token provisional
    // -----------------------------------------------------------------
    @Test
    @DisplayName("Cuando el servicio devuelve token provisional, se agrega la cookie provisional")
    void loginWithProvisionalToken_addsProvisionalCookie() {
        LoginResponse serviceResponse = new LoginResponse(
            null, "provisional-token-value", true, null, "Código 2FA requerido"
        );
        when(authService.login(any(LoginRequest.class), any(), any())).thenReturn(serviceResponse);
        ResponseCookie provisionalCookie = ResponseCookie.from("auth_token", "provisional-token-value").build();
        when(authCookieService.buildProvisionalCookie(eq("provisional-token-value")))
            .thenReturn(provisionalCookie);

        LoginRequest request = new LoginRequest("user@test.com", "Abcd1234!", false);
        controller.login(request, httpServletRequest, httpServletResponse);

        verify(authCookieService).buildProvisionalCookie("provisional-token-value");
        verify(authCookieService, never()).buildSessionCookie(any(), any(Boolean.class));
        verify(httpServletResponse).addHeader("Set-Cookie", provisionalCookie.toString());
    }

    // -----------------------------------------------------------------
    // Caso 5: Logout agrega cookie de limpieza
    // -----------------------------------------------------------------
    @Test
    @DisplayName("Logout agrega la cookie de limpieza generada por AuthCookieService")
    void logout_addsClearCookie() {
        AuthPrincipal principal = new AuthPrincipal(1L, "user@test.com", "ESTUDIANTE");
        when(currentUserProvider.requireCurrentUser()).thenReturn(principal);
        when(currentUserProvider.currentJti()).thenReturn("jti-123");
        ResponseCookie clearCookie = ResponseCookie.from("auth_token", "").maxAge(0).build();
        when(authCookieService.clearCookie()).thenReturn(clearCookie);

        ResponseEntity<MessageResponse> result = controller.logout(httpServletResponse);

        verify(authService).logoutCurrent(1L, "jti-123");
        verify(httpServletResponse).addHeader("Set-Cookie", clearCookie.toString());
        assertEquals(200, result.getStatusCode().value());
        assertEquals("Sesión cerrada", result.getBody().message());
    }
}
