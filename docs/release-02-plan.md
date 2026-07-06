# Plan – Release 02 (Sprints 5 y 6)

## Visión general

Implementar las 10 historias del Release 2 **excluyendo** E1-H10 (mapa de campus), E1-H12 (búsqueda IA) y E5-H1 (dashboard analítico). El alcance cubre:

- **Sprint 5:** Gestión de cuenta (perfil, actividad, sesiones, seguridad avanzada).
- **Sprint 6:** Notificaciones avanzadas (suscripción a salas, alerta de liberación, comunicados institucionales, inasistencias).

El trabajo sigue los patrones del Release 1: DTOs como `record`, servicios `@Transactional`, cola de correos via `email_outbox`, `audit_log` para trazabilidad, y JWT + cookie HttpOnly para sesiones.

---

## Decisiones de diseño confirmadas

| Decisión | Elección |
|---|---|
| Revocación de sesiones (E4-H7) | Tabla `login_sessions` con JTI claim en el JWT. El filtro valida que el JTI no esté revocado. |
| Acciones que requieren confirmación por correo (E4-H9) | Cambio de contraseña, desactivación de 2FA, cierre global de sesiones. |
| Registro de inasistencias (E3-H10) | Columna `bookings.attendance_status` + tabla `attendance_records`. |
| Criterio de acceso inusual (E4-H6) | Combinación IP nunca vista **y** user-agent nunca visto → alerta. |
| Ubicación de nuevas vistas de perfil (frontend) | Nueva página `ProfilePage.tsx` con subnav de 4 pestañas: Mi perfil, Actividad, Sesiones activas, Seguridad. |

---

## Sub-tareas

---

### ST-01 · Migración de base de datos (Release 2)

**Status:** `[ ] pending`

**Intent:**  
Crear todos los objetos de base de datos nuevos en un único script SQL antes de que cualquier subtarea de backend intente usarlos. Así cada subtarea posterior puede asumir que el esquema ya existe.

**Expected Outcomes:**
- Script `database/004_release02.sql` ejecutable sobre la DB del Release 1 sin errores.
- Todas las tablas, columnas e índices necesarios para las 10 historias están disponibles.

**Todo List:**

1. Agregar columna `user_agent VARCHAR(512)` a `login_attempts`.
2. Crear tabla `login_sessions` con campos: `id`, `user_id`, `jti VARCHAR(36) UNIQUE`, `ip VARCHAR(64)`, `user_agent VARCHAR(512)`, `device_label VARCHAR(120)`, `created_at`, `last_seen_at`, `revoked_at TIMESTAMPTZ` (nullable), `current BOOLEAN DEFAULT FALSE`.  
   Índices: `(user_id, revoked_at)`, `(jti)`.
3. Crear tabla `sensitive_change_tokens` con campos: `id`, `user_id`, `action_type VARCHAR(40)`, `token VARCHAR(255) UNIQUE`, `payload TEXT`, `expires_at TIMESTAMPTZ`, `used BOOLEAN DEFAULT FALSE`, `created_at`.  
   Índice: `(token, used)`.
4. Agregar columna `attendance_status VARCHAR(20)` a `bookings` (nullable; check: `ASISTIO`, `INASISTIO`).
5. Crear tabla `attendance_records` con campos: `id`, `booking_id BIGINT UNIQUE` (FK → bookings), `user_id BIGINT` (FK → users), `recorded_at TIMESTAMPTZ`, `tolerance_minutes INTEGER`.  
   Índices: `(booking_id)`, `(user_id)`.
6. Crear tabla `room_availability_subscriptions` con campos: `id`, `user_id BIGINT`, `room_id BIGINT`, `target_date DATE`, `start_time TIME`, `end_time TIME`, `status VARCHAR(20) DEFAULT 'ACTIVA'` (check: `ACTIVA`, `NOTIFICADA`, `CANCELADA`), `created_at`, `notified_at TIMESTAMPTZ` (nullable).  
   Restricción única: `(user_id, room_id, target_date, start_time, end_time)` cuando `status = 'ACTIVA'`.  
   Índices: `(user_id)`, `(room_id, target_date, status)`.
7. Crear tabla `institutional_announcements` con campos: `id`, `author_user_id BIGINT` (FK → users), `title VARCHAR(160)`, `content TEXT`, `announcement_type VARCHAR(40)`, `status VARCHAR(20) DEFAULT 'PUBLICADO'`, `created_at`.  
   Índice: `(created_at DESC)`.

**Relevant Context:**
- `database/001_init.sql` — esquema actual de referencia.
- `database/002_seed_release01.sql` — datos semilla existentes.

---

### ST-02 · Sesiones con JTI y revocación (E4-H7)

**Status:** `[ ] pending`

**Intent:**  
Introducir el concepto de sesión trazable en el JWT. Cada token lleva un JTI (UUID) que se persiste en `login_sessions`. El `JwtAuthenticationFilter` verifica que el JTI no esté revocado. Esto habilita el cierre de sesiones remotas.

**Expected Outcomes:**
- El JWT generado contiene un campo JTI.
- Cada login crea un registro en `login_sessions`.
- El filtro de autenticación rechaza tokens con JTI revocado.
- `POST /api/auth/logout` revoca la sesión actual.
- `GET /api/me/sessions` lista sesiones activas del usuario.
- `DELETE /api/me/sessions/{sessionId}` revoca una sesión remota.
- `DELETE /api/me/sessions` revoca todas las sesiones del usuario.

**Todo List:**

1. **Modelo:** Crear `LoginSessionEntity` mapeando `login_sessions`.
2. **Repository:** Crear `LoginSessionRepository` con métodos:
   - `findByUsuarioAndRevokedAtIsNullOrderByCreatedAtDesc`
   - `findByJti`
   - `revokeAllByUsuario` (update masivo)
3. **JwtService:** Extender el payload del token para incluir `jti` (UUID). Nuevo formato de payload: `{jti}|{userId}|{role}|{provisional}|{exp}`. Ajustar `extractClaims()` y `generateToken()`.
4. **AuthService / LoginService:** Al completar login exitoso (estrategia estándar y 2FA), crear registro en `login_sessions` con `jti`, `ip` (del request), `user_agent`, `current=true`, `device_label` derivado del user-agent.
5. **JwtAuthenticationFilter:** Tras validar el token, consultar `LoginSessionRepository.findByJti()`. Si el registro no existe o `revokedAt != null`, rechazar con 401.
6. **AuthController:** Ajustar `POST /api/auth/logout` para revocar el JTI de la sesión actual (setear `revoked_at = NOW`).
7. **SessionController** (nuevo): Exponer `GET /api/me/sessions`, `DELETE /api/me/sessions/{sessionId}`, `DELETE /api/me/sessions`.
8. **DTOs:** `SessionResponse(id, ip, userAgent, deviceLabel, createdAt, lastSeenAt, current)`, `SessionListResponse(List<SessionResponse>)`.
9. **Frontend – ProfilePage.tsx:** Crear página con subnav de pestañas: `Mi perfil`, `Actividad`, `Sesiones activas`, `Seguridad`.  
   La pestaña "Sesiones activas" lista sesiones con opción de revocar cada una o todas.
10. **Frontend – routes.ts:** Agregar ruta `/perfil` con `RouteKey = 'profile'`.
11. **Frontend – GlobalTopbar.tsx:** Agregar acceso al perfil desde el topbar para ambos roles.
12. **Frontend – api.ts:** Agregar `getSessions()`, `revokeSession(id)`, `revokeAllSessions()`.

**Relevant Context:**
- `security/JwtService.java` — formato actual del token: `{userId}|{role}|{provisional}|{exp}`.
- `security/JwtAuthenticationFilter.java` — punto donde se valida el token.
- `service/auth/LoginService.java` y estrategias de login.
- `controller/AuthController.java` — `POST /api/auth/logout` actual.
- `viewmodels/routes.ts` — patrones de rutas existentes.

---

### ST-03 · Alerta por acceso inusual (E4-H6)

**Status:** `[ ] pending`

**Intent:**  
Tras un login exitoso, capturar `ip` y `user_agent`. Si **ambos** son nuevos para ese usuario (no aparecen combinados en `login_sessions` previas), encolar un correo de alerta de seguridad.

**Expected Outcomes:**
- El login captura IP y user-agent correctamente.
- Login desde combinación nueva de IP+UA envía correo de alerta.
- Login repetido desde dispositivo conocido no genera alerta.

**Todo List:**

1. En `LoginService` (o `AuthService`), tras crear la sesión en `login_sessions`, consultar si existen sesiones previas del usuario con la misma `(ip, user_agent)`.
2. Si no existen sesiones previas con esa combinación, llamar a `EmailOutboxService.enqueueSecurity()` con template de alerta.
3. Agregar template en `EmailTemplateService`: `accessAlert(String ip, String userAgent, OffsetDateTime when)` usando el método `branded()` o `alert()` existente.
4. Registrar evento en `audit_log` con acción `LOGIN_UNUSUAL_ACCESS`.

**Relevant Context:**
- ST-02 crea `login_sessions` que se reutiliza aquí.
- `EmailOutboxService.enqueueSecurity()` — bypass de preferencias.
- `EmailTemplateService.alert()` — template de alerta simple.
- `AuditService.record()` — registro de auditoría.

---

### ST-04 · Historial de actividad de cuenta (E2-H7)

**Status:** `[ ] pending`

**Intent:**  
Exponer el historial de actividad personal del usuario autenticado, leyendo de `audit_log` y `login_sessions`. Los eventos relevantes son: `LOGIN_SUCCESS`, `LOGOUT_CURRENT`, `LOGOUT_REMOTE`, `LOGOUT_ALL`, `SENSITIVE_CHANGE_CONFIRMED`, `LOGIN_UNUSUAL_ACCESS`.

**Expected Outcomes:**
- `GET /api/me/activity` devuelve lista paginada de eventos propios en orden descendente por fecha.
- Solo el usuario autenticado puede ver su historial.
- Los eventos muestran: tipo, fecha, IP, dispositivo (cuando aplica).

**Todo List:**

1. Asegurar que `LoginService` registre `LOGIN_SUCCESS` en `audit_log` con IP y user-agent en el campo `detalle`.
2. Asegurar que `AuthController.logout()` registre `LOGOUT_CURRENT` en `audit_log`.
3. Asegurar que `SessionController` registre `LOGOUT_REMOTE` o `LOGOUT_ALL` en `audit_log`.
4. Crear endpoint `GET /api/me/activity?page=0&size=20` en un nuevo `AccountController`.
5. **DTO:** `ActivityEventResponse(id, action, detail, ip, deviceLabel, createdAt)`.
6. **Query:** En `AuditLogRepository` agregar `findByActorOrderByCreadoEnDesc(UserEntity actor, Pageable p)`.
7. **Frontend – ProfilePage.tsx pestaña "Actividad":** Lista de eventos con fecha, ícono por tipo, IP y dispositivo.
8. **Frontend – api.ts:** Agregar `getMyActivity(page, size)`.

**Relevant Context:**
- `model/AuditLogEntity.java` — campos: `accion`, `entidad`, `entidadId`, `detalle`, `creadoEn`.
- `service/AuditService.java` — método `record()`.
- `repository/AuditLogRepository.java` — agregar query paginada por actor.

---

### ST-05 · Mi perfil en modo solo lectura (E2-H2)

**Status:** `[ ] pending`

**Intent:**  
La pestaña "Mi perfil" de `ProfilePage.tsx` muestra los datos del usuario autenticado en modo solo lectura. No hay campos editables para nombre, apellidos ni correo.

**Expected Outcomes:**
- La pestaña "Mi perfil" muestra: código, nombres, apellidos, correo, rol y estado.
- No existe ningún input editable en esa vista.
- Los datos se obtienen del endpoint `GET /api/auth/me` ya existente.

**Todo List:**

1. **Frontend – ProfilePage.tsx pestaña "Mi perfil":** Componente de solo lectura que muestra los campos del usuario usando `AuthUser` del estado de la app.
2. Verificar que `AuthUserResponse` del backend incluye todos los campos necesarios (`code`, `firstName`, `lastName`, `email`, `role`, `status`, `has2fa`). Está completo según exploración.
3. No requiere cambios en backend.

**Relevant Context:**
- `dto/auth/AuthUserResponse.java` — DTO actual con todos los campos necesarios.
- `controller/AuthController.java GET /api/auth/me` — endpoint ya existente.
- `models/types.ts` — tipo `AuthUser` en frontend.

---

### ST-06 · Confirmación por correo para cambios sensibles (E4-H9)

**Status:** `[ ] pending`

**Intent:**  
Antes de ejecutar: cambio de contraseña, desactivación de 2FA o cierre global de sesiones, el sistema solicita un token temporal enviado por correo. Solo tras confirmar el token se aplica la acción.

**Expected Outcomes:**
- `POST /api/auth/sensitive-change/request` acepta `{actionType, payload?}` y envía correo con token.
- `POST /api/auth/sensitive-change/confirm` acepta `{token}` y ejecuta la acción correspondiente.
- Token expirado o ya usado devuelve error.
- Acción sin token previo no puede ejecutarse directamente.

**Todo List:**

1. **Modelo:** Crear `SensitiveChangeTokenEntity` mapeando `sensitive_change_tokens`. Campos: `actionType` (enum: `CHANGE_PASSWORD`, `DISABLE_2FA`, `REVOKE_ALL_SESSIONS`), `token`, `payload TEXT`, `expiresAt`, `used`, `createdAt`.
2. **Repository:** `SensitiveChangeTokenRepository` con `findByTokenAndUsedFalse()`.
3. **Servicio:** `SensitiveChangeService` con:
   - `requestChange(userId, actionType, payload)` → genera token hasheado (reutilizar `SecretHashService`), persiste, encola correo.
   - `confirmChange(token)` → valida token, despacha acción según `actionType`, marca `used=true`.
4. **Acciones:**
   - `CHANGE_PASSWORD` → actualiza `user.passwordHash` desde `payload`.
   - `DISABLE_2FA` → desactiva `user.has2fa`.
   - `REVOKE_ALL_SESSIONS` → llama `SessionService.revokeAll(userId)`.
5. **Controller:** Agregar 2 endpoints en `AuthController`.
6. **Template de correo:** Reutilizar `EmailTemplateService.callToAction()` o `securityCode()` con el token/enlace.
7. **Registro:** Registrar `SENSITIVE_CHANGE_CONFIRMED` en `audit_log` al confirmar.
8. **Frontend – ProfilePage.tsx pestaña "Seguridad":** Sección con botones: "Cambiar contraseña", "Desactivar 2FA" (si activo), "Activar 2FA" (si inactivo), "Cerrar todas las sesiones". Al pulsar cada uno, mostrar mensaje "Revisa tu correo para confirmar".
9. **Frontend – api.ts:** Agregar `requestSensitiveChange(actionType, payload?)` y `confirmSensitiveChange(token)`.

**Relevant Context:**
- `security/SecretHashService.java` — hashing de tokens/códigos.
- `model/PasswordResetEntity.java` — patrón de token temporal de referencia.
- `service/auth/TwoFactorService.java` — patrón de flujo de 2 pasos.
- `service/EmailOutboxService.enqueueSecurity()` — envío sin chequear preferencias.

---

### ST-07 · Historial de intentos fallidos y bloqueos (admin) (E4-H10)

**Status:** `[ ] pending`

**Intent:**  
Vista administrativa de eventos de seguridad que consulta `login_attempts`. Filtra por usuario/correo, fecha y resultado.

**Expected Outcomes:**
- `GET /api/admin/security/login-attempts` devuelve lista paginada con filtros opcionales: `userId`, `email`, `from`, `to`, `success`.
- Solo accesible por administrador.
- Respuesta incluye: usuario, correo, IP, user-agent, fecha, éxito/fallo, bloqueo activo.

**Todo List:**

1. **Repository:** Agregar query en `LoginAttemptRepository` con filtros opcionales por `userId`, `email` (via join), `fechaIntento` range, `exito`.
2. **DTO:** `LoginAttemptAdminResponse(id, userId, userEmail, ip, userAgent, attemptedAt, success, lockedUntil)`.
3. **Controller:** Agregar endpoint en `AdminController`.
4. **Frontend – PerfilesPage.tsx:** Agregar tab "Seguridad" con tabla de intentos y filtros.
5. **Frontend – api.ts:** Agregar `getLoginAttempts(filters, page, size)`.

**Relevant Context:**
- `model/LoginAttemptEntity.java` — campos actuales: `usuario`, `fechaIntento`, `exito`, `ipOrigen`.
- ST-02 agrega `user_agent` a `login_attempts` (ya disponible tras ST-01/ST-02).
- `controller/AdminController.java` — controlador admin existente.

---

### ST-08 · Suscripción a disponibilidad de sala (E3-H13 + E3-H4)

**Status:** `[ ] pending`

**Intent:**  
Permitir que estudiantes se suscriban para recibir aviso cuando una sala ocupada se libere. Al cancelar una reserva, el sistema notifica a los suscriptores activos y marca las suscripciones como notificadas.

**Expected Outcomes:**
- `POST /api/rooms/{roomId}/availability-subscriptions` crea suscripción para `(room, date, start, end)`.
- `DELETE /api/rooms/{roomId}/availability-subscriptions/me` cancela suscripción activa.
- `GET /api/me/availability-subscriptions` lista suscripciones activas del usuario.
- Al cancelar una reserva, se notifica a suscriptores compatibles y se marca `status=NOTIFICADA`.
- No se crean duplicados de suscripción activa.

**Todo List:**

1. **Modelo:** Crear `RoomAvailabilitySubscriptionEntity` mapeando `room_availability_subscriptions`.
2. **Repository:** `RoomAvailabilitySubscriptionRepository` con:
   - `findActiveByUserAndRoom(userId, roomId, targetDate, startTime, endTime)` — para verificar duplicado.
   - `findActiveSubscriptionsForRoom(roomId, targetDate, startTime, endTime)` — para notificar al cancelar.
   - `findByUsuarioAndStatusActiva(userId)` — para el listado del usuario.
3. **DTOs:** `AvailabilitySubscriptionRequest(targetDate, startTime, endTime)`, `AvailabilitySubscriptionResponse(id, roomId, roomName, targetDate, startTime, endTime, status, createdAt)`.
4. **Servicio:** `AvailabilitySubscriptionService` con `subscribe()`, `unsubscribe()`, `getMySubscriptions()`.
5. **Controller:** `RoomController` (agregar los 2 endpoints de suscripción) + `AccountController` (endpoint listado propio).
6. **Notificación al cancelar:** En `BookingService.cancelBooking()`, tras cancelar, llamar a `AvailabilitySubscriptionService.notifySubscribers(room, date, start, end)`:
   - Busca suscripciones activas compatibles.
   - Para cada una, encola correo via `EmailOutboxService.enqueue()` con template de aviso.
   - Marca `status=NOTIFICADA`, `notified_at=NOW`.
7. **Template de correo:** Agregar en `EmailTemplateService` método `roomAvailableAlert(roomName, targetDate, startTime, endTime)`.
8. **Frontend – ReservasPage.tsx:** Mostrar botón "Avisarme cuando se libere" si la sala está ocupada en el horario seleccionado (solo para estudiantes).
9. **Frontend – MisReservasPage.tsx:** Listar suscripciones activas del usuario con opción de cancelar.
10. **Frontend – api.ts:** Agregar `subscribeToRoom()`, `unsubscribeFromRoom()`, `getMySubscriptions()`.

**Relevant Context:**
- `service/BookingService.cancelBooking()` — punto de integración para notificación.
- `service/EmailOutboxService.enqueue()` — respetar preferencias de notificación.
- `model/ReservationEntity.java` — entidad de reserva con sala, fecha y horario.
- Tabla `room_availability_subscriptions` creada en ST-01.

---

### ST-09 · Comunicados institucionales (E3-H9)

**Status:** `[ ] pending`

**Intent:**  
El administrador puede publicar un comunicado institucional que se envía masivamente por correo a todos los estudiantes activos.

**Expected Outcomes:**
- `POST /api/admin/announcements` crea el comunicado y encola correos a todos los estudiantes con estado HABILITADO.
- Solo accesible por administrador.
- El comunicado queda registrado en `institutional_announcements`.
- Los correos respetan `notification_preferences.email_enabled`.

**Todo List:**

1. **Modelo:** Crear `InstitutionalAnnouncementEntity` mapeando `institutional_announcements`.
2. **Repository:** `InstitutionalAnnouncementRepository`.
3. **DTOs:** `AnnouncementRequest(title, content, announcementType)`, `AnnouncementResponse(id, title, announcementType, createdAt, recipientCount)`.
4. **Servicio:** `AnnouncementService.publish(authorId, request)`:
   - Persiste el comunicado.
   - Carga todos los usuarios con `status=HABILITADO` y `role=ESTUDIANTE`.
   - Para cada uno llama `EmailOutboxService.enqueue()` — respeta preferencias del usuario.
   - Retorna `recipientCount`.
5. **Template de correo:** Reutilizar `EmailTemplateService.branded()` con el título y contenido del comunicado.
6. **Controller:** Agregar endpoint en `AdminController`.
7. **Frontend – SalasPage.tsx:** Agregar sección de comunicados con formulario de `titulo`, `contenido`, `tipo` (select: `NUEVA_SALA`, `CAMBIO_POLITICA`, `AVISO_GENERAL`) y botón "Publicar comunicado". Visible solo para administrador.
8. **Frontend – api.ts:** Agregar `publishAnnouncement(request)`.

**Relevant Context:**
- `repository/UserRepository.java` — consultar usuarios HABILITADO con rol ESTUDIANTE.
- `service/EmailOutboxService.enqueue()` — con payload de tipo `ANNOUNCEMENT`.
- `controller/AdminController.java` — controlador admin existente.

---

### ST-10 · Registro automático de inasistencias (E3-H10)

**Status:** `[ ] pending`

**Intent:**  
Un job programado detecta reservas activas cuya hora de inicio pasó hace más de 15 minutos sin registro de asistencia, las marca como inasistencia y notifica al estudiante.

**Expected Outcomes:**
- El job corre periódicamente (cada 5 minutos).
- Reservas activas con `booking_date = hoy` y `start_time <= ahora - 15min` sin `attendance_status` se marcan como `INASISTIO`.
- Se crea un registro en `attendance_records`.
- Se encola correo de aviso al estudiante.
- La misma reserva no se marca dos veces.

**Todo List:**

1. **Modelo:** Crear `AttendanceRecordEntity` mapeando `attendance_records`.
2. **Repository:** `AttendanceRecordRepository` con `existsByReserva(bookingId)`.
3. **Repository:** En `ReservationRepository`, agregar query `findActiveBookingsMissedBefore(LocalDate today, LocalTime cutoffTime)` — busca reservas con `estado=ACTIVA`, `booking_date=today`, `start_time <= cutoffTime` y `attendance_status IS NULL`.
4. **Servicio:** `AttendanceService.processMissedBookings()`:
   - Calcula `cutoffTime = ahora - 15 minutos`.
   - Carga reservas elegibles.
   - Para cada una: verificar que no existe `AttendanceRecord` (idempotencia).
   - Actualizar `booking.attendance_status = INASISTIO`.
   - Crear `AttendanceRecord`.
   - Encolar correo via `EmailOutboxService.enqueue()`.
5. **Scheduler:** `AttendanceScheduler` con `@Scheduled(fixedDelay = 300_000)` (5 min) que llama `AttendanceService.processMissedBookings()`.
6. **Template de correo:** Agregar `EmailTemplateService.absenceNotice(ReservationEntity booking)`.
7. **Admin view (opcional):** En `AdminReservasPage.tsx` o `PerfilesPage.tsx`, la columna de reservas puede mostrar el `attendance_status`.
8. **Frontend – MisReservasPage.tsx:** Mostrar `attendance_status` en el listado de reservas del estudiante (p.ej. badge "Inasistencia").
9. **Frontend – api.ts:** Si se necesita endpoint de consulta de inasistencias, agregar `getAttendanceHistory()`.

**Relevant Context:**
- Columna `bookings.attendance_status` y tabla `attendance_records` creadas en ST-01.
- `service/booking/command/BookingReminderScheduler.java` — patrón de scheduler de referencia.
- `service/EmailOutboxService.enqueue()` — con payload de tipo `ABSENCE_NOTICE`.
- `dto/booking/BookingResponse.java` — extender con `attendanceStatus` si es necesario.

---

## Orden de ejecución

```
ST-01  → ST-02  → ST-03
                  ST-04  (depende de ST-02 para LOGOUT_CURRENT)
                  ST-05  (independiente, solo frontend)
ST-02  → ST-06  (depende de revokeAll de sesiones)
         ST-07  (depende de user_agent en login_attempts, ST-01)
ST-01  → ST-08
         ST-09  (independiente del grupo anterior)
ST-01  → ST-10
```

Las sub-tareas ST-05, ST-09 y ST-10 pueden implementarse en paralelo con el grupo de sesiones una vez que ST-01 esté completa.

---

## Nuevas tablas y columnas (resumen)

| Objeto | Tabla/Columna |
|---|---|
| Columna | `login_attempts.user_agent` |
| Tabla | `login_sessions` |
| Tabla | `sensitive_change_tokens` |
| Columna | `bookings.attendance_status` |
| Tabla | `attendance_records` |
| Tabla | `room_availability_subscriptions` |
| Tabla | `institutional_announcements` |

## Nuevos endpoints (resumen)

| Método | Ruta | Historia |
|---|---|---|
| GET | `/api/me/sessions` | E4-H7 |
| DELETE | `/api/me/sessions/{id}` | E4-H7 |
| DELETE | `/api/me/sessions` | E4-H7 |
| GET | `/api/me/activity` | E2-H7 |
| GET | `/api/me/availability-subscriptions` | E3-H13 |
| POST | `/api/auth/sensitive-change/request` | E4-H9 |
| POST | `/api/auth/sensitive-change/confirm` | E4-H9 |
| GET | `/api/admin/security/login-attempts` | E4-H10 |
| POST | `/api/admin/announcements` | E3-H9 |
| POST | `/api/rooms/{roomId}/availability-subscriptions` | E3-H13 |
| DELETE | `/api/rooms/{roomId}/availability-subscriptions/me` | E3-H13 |

## Nuevas páginas / rutas frontend

| Componente | Ruta | Descripción |
|---|---|---|
| `ProfilePage.tsx` | `/perfil` | Subnav: Mi perfil / Actividad / Sesiones activas / Seguridad |
