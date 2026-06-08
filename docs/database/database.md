# Base de Datos - Luistudio

## Motor y acceso

- Motor: `PostgreSQL` (local o cloud: Supabase/Neon).
- Puerto por defecto: `5432`.
- Backend consumidor: `Spring Boot`.
- ORM equivalente en stack Java: `Spring Data JPA` con `Hibernate` c?mo proveedor JPA.

![Diagrama de base de datos](image/database/diagrama_bd.png)

## Entidades principales

### M?dulo: Usuarios y Acceso

| Entidad               | Descripción |
|-----------------------|-------------|
| `usuario`             | Datos del usuario (estudiante o administrador): nombre, apellido, código, correo institucional, contraseña (hash bcrypt), estado (habilitado/deshabilitado). |
| `rol`                 | Define el tipo de usuario: Administrador o Estudiante. |
| `permiso`             | Permisos granulares asociados a cada rol. |
| `rol_permiso`         | Tabla puente para relacion muchos-a-muchos entre `rol` y `permiso`. |
| `intento_login`       | Registro de intentos de inicio de sesión fallidos para bloqueo autom?tico. |
| `doble_factor`        | Configuración de autenticaci?n de dos factores (2FA) por usuario. |
| `recuperacion_contra` | Tokens temporales para el flujo de restablecimiento de contraseña. |

### M?dulo: Reservas y Salas

| Entidad         | Descripción |
|-----------------|-------------|
| `pabellon`      | Agrupación de salas por edificio o ubicación física dentro del campus. |
| `sala`          | Información de la sala: nombre, capacidad, ubicacion, estado (disponible/en_mantenimiento). |
| `reserva`       | Registro de reservas: sala, usuario, fecha, hora inicio, hora fin, estado (activa/cancelada/completada). |
| `mantenimiento` | Periodos de mantenimiento programados para una sala, que bloquean su disponibilidad. |

### Módulo: Notificaciones

| Entidad                     | Descripción |
|----------------------------|-------------|
| `registro_notificacion`    | Historial de todas las notificaciones enviadas (tipo, destinatario, fecha, contenido). |
| `preferencia_notificacion` | Configuración personal de que notificaciones desea recibir cada usuario. |
| `email_outbox`             | Cola de correos pendientes de envío (confirmaciones, recordatorios, alertas). |

### M?dulo: Auditoria

| Entidad     | Descripción |
|-------------|-------------|
| `audit_log` | Registro de acciones relevantes: actor, acción, entidad afectada y fecha. |

## Relaciones clave

```text
usuario 1 --- n reserva n --- 1 sala
usuario 1 --- n intento_login
usuario 1 --- 1 doble_factor
usuario 1 --- n recuperacion_contra
usuario 1 --- 1 preferencia_notificacion
usuario 1 --- n registro_notificacion
sala n --- 1 pabellon
sala 1 --- n mantenimiento
rol 1 --- n usuario
rol n --- n permiso (via rol_permiso)
```

## Indices recomendados

- `sala(ubicacion)` para filtros rápidos por campus/pabellon.
- `reserva(usuario_id, fecha)` para validar límite de reservas por usuario.
- `reserva(sala_id, fecha, hora_inicio, hora_fin)` para buscar solapamientos.
- `intento_login(usuario_id, fecha_intento)` para detección de accesos fallidos.
- `audit_log(entidad, entidad_id)` para trazabilidad por recurso.

## Script SQL inicial

El esquema inicial propuesto para desarrollo local está en:

- `database/001_init.sql`
