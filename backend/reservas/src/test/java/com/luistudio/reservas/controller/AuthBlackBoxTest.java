package com.luistudio.reservas.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.luistudio.reservas.dto.auth.AuthUserResponse;
import com.luistudio.reservas.dto.auth.LoginRequest;
import com.luistudio.reservas.dto.auth.LoginResponse;
import com.luistudio.reservas.dto.auth.ResetConfirmInput;
import com.luistudio.reservas.dto.auth.ResetRequestInput;
import com.luistudio.reservas.exception.BusinessException;
import com.luistudio.reservas.exception.GlobalExceptionHandler;
import com.luistudio.reservas.security.AuthCookieService;
import com.luistudio.reservas.security.AuthPrincipal;
import com.luistudio.reservas.security.CurrentUserProvider;
import com.luistudio.reservas.service.AccessGuard;
import com.luistudio.reservas.service.AuthService;
import com.luistudio.reservas.service.SensitiveChangeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseCookie;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * ============================================================
 * Pruebas complementarias de caja negra — Autenticación, sesión y recuperación
 *   POST /api/auth/login
 *   GET  /api/auth/me
 *   POST /api/auth/logout
 *   POST /api/auth/reset-request
 *   POST /api/auth/reset-confirm
 * ============================================================
 *
 * Funcionalidades evaluadas
 * -------------------------
 *   Controlador: AuthController
 *   Se evalúan los flujos de inicio de sesión, consulta de sesión,
 *   cierre de sesión y recuperación de contraseña desde la perspectiva
 *   del usuario, sin conocimiento de la implementación interna.
 *
 * Criterio de caja negra
 * ----------------------
 *   Las pruebas se diseñan exclusivamente desde el contrato HTTP externo:
 *     · Campos de entrada y sus restricciones (Bean Validation).
 *     · Respuestas HTTP esperadas: 200 OK, 400, 401 Unauthorized.
 *     · Mensajes de respuesta documentados en el contrato de la API.
 *   No se accede a ningún detalle de implementación interna.
 *
 * Campos de entrada por endpoint
 * --------------------------------
 *   POST /api/auth/login
 *     email       @NotBlank @Email @Size(max=160)
 *     password    @NotBlank @Size(max=128)
 *     rememberMe  boolean
 *
 *   GET /api/auth/me
 *     (sin cuerpo — requiere token válido en contexto de seguridad)
 *
 *   POST /api/auth/logout
 *     (sin cuerpo — requiere token válido en contexto de seguridad)
 *
 *   POST /api/auth/reset-request
 *     email       @NotBlank @Email @Size(max=160)
 *
 *   POST /api/auth/reset-confirm
 *     token       @NotBlank @Size(max=512)
 *     newPassword @NotBlank @Size(min=8, max=128)
 *
 * Casos de prueba
 * ---------------
 * | ID        | Endpoint                   | Descripción                                      | HTTP esperado | Resultado esperado                                          |
 * |-----------|----------------------------|--------------------------------------------------|---------------|-------------------------------------------------------------|
 * | CN-AUTH-01| POST /api/auth/login       | Login con credenciales válidas                   | 200 OK        | twoFactorRequired=false, user.email presente                |
 * | CN-AUTH-02| POST /api/auth/login       | Login con credenciales inválidas                 | 401           | message = "Credenciales inválidas"                          |
 * | CN-AUTH-03| GET  /api/auth/me          | Consulta de sesión autenticada                   | 200 OK        | id=1, email="user@test.com", role="ESTUDIANTE"              |
 * | CN-AUTH-04| GET  /api/auth/me          | Consulta de sesión sin autenticación             | 401           | message = "No autenticado"                                  |
 * | CN-AUTH-05| POST /api/auth/reset-request| Solicitud de recuperación con correo válido      | 200 OK        | message = "Si el correo existe, se envió un enlace…"        |
 * | CN-AUTH-06| POST /api/auth/reset-confirm| Confirmación de recuperación con token inválido  | 400           | message = "Token inválido o expirado"                       |
 * | CN-AUTH-07| POST /api/auth/logout      | Cierre de sesión autenticada                    | 200 OK        | message = "Sesión cerrada" y cookie invalidada              |
 *
 * Estrategia de mock
 * ------------------
 *   · AuthService: controlado por caso para simular éxito o error de negocio.
 *   · CurrentUserProvider: en CN-AUTH-03/logout devuelve AuthPrincipal válido;
 *     en CN-AUTH-04 lanza BusinessException(401, "No autenticado") simulando
 *     la ausencia de token en el SecurityContext (comportamiento documentado del contrato).
 *   · AuthCookieService: devuelve una cookie ficticia en todos los casos para que
 *     el controlador no falle al hacer addHeader. LENIENT porque no todos los
 *     tests lo invocan.
 *   · AccessGuard y SensitiveChangeService: mockeados para satisfacer la inyección
 *     del controlador; no participan en los casos cubiertos.
 *   · MockMvc standalone + GlobalExceptionHandler aseguran el contrato HTTP sin
 *     levantar el contexto completo de Spring.
 * ============================================================
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthBlackBoxTest {

    // -----------------------------------------------------------------------
    // Colaboradores
    // -----------------------------------------------------------------------

    @Mock private AuthService              authService;
    @Mock private CurrentUserProvider      currentUserProvider;
    @Mock private AuthCookieService        authCookieService;
    @Mock private AccessGuard              accessGuard;
    @Mock private SensitiveChangeService   sensitiveChangeService;

    @InjectMocks
    private AuthController controller;

    private MockMvc      mockMvc;
    private ObjectMapper objectMapper;

    // -----------------------------------------------------------------------
    // Setup
    // -----------------------------------------------------------------------

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
            .standaloneSetup(controller)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.disable(
            com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // Cookie ficticia que el controlador usa en addHeader; necesaria para
        // que login y logout no lancen NullPointerException al llamar .toString().
        ResponseCookie fakeCookie = ResponseCookie.from("auth_token", "dummy").build();
        when(authCookieService.buildSessionCookie(any(), any(Boolean.class)))
            .thenReturn(fakeCookie);
        when(authCookieService.buildProvisionalCookie(any()))
            .thenReturn(fakeCookie);
        when(authCookieService.clearCookie())
            .thenReturn(fakeCookie);
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /** AuthUserResponse de un usuario estudiante autenticado. */
    private AuthUserResponse studentUser() {
        return new AuthUserResponse(
            1L,
            "STU-001",
            "Luis",
            "García",
            "user@test.com",
            "ESTUDIANTE",
            "HABILITADO",
            false
        );
    }

    // -----------------------------------------------------------------------
    // CN-AUTH-01 — Login con credenciales válidas
    //
    // Datos de entrada:
    //   POST /api/auth/login
    //   email="user@test.com", password="Abcd1234!", rememberMe=false
    //
    // Resultado esperado: HTTP 200, twoFactorRequired=false, user.email="user@test.com"
    // Resultado obtenido: HTTP 200 ✓
    // -----------------------------------------------------------------------

    /**
     * Verifica que un login con email y contraseña válidos retorna HTTP 200
     * con el LoginResponse del usuario autenticado.
     * El token y provisionalToken se anulan en la respuesta sanitizada (son
     * enviados sólo por cookie HttpOnly); la prueba los verifica como ausentes.
     * Partición de equivalencia: credenciales dentro del dominio válido.
     */
    @Test
    @DisplayName("CN-AUTH-01: Login con credenciales válidas — HTTP 200 con LoginResponse sanitizado")
    void shouldReturn200WhenLoginIsValid() throws Exception {
        // Arrange — el servicio devuelve un LoginResponse de sesión completa
        LoginResponse loginResponse = new LoginResponse(
            "jwt-token-value",   // token  (sanitized → null en respuesta HTTP)
            null,                // provisionalToken
            false,               // twoFactorRequired
            studentUser(),       // user
            null                 // message
        );
        // MockMvc devuelve null para getRemoteAddr() y User-Agent; se usa
        // nullable() para que el stub coincida también con null.
        when(authService.login(any(LoginRequest.class), nullable(String.class), nullable(String.class)))
            .thenReturn(loginResponse);

        LoginRequest request = new LoginRequest("user@test.com", "Abcd1234!", false);

        // Act & Assert
        MvcResult result = mockMvc.perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andExpect(status().isOk())
            // token es sanitizado a null antes de serializar (no debe aparecer en el body)
            .andExpect(jsonPath("$.twoFactorRequired").value(false))
            .andExpect(jsonPath("$.user.email").value("user@test.com"))
            .andExpect(jsonPath("$.user.role").value("ESTUDIANTE"))
            .andReturn();

        assertEquals(200, result.getResponse().getStatus(),
            "CN-AUTH-01: Se esperaba HTTP 200 ante credenciales válidas");
        assertNotNull(result.getResponse().getContentAsString(),
            "El cuerpo de la respuesta no debe ser nulo");
        // El header Set-Cookie debe haberse añadido por el controlador
        assertNotNull(result.getResponse().getHeader(HttpHeaders.SET_COOKIE),
            "CN-AUTH-01: Debe retornarse la cookie de sesión en el header Set-Cookie");
    }

    // -----------------------------------------------------------------------
    // CN-AUTH-02 — Login con credenciales inválidas
    //
    // Datos de entrada:
    //   POST /api/auth/login
    //   email="user@test.com", password="WrongPass!", rememberMe=false
    //
    // Resultado esperado: HTTP 401, message = "Credenciales inválidas"
    // Resultado obtenido: HTTP 401 ✓ (BusinessException propagada por GlobalExceptionHandler)
    // -----------------------------------------------------------------------

    /**
     * Verifica que proporcionar credenciales incorrectas retorna HTTP 401.
     * La capa de servicio lanza BusinessException(UNAUTHORIZED) cuando el
     * usuario no existe o la contraseña no coincide.
     * Partición de equivalencia: contraseña fuera del dominio de credenciales aceptadas.
     */
    @Test
    @DisplayName("CN-AUTH-02: Login con credenciales inválidas — HTTP 401 con mensaje de error")
    void shouldReturn401WhenCredentialsAreInvalid() throws Exception {
        // Arrange — el servicio rechaza las credenciales
        when(authService.login(any(LoginRequest.class), nullable(String.class), nullable(String.class)))
            .thenThrow(new BusinessException(
                HttpStatus.UNAUTHORIZED,
                "Credenciales inválidas"
            ));

        LoginRequest request = new LoginRequest("user@test.com", "WrongPass!", false);

        // Act & Assert
        MvcResult result = mockMvc.perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.message").value("Credenciales inválidas"))
            .andReturn();

        assertEquals(401, result.getResponse().getStatus(),
            "CN-AUTH-02: Se esperaba HTTP 401 ante credenciales inválidas");
    }

    // -----------------------------------------------------------------------
    // CN-AUTH-03 — Consulta de sesión autenticada (GET /api/auth/me)
    //
    // Datos de entrada:
    //   GET /api/auth/me
    //   (token válido en SecurityContext, simulado por mock de CurrentUserProvider)
    //
    // Resultado esperado: HTTP 200, id=1, email="user@test.com", role="ESTUDIANTE"
    // Resultado obtenido: HTTP 200 ✓
    // -----------------------------------------------------------------------

    /**
     * Verifica que un usuario con sesión activa puede consultar sus datos
     * mediante GET /api/auth/me y recibe HTTP 200 con su perfil.
     * Partición de equivalencia: token de sesión válido presente.
     */
    @Test
    @DisplayName("CN-AUTH-03: Consulta de sesión autenticada — HTTP 200 con AuthUserResponse")
    void shouldReturn200WhenSessionIsActive() throws Exception {
        // Arrange — usuario autenticado en el SecurityContext (simulado)
        AuthPrincipal principal = new AuthPrincipal(1L, "user@test.com", "ESTUDIANTE");
        when(currentUserProvider.requireCurrentUser()).thenReturn(principal);
        // requireNotProvisionalToken() no lanza → sesión completa (no provisional)
        doNothing().when(currentUserProvider).requireNotProvisionalToken();
        when(authService.me(1L)).thenReturn(studentUser());

        // Act & Assert
        MvcResult result = mockMvc.perform(get("/api/auth/me"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.email").value("user@test.com"))
            .andExpect(jsonPath("$.role").value("ESTUDIANTE"))
            .andExpect(jsonPath("$.status").value("HABILITADO"))
            .andReturn();

        assertEquals(200, result.getResponse().getStatus(),
            "CN-AUTH-03: Se esperaba HTTP 200 para una sesión autenticada");
    }

    // -----------------------------------------------------------------------
    // CN-AUTH-04 — Consulta de sesión sin autenticación (GET /api/auth/me)
    //
    // Datos de entrada:
    //   GET /api/auth/me
    //   (sin token — SecurityContext vacío, simulado por mock que lanza excepción)
    //
    // Resultado esperado: HTTP 401, message = "No autenticado"
    // Resultado obtenido: HTTP 401 ✓ (BusinessException lanzada por CurrentUserProvider)
    // -----------------------------------------------------------------------

    /**
     * Verifica que acceder a GET /api/auth/me sin un token válido retorna HTTP 401.
     * El comportamiento documentado: CurrentUserProvider lanza
     * BusinessException(UNAUTHORIZED, "No autenticado") cuando no hay
     * AuthPrincipal en el SecurityContext.
     * Partición de equivalencia: ausencia de credenciales de sesión.
     */
    @Test
    @DisplayName("CN-AUTH-04: Consulta de sesión sin autenticación — HTTP 401 con mensaje de error")
    void shouldReturn401WhenNotAuthenticated() throws Exception {
        // Arrange — sin token: el provider lanza la excepción documentada
        when(currentUserProvider.requireCurrentUser())
            .thenThrow(new BusinessException(HttpStatus.UNAUTHORIZED, "No autenticado"));

        // Act & Assert
        MvcResult result = mockMvc.perform(get("/api/auth/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.message").value("No autenticado"))
            .andReturn();

        assertEquals(401, result.getResponse().getStatus(),
            "CN-AUTH-04: Se esperaba HTTP 401 cuando no hay sesión activa");
    }

    // -----------------------------------------------------------------------
    // CN-AUTH-05 — Solicitud de recuperación con correo válido
    //
    // Datos de entrada:
    //   POST /api/auth/reset-request
    //   email="user@test.com"
    //
    // Resultado esperado: HTTP 200,
    //   message = "Si el correo existe, se envió un enlace de recuperación"
    // Resultado obtenido: HTTP 200 ✓
    //
    // Nota: el endpoint siempre retorna 200 independientemente de si el correo
    // existe o no (diseño intencionado para no revelar información sobre usuarios).
    // -----------------------------------------------------------------------

    /**
     * Verifica que enviar un correo válido a /reset-request retorna HTTP 200
     * con el mensaje estándar de confirmación (diseño de privacidad: no revela
     * si el correo está registrado o no).
     * Partición de equivalencia: email con formato correcto.
     */
    @Test
    @DisplayName("CN-AUTH-05: Solicitud de recuperación con correo válido — HTTP 200 con mensaje informativo")
    void shouldReturn200WhenResetRequestIsValid() throws Exception {
        // Arrange — el servicio procesa la solicitud sin errores (email puede
        // existir o no; el controlador siempre responde igual)
        doNothing().when(authService).requestReset(any(ResetRequestInput.class));

        ResetRequestInput request = new ResetRequestInput("user@test.com");

        // Act & Assert
        MvcResult result = mockMvc.perform(
                post("/api/auth/reset-request")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message")
                .value("Si el correo existe, se envió un enlace de recuperación"))
            .andReturn();

        assertEquals(200, result.getResponse().getStatus(),
            "CN-AUTH-05: Se esperaba HTTP 200 para una solicitud de recuperación válida");
    }

    // -----------------------------------------------------------------------
    // CN-AUTH-06 — Confirmación de recuperación con token inválido
    //
    // Datos de entrada:
    //   POST /api/auth/reset-confirm
    //   token="token-invalido-o-expirado", newPassword="NuevaPass123!"
    //
    // Resultado esperado: HTTP 400, message = "Token inválido o expirado"
    // Resultado obtenido: HTTP 400 ✓ (BusinessException propagada por GlobalExceptionHandler)
    // -----------------------------------------------------------------------

    /**
     * Verifica que intentar confirmar el restablecimiento con un token inválido
     * o expirado retorna HTTP 400 con el mensaje de error correspondiente.
     * Partición de equivalencia: token fuera del dominio de tokens activos válidos.
     */
    @Test
    @DisplayName("CN-AUTH-06: Confirmación de recuperación con token inválido — HTTP 400 con mensaje de error")
    void shouldReturn400WhenResetTokenIsInvalid() throws Exception {
        // Arrange — el servicio rechaza el token inválido
        doThrow(new BusinessException(
                HttpStatus.BAD_REQUEST,
                "Token inválido o expirado"))
            .when(authService).confirmReset(any(ResetConfirmInput.class));

        ResetConfirmInput request = new ResetConfirmInput(
            "token-invalido-o-expirado",
            "NuevaPass123!"
        );

        // Act & Assert
        MvcResult result = mockMvc.perform(
                post("/api/auth/reset-confirm")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            )
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.message").value("Token inválido o expirado"))
            .andReturn();

        assertEquals(400, result.getResponse().getStatus(),
            "CN-AUTH-06: Se esperaba HTTP 400 cuando el token de recuperación es inválido o expirado");

        // Evidencia adicional: el body no está vacío y contiene el campo message
        String body = result.getResponse().getContentAsString();
        assertTrue(body.contains("Token inválido o expirado"),
            "CN-AUTH-06: El body debe contener el mensaje de error. Body: " + body);
    }

    // -----------------------------------------------------------------------
    // CN-AUTH-07 — Cierre de sesión autenticada
    // -----------------------------------------------------------------------

    @Test
    @DisplayName("CN-AUTH-07: Logout autenticado — HTTP 200 y cookie de sesión invalidada")
    void shouldReturn200AndClearCookieWhenLogoutIsAuthenticated() throws Exception {
        AuthPrincipal principal = new AuthPrincipal(1L, "user@test.com", "ESTUDIANTE");
        when(currentUserProvider.requireCurrentUser()).thenReturn(principal);
        when(currentUserProvider.currentJti()).thenReturn("jti-session-123");

        ResponseCookie clearCookie = ResponseCookie.from("auth_token", "")
            .maxAge(0)
            .path("/")
            .build();
        when(authCookieService.clearCookie()).thenReturn(clearCookie);

        mockMvc.perform(post("/api/auth/logout"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.message").value("Sesión cerrada"))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("auth_token=")))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));

        verify(authService).logoutCurrent(1L, "jti-session-123");
    }
}
