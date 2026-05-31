# Backend - Luistudio (Release 01)

## Stack

- Java 21
- Spring Boot 3.x
- Spring Security + JWT custom
- Spring Data JPA + Hibernate
- PostgreSQL (o H2 en memoria para desarrollo rapido)

## Ejecucion local

1. Asegura `JAVA_HOME` apuntando a Java 21.
2. (Opcional recomendado) crear BD PostgreSQL `luistudio_db` y ejecutar `database/001_init.sql`.
3. Variables de entorno opcionales:
   - `DB_URL`
   - `DB_USER`
   - `DB_PASSWORD`
   - `DB_DRIVER` (por defecto H2)
   - `JWT_SECRET`
   - `CORS_ORIGINS`
   - `EMAIL_PROVIDER` (`log` por defecto, `resend` para envio real por API HTTP)
   - `EMAIL_FROM` (ej. `Luistudio <no-reply@tu-dominio.com>`)
   - `RESEND_API_KEY` (requerida si `EMAIL_PROVIDER=resend`)
4. Ejecuta:

```bash
./mvnw spring-boot:run
```

Si usas el script `scripts/start-backend.ps1`, este carga variables automaticamente desde:
- `/.env` (raiz del proyecto), o
- `/backend/reservas/.env` (si no existe el de raiz).

## Despliegue en Render (Docker)

En este proyecto puedes desplegar el backend usando:
- `backend/reservas/Dockerfile`
- `backend/reservas/.dockerignore`

Sugerencias de configuracion en Render:
- `Root Directory`: `backend/reservas`
- Runtime: `Docker`
- Variables de entorno minimas:
  - `DB_URL`
  - `DB_USER`
  - `DB_PASSWORD`
  - `DB_DRIVER=org.postgresql.Driver`
  - `JWT_SECRET`
  - `CORS_ORIGINS`

Memoria (500 MB):
- El Dockerfile ya incluye ajustes JVM para memoria limitada (`MaxRAMPercentage=70`, `SerialGC`).
- Con trafico bajo/moderado suele funcionar bien en 500 MB.
- Si sube la concurrencia, exportaciones pesadas o procesos simultaneos, puede haber reinicios por OOM; en ese caso conviene subir a 1 GB.

## Endpoints principales (Release 01)

### Auth y seguridad

- `POST /api/auth/login`
- `GET /api/auth/me`
- `POST /api/auth/reset-request`
- `POST /api/auth/reset-confirm`
- `POST /api/auth/2fa/enroll`
- `POST /api/auth/2fa/confirm`
- `POST /api/auth/2fa/verify`
- `POST /api/auth/2fa/disable`

### Salas y disponibilidad

- `GET /api/rooms`
- `GET /api/rooms?campus&ubicacion`
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
- `GET /api/bookings/me`
- `GET /api/bookings/{id}/ics`
- `GET /api/admin/bookings`

### Administracion

- `GET /api/admin/users`
- `PATCH /api/admin/users/{id}/estado`
- `GET /api/admin/config`
- `PUT /api/admin/config`
- `GET /api/admin/campus-schedules`
- `PUT /api/admin/campus-schedules`
- `GET /api/campus/map`

### Preferencias

- `GET /api/me/preferences`
- `PUT /api/me/preferences`
  - Incluye preferencias de notificaciones + UI (`themeMode`, `fontScale`).

### Usuarios (consulta para reservas)

- `GET /api/users/lookup?code=...`
  - Requiere usuario autenticado.
  - Devuelve codigo y nombre completo para validar participantes de una reserva.

## Notas de implementacion

- Se implementa bloqueo temporal tras intentos fallidos de login.
- Se implementa 2FA por código temporal.
- `email_outbox` se procesa por scheduler (reintentos automaticos).
- En local, sin `RESEND_API_KEY`, el envio de correos cae en modo log (no SMTP).
- Para despliegue en Render, usar `EMAIL_PROVIDER=resend` + `RESEND_API_KEY` (salida por HTTPS/443).
- Se generan enlaces Google Calendar y descarga `.ics` por reserva.
- El esquema SQL base (`database/001_init.sql` y `database/002_seed_release01.sql`) define tablas y columnas para instalaciones desde cero.
- Para bases existentes, aplicar los scripts incrementales disponibles en `/database` antes de levantar el backend.
- Las salas almacenan data de catalogo en espanol (`code`, `name`, `campus`, `venue`, `location`).
- Se agregan horarios por campus (`campus_schedules`) y override por sala (`room_schedules`) para validar reservas por dia/hora.
- Se agregan reglas por sala de personas: `minPeople`, `minPeopleRequired`, `maxPeople`.
- Se agrega configuracion de duracion por bloque de reserva por campus (Monterrico 60 min, Mayorazgo 45 min por defecto).
- Las reservas solo aceptan fechas/horas dentro de la ventana permitida (semana actual; fin de semana habilita tambien la siguiente semana) y siempre en horas futuras del dia actual.
- No se permite cancelar reservas que ya finalizaron (validacion en backend).
- Se agrega preferencia por usuario `login_landing_view` para definir la vista inicial al iniciar sesion (validada por rol).
- Para bases ya existentes, aplicar `database/004_add_login_landing_view.sql`.
- Para convertir data previa de salas (ingles -> espanol en `code/name/campus/venue/location`), aplicar `database/005_rooms_data_to_spanish.sql`.

## Keep-alive en Render con UptimeRobot

Si despliegas en Render y quieres evitar que el backend entre en reposo por inactividad, puedes usar UptimeRobot para hacer ping cada 5 minutos.

1. Verifica endpoint de salud publico:
   - URL recomendada: `GET /actuator/health`
   - En este proyecto:
     - Actuator esta habilitado.
     - Solo se expone `health`.
     - `GET /actuator/health` esta permitido sin autenticacion.
2. Crea cuenta gratuita en UptimeRobot (hasta 50 monitores, intervalo minimo 5 min).
3. Crea monitor:
   - `Monitor Type`: `HTTP(s)`
   - `Friendly Name`: `Luistudio Backend`
   - `URL`: `https://<tu-servicio-render>.onrender.com/actuator/health`
   - `Monitoring Interval`: `5 minutes`
4. Guarda el monitor y valida que reciba `200 OK`.

Nota:
- No uses `GET /api/rooms` para keep-alive si no mandas token, porque ese endpoint requiere autenticacion.

## Patrones aplicados

- `Strategy`:
  - Reglas de validacion de reservas (`service/booking/rule/*`).
  - Flujo de respuesta de login (`service/auth/strategy/*`).
- `Command`:
  - Recordatorios de reservas (`service/booking/command/*`).
- `Factory Method`:
  - Creacion de entidades de reservas/salas/mantenimiento/seguridad (`service/factory/*`).
  - Seleccion de proveedor de envio de correo (`service/email/gateway/EmailGatewayFactory`).
- `Adapter`:
  - Adaptacion de proveedor de correo (`service/email/gateway/*Gateway`).
