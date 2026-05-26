# Base de Datos - Luistudio

## Motor y acceso

- Motor: `PostgreSQL` (local o cloud: Supabase/Neon).
- Puerto por defecto: `5432`.
- Backend consumidor: `Spring Boot`.
- ORM equivalente en stack Java: `Spring Data JPA` con `Hibernate` como proveedor JPA.

![Diagrama de base de datos](image/database/diagrama_bd.png)

## Entidades principales

### Modulo: Usuarios y Acceso

| Entidad               | Descripcion |
|-----------------------|-------------|
| `usuario`             | Datos del usuario (estudiante o administrador): nombre, apellido, codigo, correo institucional, contrasena (hash bcrypt), estado (habilitado/deshabilitado). |
| `rol`                 | Define el tipo de usuario: Administrador o Estudiante. |
| `permiso`             | Permisos granulares asociados a cada rol. |
| `rol_permiso`         | Tabla puente para relacion muchos-a-muchos entre `rol` y `permiso`. |
| `intento_login`       | Registro de intentos de inicio de sesion fallidos para bloqueo automatico. |
| `doble_factor`        | Configuracion de autenticacion de dos factores (2FA) por usuario. |
| `recuperacion_contra` | Tokens temporales para el flujo de restablecimiento de contrasena. |

### Modulo: Reservas y Salas

| Entidad         | Descripcion |
|-----------------|-------------|
| `pabellon`      | Agrupacion de salas por edificio o ubicacion fisica dentro del campus. |
| `sala`          | Informacion de la sala: nombre, capacidad, ubicacion, estado (disponible/en_mantenimiento). |
| `reserva`       | Registro de reservas: sala, usuario, fecha, hora inicio, hora fin, estado (activa/cancelada/completada). |
| `mantenimiento` | Periodos de mantenimiento programados para una sala, que bloquean su disponibilidad. |

### Modulo: Notificaciones

| Entidad                     | Descripcion |
|----------------------------|-------------|
| `registro_notificacion`    | Historial de todas las notificaciones enviadas (tipo, destinatario, fecha, contenido). |
| `preferencia_notificacion` | Configuracion personal de que notificaciones desea recibir cada usuario. |
| `email_outbox`             | Cola de correos pendientes de envio (confirmaciones, recordatorios, alertas). |

### Modulo: Auditoria

| Entidad     | Descripcion |
|-------------|-------------|
| `audit_log` | Registro de acciones relevantes: actor, accion, entidad afectada y fecha. |

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

- `sala(ubicacion)` para filtros rapidos por campus/pabellon.
- `reserva(usuario_id, fecha)` para validar limite de reservas por usuario.
- `reserva(sala_id, fecha, hora_inicio, hora_fin)` para buscar solapamientos.
- `intento_login(usuario_id, fecha_intento)` para deteccion de accesos fallidos.
- `audit_log(entidad, entidad_id)` para trazabilidad por recurso.

## Script SQL inicial

El esquema inicial propuesto para desarrollo local esta en:

- `database/001_init.sql`
