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
  - `GROQ_API_KEY` (requerida para la búsqueda inteligente; no se expone al frontend)
  - `GROQ_MODEL` (por defecto `openai/gpt-oss-20b`)
  - `APP_LOG_LEVEL` (`DEBUG` por defecto para `com.luistudio.reservas` en desarrollo; usar `INFO` en produccion)
4. Ejecuta:

```bash
./mvnw spring-boot:run
```

Si usas el script `scripts/start-backend.ps1`, este carga variables automáticamente desde:
- `/.env` (raiz del proyecto), o
- `/backend/reservas/.env` (si no existe el de raiz).

## Desarrollo con Docker

Desde la raíz del proyecto ejecuta:

```bash
docker compose up
```

El servicio `backend` monta el código fuente y ejecuta un watcher que recompila al guardar cambios; `spring-boot-devtools` reinicia la aplicación cuando detecta las clases actualizadas. No necesitas reconstruir ni reiniciar el contenedor para cambios normales de Java o recursos.

## Pruebas

```bash
./mvnw test
```

La suite incluye pruebas de caja negra de autenticación, reservas y administración de salas; pruebas unitarias de cookies, JWT, cancelación, exportación ICS, paginación y validaciones de reserva; además del contrato entre entidades JPA y el esquema SQL.

JaCoCo instrumenta las pruebas automáticamente y genera el reporte de cobertura en `target/site/jacoco/index.html`. El reporte CSV para anexar como evidencia queda en `target/site/jacoco/jacoco.csv`.

Las exportaciones ICS y los enlaces de Google Calendar construyen la ubicación desde la jerarquía persistida `sala -> pabellón -> campus`. Cuando `buildings` tiene coordenadas, el texto incluye latitud/longitud y el ICS agrega la propiedad `GEO`.

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
- `POST /api/auth/2fa/disable/confirm`
- `POST /api/auth/sensitive-change/confirm`
- `GET /api/me/sessions`
- `DELETE /api/me/sessions/{sessionId}`
- `DELETE /api/me/sessions`
- `GET /api/me/activity`

### Salas y disponibilidad

- `GET /api/rooms?page&size&includeSchedule&campus&recinto&ubicacion&q`
  - Devuelve `PageResponse<RoomResponse>`. Por defecto `includeSchedule=false` para listar salas sin cargar horarios completos.
- `POST /api/rooms/intelligent-search`
  - Requiere sesión y recibe `{ "query", "date", "start", "end", "limit" }`.
  - Groq interpreta `query` a una intención tipada usando solo el catálogo público de salas activas. El backend valida esa intención, filtra por disponibilidad y metadata y calcula un ranking determinista; la IA no decide qué sala se reserva.
  - La respuesta devuelve `score` y `reasons` construidos por reglas locales para cada alternativa.
- `GET /api/rooms/{id}/bookings?desde&hasta`
- `POST /api/rooms`
- `PUT /api/rooms/{id}`
- `DELETE /api/rooms/{id}`
- `POST /api/rooms/{id}/availability-subscriptions`
- `DELETE /api/rooms/{id}/availability-subscriptions/me`
- `GET /api/me/availability-subscriptions`

### Reservas

- `POST /api/bookings`
- `PUT /api/bookings/{id}`
- `PATCH /api/bookings/{id}/cancel`
- `GET /api/bookings/me?page&size`
  - Devuelve `PageResponse<BookingResponse>`.
- `GET /api/bookings/{id}/ics` descarga un archivo calendario para reservas confirmadas propias del estudiante autenticado. Convierte horarios locales con zona `America/Lima` a UTC.
- `GET /api/admin/bookings`
- `GET /api/admin/attendance?query&campus&pavilion&status&from&to&page&size&sortBy&sortDir`
- `PATCH /api/admin/attendance/{bookingId}`

### Administración

- `GET /api/admin/dashboard?from=YYYY-MM-DD&to=YYYY-MM-DD`
  - Calcula disponibilidad con el horario específico de cada sala. Si una sala no tiene horario para un día, hereda el horario general de su campus; un horario propio, incluso cerrado, siempre prevalece como override.
- `GET /api/admin/users`
  - Soporta `query` por código/correo/nombres/apellidos, `year`, `status` (`HABILITADO`, `DESHABILITADO`, `BLOQUEADO`), `sortBy` (`firstName`, `lastName`, `code`, `status`) y `sortDir`.
- `PATCH /api/admin/users/{id}/estado`
  - No permite que un administrador se deshabilite a si mismo ni deshabilitar al ultimo administrador habilitado.
- `GET /api/admin/config`
- `PUT /api/admin/config`
- `GET /api/admin/campus-schedules`
- `PUT /api/admin/campus-schedules`
  - En días cerrados (`closed=true`), `openTime` y `closeTime` pueden enviarse c?mo `null`.
  - En días abiertos, `openTime` y `closeTime` deben usar horas exactas (`:00`). La duración por reserva solo admite 30 o 60 minutos.
  - Los errores de validación incluyen el campo que falló.
- `GET /api/campus/map`
- `PUT /api/admin/buildings/{id}/location`
- `GET /api/admin/security/login-attempts`
- `POST /api/admin/announcements`

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
- Los índices de autenticación forman parte de `database/001_init.sql`.
- La estructura y los datos iniciales de búsqueda inteligente están consolidados en `database/001_init.sql` y `database/002_seed_release01.sql`.
- `database/002_seed_release01.sql` incluye datos de demostración idempotentes para Dashboard, Mapa y Seguridad: 100 estudiantes con estados y 2FA variados, una cuenta temporalmente bloqueada (`DEMO005`), 720 reservas históricas repartidas uniformemente de lunes a sábado y entre las 08:00 y 18:00, 120 reservas futuras, ocupaciones actuales, horarios específicos por sala, un mantenimiento activo e intentos de acceso desde distintos dispositivos. Las filas controladas se identifican con valores `DEMO_*` para poder regenerarlas sin duplicados.
- El listado administrativo de usuarios expone si una cuenta sigue bloqueada temporalmente y permite desbloquearla al volver a `HABILITADO`.
- Se implementa 2FA por código temporal.
- Los tokens de sesión y tokens provisionales viajan cifrados y se entregan en cookie `HttpOnly`.
- Los tokens de recuperación y códigos 2FA se almacenan hasheados; no se persisten en texto plano.
- Las entidades de recuperación y 2FA mapean el estado usado/no usado a la columna SQL `used`.
- El esquema reforzado de autenticación está consolidado en `database/001_init.sql`.
- `email_outbox` se procesa por scheduler (reintentos automáticos).
- En local, sin `RESEND_API_KEY`, el envío de correos cae en modo log (no SMTP).
- Para despliegue en Render, usar `EMAIL_PROVIDER=resend` + `RESEND_API_KEY` (salida por HTTPS/443).
- Se generan enlaces Google Calendar y descarga `.ics` por reserva.
- El esquema SQL base (`database/001_init.sql` y `database/002_seed_release01.sql`) define tablas y columnas para instalaciones desde cero.
- Los tests de backend incluyen un contrato que compara entidades JPA contra `database/001_init.sql` para detectar tablas/columnas desalineadas antes de desplegar.
- Para una instalación limpia, ejecutar `001_init.sql` y después `002_seed_release01.sql`; el script `003_drop_all_tables.sql` permite reiniciarla.
- El catálogo está normalizado como `campuses -> buildings -> rooms`: campus y recinto se derivan del pabellón, y la sala solo guarda su ubicación interna.
- El campus Monterrico incluye el `Edificio M - Biblioteca Antonio Pinilla`, ubicado aproximadamente sobre la referencia del mapa institucional, con salas de estudio y visionado entre los pisos 2 y 7.
- El listado de salas usa paginación y respuesta ligera por defecto; la creación/edición conserva horario completo en la respuesta.
- Los listados de reservas que se mapean a DTO cargan usuario y sala con `EntityGraph` para evitar N+1.
- El mapa de campus agrupa salas por pabellón desde una carga única y resuelve ocupación/mantenimiento con sets por id.
- El scheduler de `email_outbox` procesa c?mo máximo 50 correos listos por ciclo.
- Se agregan horarios por campus (`campus_schedules`) y override por sala (`room_schedules`) para validar reservas por día/hora.
- Se agregan reglas por sala de personas: `minPeople`, `minPeopleRequired`, `maxPeople`.
- La duración por reserva se configura por campus con 60 minutos por defecto tanto en Monterrico como en Mayorazgo; el administrador puede elegir 30 o 60 minutos.
- Las reservas solo aceptan fechas/horas dentro de la ventana permitida (semana actual; fin de semana habilita también la siguiente semana) y siempre en horas futuras del día actual.
- Las reglas de negocio de reservas usan la zona `America/Lima` para comparar fecha/hora actual en producción.
- No se permite cancelar reservas que ya finalizaron (validaci?n en backend).
- La cancelación administrativa reutiliza `PATCH /api/bookings/{id}/cancel`; el frontend ahora agrega confirmación previa y el backend mantiene el envío de correo automático.
- Se agrega preferencia por usuario `login_landing_view` para definir la vista inicial al iniciar sesión (validada por rol).
- La vista inicial y el catálogo actual de salas están consolidados en `database/001_init.sql` y `database/002_seed_release01.sql`.

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
- Las cuentas institucionales `@aloe.ulima.edu.pe` incluidas en el seed se crean con 2FA desactivado; cada usuario puede activarlo posteriormente desde Configuración.
- Los correos salientes se renderizan con `EmailTemplateService`, una plantilla HTML unificada para alertas, recuperación de contraseña, 2FA, reservas y recordatorios; no hay layouts HTML duplicados en los servicios de negocio.
- Correos de 2FA (activación, desactivación e inicio de sesión) ajustados con tildes correctas y render de código sin duplicados en plantilla HTML.
- Para desarrollo, el backend incluye spring-boot-devtools (runtime) para reinicio automático al detectar cambios mientras se ejecuta ./mvnw spring-boot:run.
- El admin puede cambiar salas a disponible, mantenimiento o inactiva; inactivar/eliminar una sala se bloquea si tiene reservas activas en curso o futuras.
- La preferencia de vista inicial para administradores acepta Salas, Perfiles y Reservas.
- Las preferencias de notificación por usuario se guardan en `notification_preferences.notification_settings`; los correos de reservas y recordatorios respetan el canal Email configurado.
## Dashboard administrativo y búsqueda inteligente

- `GET /api/admin/dashboard?from=YYYY-MM-DD&to=YYYY-MM-DD` requiere rol administrador y acepta rangos inclusivos de hasta 366 días.
- La ocupación por sala se calcula como minutos reservados (sin cancelaciones) entre minutos disponibles según `room_schedules`.
- Las horas pico distribuyen los minutos de cada reserva en bloques horarios; el ranking de estudiantes se ordena por cantidad, minutos reservados y código.
- La inasistencia usa reservas no canceladas cuyo inicio ya superó la tolerancia: `INASISTIO / reservas elegibles`.
- La respuesta del dashboard incluye tendencia diaria de ocupación, celdas día/hora para el mapa de calor, conteos de asistencia/inasistencia/pendientes e inasistencias por estudiante.
- `ADMIN_DASHBOARD` y su valor inicial para administradores forman parte de `database/001_init.sql` y `database/002_seed_release01.sql`.
- `POST /api/rooms/intelligent-search` filtra primero las salas activas y disponibles, interpreta el texto mediante `RoomIntentInterpreter` y aplica el ranking final localmente. `limit` admite valores de 1 a 3 (3 por defecto).
- Groq recibe `query` y el catálogo de salas activas con datos públicos: ID, código, nombre, descripción, ubicación, coordenadas del pabellón, capacidad, ruido, tipo, equipamiento, actividades, servicios cercanos y accesibilidad. Así puede entender exclusiones y referencias espaciales aunque la sala de referencia esté ocupada. No recibe fecha, horario, usuario, correo, cookie ni sesión; el backend filtra localmente las salas disponibles antes de recomendar.
- Los atributos administrables por sala (`room_type`, `noise_level`, `supports_concentration`, `description`, `room_equipment`, `room_allowed_activities`, `room_nearby_services` y `room_accessibility_features`) se reciben en el alta/edición. Enviar una colección vacía elimina sus valores registrados.
- La afinidad semántica de Groq suma como máximo 30 puntos. El backend descarta IDs no disponibles o duplicados, conserva los filtros obligatorios y decide el orden final.
- La búsqueda solo recomienda rangos que coincidan con el bloque configurado del campus y estén alineados desde la apertura; si la duración u horario no son válidos, devuelve una explicación específica.
- Cada coincidencia de Groq incluye `excluded`: una negación explícita como "no quiero en el edificio M", "evita Mayorazgo" o "excepto canchas" elimina esas salas antes de puntuar. Una ausencia de necesidad como "no necesito proyector" no excluye salas que lo tengan.
- Las candidatas incluyen latitud y longitud públicas del pabellón. Si la consulta pide `NEAR` o `FAR` respecto de un edificio identificable, Groq devuelve una sala de referencia y el backend calcula la distancia Haversine localmente; la prioridad geográfica aporta hasta 20 puntos.
- Consultas cubiertas por el contrato semántico: "no me importa el ruido", "no quiero ruido", "con comedor cerca", "sin escaleras", "para cuatro con pizarra", "fuera del edificio M", "cerca de F1" y "lo más lejos posible de Mayorazgo". "Cerca de mí" no se interpreta geográficamente porque no se envían coordenadas del usuario.
- Para una base existente, vuelva a aplicar `database/001_init.sql` y `database/002_seed_release01.sql`; ambos scripts son idempotentes para estos metadatos.

## Seguridad, disponibilidad e inasistencias del Release 2

- `GET` y `HEAD /actuator/health` son públicos para el monitor de uptime. Los endpoints funcionales requieren sesión, salvo los puntos mínimos que inician o confirman login y recuperación de acceso.
- Se retiraron contratos HTTP sin consumidor (`rooms/available`, endpoints de indisponibilidad/mantenimiento manual, `PATCH /rooms/{id}` y confirmación duplicada de reserva); el frontend usa los contratos restantes.
- `GET /api/me/activity` acepta `from`, `to`, `page` y `size`; el historial personal se filtra y pagina en base de datos. El historial administrativo de accesos también admite `sortBy` (`date`, `email`, `status`, `block`) y `sortDir`.
- Spring procesa encabezados reenviados del proxy para conservar la IP real del acceso. El primer acceso crea la referencia; los siguientes alertan por IP o tipo de dispositivo no reconocido.
- El usuario puede revocar una sesión propia, la sesión actual o todas sus sesiones sin aprobación administrativa. La confirmación por correo se conserva para desactivar 2FA.
- Las suscripciones evitan duplicados activos por usuario y sala. Un scheduler comprueba periódicamente si el horario quedó libre; usa `EN_COLA` para evitar correos duplicados y cambia a `NOTIFICADA` solo después del envío exitoso.
- Si el correo de disponibilidad agota sus tres intentos, la suscripción vuelve de `EN_COLA` a `ACTIVA` para que pueda reintentarse en un ciclo posterior.
- Editar una reserva también notifica a los suscriptores del horario anterior cuando este queda libre.
- Los comunicados recorren en lotes de 200 a todos los estudiantes habilitados, sin un tope total de destinatarios.
- `BookingResponse.attendanceStatus` expone `ASISTIO`, `INASISTIO` o `null` para el historial del estudiante.
- El control automático de asistencia corre cada minuto y registra `INASISTIO` cuando han transcurrido 15 minutos desde el inicio. También recupera reservas activas de días anteriores que hubieran quedado pendientes y encola el correo al estudiante.
- `attendance_records` conserva la auditoría (`status`, `recorded_at`, `recorded_by`) y `bookings.attendance_status` mantiene el estado actual consultado por las vistas, sin tablas paralelas para sanciones o fechas de impedimento que este release no implementa.
- La administración lista asistencias con filtros y paginación de backend, incluido campus y pabellón, y puede corregir una reserva a `ASISTIO` o `INASISTIO`.
- El scheduler de mantenimiento sincroniza `PROGRAMADO`, `EN_CURSO` y `FINALIZADO`, bloquea la sala solo durante el intervalo vigente y la devuelve a `DISPONIBLE` al terminar.
- El seed regenera reservas `DEMO_DASHBOARD_*`, `DEMO_FUTURE_*` y `DEMO_MAP_CURRENT_*` con variedad de campus, salas, fechas, horas, asistencia y estados; además oculta del mapa pabellones sin salas activas.

## Inventario físico de salas

- `rooms.inventory_count` conserva una sola sala lógica por tipo, piso y edificio, y registra cuántas unidades físicas existen. El seed configura 4 cubículos, 2 salas de visionado, 4 salas grupales de 6 personas y 2 salas grupales de 8 personas solo donde esos recursos ya existían.
- `bookings.room_unit_number` identifica la unidad física asignada a cada reserva. El backend toma un bloqueo pesimista sobre la sala y elige la primera unidad libre para evitar sobreventa ante solicitudes simultáneas.
- La disponibilidad se agota cuando la cantidad de reservas superpuestas alcanza `inventory_count`; estudiantes y suscripciones trabajan con la sala lógica, mientras la respuesta administrativa expone `roomUnitNumber` y `roomUnitLabel`.
- El mapa y el dashboard consideran el inventario total: una sala lógica solo figura ocupada cuando todas sus unidades están reservadas y los denominadores de ocupación se multiplican por la cantidad de unidades.

# Mapa de disponibilidad E1-H10

El esquema y las coordenadas iniciales del mapa están incluidos en `database/001_init.sql` y `database/002_seed_release01.sql`. `GET /api/campus/map?campus=Monterrico` requiere sesión. `PUT /api/admin/buildings/{id}/location` requiere administrador y persiste una calibración.

Variables nuevas para Render/backend: `REDIS_URL`, `CAMPUS_MAP_CACHE_TTL_SECONDS=10` y `CAMPUS_MAP_REFRESH_SECONDS=15`. Si Redis falla, el servicio registra una advertencia y consulta PostgreSQL.
