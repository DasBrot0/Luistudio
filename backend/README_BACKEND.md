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
4. Ejecuta:

```bash
./mvnw spring-boot:run
```

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
- `GET /api/rooms/available?fecha&horaInicio&horaFin`
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
- `GET /api/campus/map`

### Preferencias

- `GET /api/me/preferences`
- `PUT /api/me/preferences`

## Notas de implementacion

- Se implementa bloqueo temporal tras intentos fallidos de login.
- Se implementa 2FA por codigo temporal.
- `email_outbox` se procesa por scheduler (reintentos automaticos).
- Se generan enlaces Google Calendar y descarga `.ics` por reserva.
