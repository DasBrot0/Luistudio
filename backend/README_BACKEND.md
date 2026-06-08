# Backend - Luistudio (Release 01)

## Stack

- Java 21
- Spring Boot 3.x
- Spring Security + token cifrado custom
- Spring Data JPA + Hibernate
- PostgreSQL (o H2 en memoria para desarrollo rapido)

## Ejecución local

1. Asegura `JAVA_HOME` apuntando a Java 21.
2. (Opcional recomendado) crear BD PostgreSQL `luistudio_db` y ejecutar `database/001_init.sql`.
3. Variables de entorno opcionales:
   - `DB_URL`
   - `DB_USER`
   - `DB_PASSWORD`
   - `DB_DRIVER` (por defecto H2)
   - `JWT_SECRET`
   - `BCRYPT_STRENGTH` (`10` por defecto; puede subirse a `11` o `12` en producción real)
   - `CORS_ORIGINS`
   - `AUTH_COOKIE_SECURE` (`false` en local, `true` en despliegue HTTPS)
   - `AUTH_COOKIE_SAME_SITE` (`Lax` por defecto)
   - `EMAIL_PROVIDER` (`log` por defecto, `resend` para envío real por API HTTP)
   - `EMAIL_FROM` (ej. `Luistudio <no-reply@tu-dominio.com>`)
  - `RESEND_API_KEY` (requerida si `EMAIL_PROVIDER=resend`)
  - `APP_LOG_LEVEL` (`DEBUG` por defecto para `com.luistudio.reservas` en desarrollo; usar `INFO` en produccion)
4. Ejecuta:

```bash
./mvnw spring-boot:run
```

Si usas el script `scripts/start-backend.ps1`, este carga variables automáticamente desde:
- `/.env` (raiz del proyecto), o
- `/backend/reservas/.env` (si no existe el de raiz).

## Despliegue en Render (Docker)

En este proyecto puedes desplegar el backend usando:
- `backend/reservas/Dockerfile`
- `backend/reservas/.dockerignore`

Sugerencias de configuración en Render:
- `Root Directory`: `backend/reservas`
- Runtime: `Docker`
- Variables de entorno minimas:
  - `DB_URL`
  - `DB_USER`
  - `DB_PASSWORD`
  - `DB_DRIVER=org.postgresql.Driver`
  - `JWT_SECRET`
  - `CORS_ORIGINS`
  - `AUTH_COOKIE_SECURE=true`

Memoria (500 MB):
- El Dockerfile ya incluye ajustes JVM para memoria limitada (`MaxRAMPercentage=70`, `SerialGC`).
- Con tráfico bajo/moderado suele funcionar bien en 500 MB.
- Si sube la concurrencia, exportaciones pesadas o procesos simultáneos, puede haber reinicios por OOM; en ese caso conviene subir a 1 GB.

## Endpoints principales (Release 01)

## Observabilidad

- Cada request recibe o reutiliza `X-Request-Id`; el valor se agrega al MDC c?mo `requestId` y se devuelve en la respuesta.
- El patron de logs incluye `requestId`.
- Los logs evitan correos, códigos, tokens, cookies, cuerpos completos y datos personales; para flujos de usuario se usan rol o hashes técnicos.

### Auth y seguridad

- `POST /api/auth/login`
- `POST /api/auth/logout`
- `GET /api/auth/me`
- `POST /api/auth/reset-request`
- `POST /api/auth/reset-confirm`
- `POST /api/auth/2fa/enroll`
- `POST /api/auth/2fa/confirm`
- `POST /api/auth/2fa/verify`
- `POST /api/auth/2fa/disable`

### Salas y disponibilidad

- `GET /api/rooms?page&size&includeSchedule&campus&recinto&ubicacion&q`
  - Devuelve `PageResponse<RoomResponse>`. Por defecto `includeSchedule=false` para listar salas sin cargar horarios completos.
- `GET /api/rooms/available?fecha&horaInicio&horaFin`
- `GET /api/rooms/{id}/bookings?desde&hasta`
- `POST /api/rooms`
- `PUT /api/rooms/{id}`
- `PATCH /api/rooms/{id}`
- `DELETE /api/rooms/{id}`
- `POST /api/rooms/{id}/unavailability`
- `GET /api/rooms/{id}/unavailability`

### Reservas

- `POST /api/bookings`
- `PUT /api/bookings/{id}`
- `PATCH /api/bookings/{id}/cancel`
- `GET /api/bookings/me?page&size`
  - Devuelve `PageResponse<BookingResponse>`.
- `GET /api/bookings/{id}/ics` descarga un archivo calendario para reservas confirmadas propias del estudiante autenticado. Convierte horarios locales con zona `America/Lima` a UTC.
- `GET /api/admin/bookings`

### Administración

- `GET /api/admin/users`
  - Soporta `query` por código/correo/nombres/apellidos, `year`, `status` (`HABILITADO`, `DESHABILITADO`, `BLOQUEADO`), `sortBy` (`firstName`, `lastName`, `code`, `status`) y `sortDir`.
- `PATCH /api/admin/users/{id}/estado`
  - No permite que un administrador se deshabilite a si mismo ni deshabilitar al ultimo administrador habilitado.
- `GET /api/admin/config`
- `PUT /api/admin/config`
- `GET /api/admin/campus-schedules`
- `PUT /api/admin/campus-schedules`
  - En días cerrados (`closed=true`), `openTime` y `closeTime` pueden enviarse c?mo `null`.
  - En días abiertos, `openTime` y `closeTime` deben alinearse con la duración por reserva del campus (30/45/60/120 min).
  - Los errores de validación incluyen el campo que falló.
- `GET /api/campus/map`

### Preferencias

- `GET /api/me/preferences`
- `PUT /api/me/preferences`
  - Incluye preferencias de notificaciones + UI (`themeMode`, `fontScale`).
  - `notificationSettings` persiste por usuario los canales `app` y `email` por tipo de aviso, filtrados por rol.

### Usuarios (consulta para reservas)

- `GET /api/users/lookup?code=...`
  - Requiere usuario autenticado.
  - Devuelve código y nombre completo para validar participantes de una reserva.

## Notas de implementacion

- Se implementa bloqueo temporal tras intentos fallidos de login.
- El costo BCrypt es configurable con `BCRYPT_STRENGTH`; para demo/nube barata se usa `10` por defecto.
- El login usa búsqueda por correo con índice funcional `LOWER(email)` y evita escrituras/flushes innecesarios en el flujo exitoso.
- CORS cachea preflight por 3600 segundos; los `GET` simples del frontend no envian `Content-Type` para evitar `OPTIONS` innecesarios.
- Para bases existentes, aplicar `database/004_optimize_auth_indexes.sql` para crear los índices de autenticación.
- El listado administrativo de usuarios expone si una cuenta sigue bloqueada temporalmente y permite desbloquearla al volver a `HABILITADO`.
- Se implementa 2FA por código temporal.
- Los tokens de sesión y tokens provisionales viajan cifrados y se entregan en cookie `HttpOnly`.
- Los tokens de recuperación y códigos 2FA se almacenan hasheados; no se persisten en texto plano.
- Las entidades de recuperación y 2FA mapean el estado usado/no usado a la columna SQL `used`.
- Para bases existentes, aplicar `database/006_harden_auth_secrets.sql` antes de desplegar este refuerzo.
- `email_outbox` se procesa por scheduler (reintentos automáticos).
- En local, sin `RESEND_API_KEY`, el envío de correos cae en modo log (no SMTP).
- Para despliegue en Render, usar `EMAIL_PROVIDER=resend` + `RESEND_API_KEY` (salida por HTTPS/443).
- Se generan enlaces Google Calendar y descarga `.ics` por reserva.
- El esquema SQL base (`database/001_init.sql` y `database/002_seed_release01.sql`) define tablas y columnas para instalaciones desde cero.
- Los tests de backend incluyen un contrato que compara entidades JPA contra `database/001_init.sql` para detectar tablas/columnas desalineadas antes de desplegar.
- Para bases existentes, aplicar los scripts incrementales disponibles en `/database` antes de levantar el backend.
- Las salas almacenan data de catalogo en espanol (`code`, `name`, `campus`, `venue`, `location`).
- El listado de salas usa paginación y respuesta ligera por defecto; la creación/edición conserva horario completo en la respuesta.
- Los listados de reservas que se mapean a DTO cargan usuario y sala con `EntityGraph` para evitar N+1.
- El mapa de campus agrupa salas por pabellón desde una carga única y resuelve ocupación/mantenimiento con sets por id.
- El scheduler de `email_outbox` procesa c?mo máximo 50 correos listos por ciclo.
- Se agregan horarios por campus (`campus_schedules`) y override por sala (`room_schedules`) para validar reservas por día/hora.
- Se agregan reglas por sala de personas: `minPeople`, `minPeopleRequired`, `maxPeople`.
- Se agrega configuración de duración por reserva por campus (Monterrico 60 min, Mayorazgo 45 min por defecto).
- Las reservas solo aceptan fechas/horas dentro de la ventana permitida (semana actual; fin de semana habilita también la siguiente semana) y siempre en horas futuras del día actual.
- Las reglas de negocio de reservas usan la zona `America/Lima` para comparar fecha/hora actual en producción.
- No se permite cancelar reservas que ya finalizaron (validaci?n en backend).
- La cancelación administrativa reutiliza `PATCH /api/bookings/{id}/cancel`; el frontend ahora agrega confirmación previa y el backend mantiene el envío de correo automático.
- Se agrega preferencia por usuario `login_landing_view` para definir la vista inicial al iniciar sesión (validada por rol).
- Para bases ya existentes, aplicar `database/004_add_login_landing_view.sql`.
- Para convertir data previa de salas (ingles -> espanol en `code/name/campus/venue/location`), aplicar `database/005_rooms_data_to_spanish.sql`.

## Keep-alive en Render con UptimeRobot

Si despliegas en Render y quieres evitar que el backend entre en reposo por inactividad, puedes usar UptimeRobot para hacer ping cada 5 minutos.

1. Verifica endpoint de salud publico:
   - URL recomendada: `GET /actuator/health`
   - En este proyecto:
     - Actuator está habilitado.
     - Solo se expone `health`.
     - `GET /actuator/health` y `HEAD /actuator/health` están permitidos sin autenticación.
2. Crea cuenta gratuita en UptimeRobot (hasta 50 monitores, intervalo mínimo 5 min).
3. Crea monitor:
   - `Monitor Type`: `HTTP(s)`
   - `Friendly Name`: `Luistudio Backend`
   - `URL`: `https://<tu-servicio-render>.onrender.com/actuator/health`
   - `Monitoring Interval`: `5 minutes`
4. Guarda el monitor y v?lida que reciba `200 OK`.

Nota:
- No uses `GET /api/rooms` para keep-alive si no mandas token, porque ese endpoint requiere autenticación.

## Patrones aplicados
## Patrones aplicados

- `Strategy`:
  - Login con `LoginContext`, `LoginStrategy`, `StandardLoginStrategy` y `TwoFactorLoginStrategy`.
- `Adapter`:
  - Email con `EmailGateway` c?mo target, `GmailEmailAdapter`/`ResendEmailAdapter` c?mo adapters y adaptees explicitos para Gmail/Resend.
  - `LogEmailGateway` es fallback local, no adapter externo.
- `Command`:
  - Recordatorios con `BookingReminderCommand`, comandos concretos, `BookingReminderCommandManager` y scheduler.
- `Facade`:
  - `AuthService` conserva el contrato de autenticación y delega en `LoginService`, `PasswordResetService`, `TwoFactorService` y `LoginAttemptService`.

Como arquitectura, no c?mo GoF: `BookingValidationService` concentra validaciones de reserva; `repository/*Repository.java` usa Spring Data JPA; `DtoMapper` y `dto/**` manejan DTOs. No se implementa Factory Method ni Singleton manual.


- Regla de no duplicación de reservas: si el usuario vuelve a reservar exactamente el mismo recurso, fecha y horario (misma identidad lógica), se reutiliza el mismo registro en BD y se actualiza su estado/datos; no se crea una fila adicional. La cantidad de personas no define identidad para est? regla.

## Envío de correo por Gmail API (sin SMTP)

Configura estas variables de entorno para usar Gmail c?mo proveedor de correo:

- EMAIL_PROVIDER=gmail
- EMAIL_FROM=Luistudio <iaboysender@gmail.com>
- GMAIL_CLIENT_ID=...
- GMAIL_CLIENT_SECRET=...
- GMAIL_REFRESH_TOKEN=...

Si falta alguna credencial, el sistema hace fallback a `log` y deja warning en logs.
- Correos de reservas (confirmación, edición, cancelación) se generan en HTML con estilo Luistudio e incluyen: sala, campus, recinto, ubicación, fecha, horario, personas e integrantes.
- La duración máxima de reserva se v?lida por bloque configurado del campus de la sala (no por un límite global único).
- El admin puede cambiar la duración por campus, pero el sistema bloquea el cambio si existen reservas futuras activas en ese campus para evitar conflictos.
- Los endpoints de escritura validan longitudes y formatos de entrada alineados con los tamaños de columna (ej. estado VARCHAR(20), observaciones/motivos VARCHAR(255), textos de sala VARCHAR(120/160)), para evitar errores SQL por datos demasiado largos.

- 2FA opcional configurable por usuario desde Configuración: activar/desactivar requiere código de confirmación enviado por correo.
- Los correos salientes se renderizan con `EmailTemplateService`, una plantilla HTML unificada para alertas, recuperación de contraseña, 2FA, reservas y recordatorios; no hay layouts HTML duplicados en los servicios de negocio.
- Correos de 2FA (activación, desactivación e inicio de sesión) ajustados con tildes correctas y render de código sin duplicados en plantilla HTML.
- Para desarrollo, el backend incluye spring-boot-devtools (runtime) para reinicio automático al detectar cambios mientras se ejecuta ./mvnw spring-boot:run.
- El admin puede cambiar salas a disponible, mantenimiento o inactiva; inactivar/eliminar una sala se bloquea si tiene reservas activas en curso o futuras.
- La preferencia de vista inicial para administradores acepta Salas, Perfiles y Reservas.
- Las preferencias de notificación por usuario se guardan en `notification_preferences.notification_settings`; los correos de reservas y recordatorios respetan el canal Email configurado.
