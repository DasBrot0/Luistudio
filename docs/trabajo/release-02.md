# Release 02 - Sprints 5 y 6 (8 semanas)

---

## Sprint 5 (4 semanas) - Seguridad y Gestion de Perfiles

**Objetivo:** Implementar las funcionalidades que permitan a los usuarios actualizar su informacion personal y recibir alertas automaticas ante cualquier modificacion o intento de acceso no autorizado. El sprint se considera alcanzado cuando las historias esten desarrolladas, probadas y validadas por el Product Owner, garantizando la integridad y trazabilidad de las cuentas de usuario.

### Historias incluidas

| Codigo  | Historia |
|---------|----------|
| E2-H2   | Actualizar informacion personal |
| E2-H4   | Notificacion si la cuenta es modificada |
| E4-H6   | Notificacion de acceso sospechoso / no autorizado |
| E2-H7   | Historial de actividad de cuenta y sesiones activas |
| E4-H7   | Cierre de sesiones activas desde el perfil |
| E4-H9   | Confirmacion por correo para cambios sensibles |
| E4-H10  | Historial de intentos fallidos y bloqueos de cuenta |

### Funcionalidades clave

- Formulario de actualizacion de perfil (nombre, apellido, correo, contrasena).
- Correo automatico de alerta cuando se modifica cualquier dato de la cuenta.
- Notificacion por correo al detectar acceso desde dispositivo o ubicacion no reconocida.
- Vista de historial de actividad con ultimos accesos, IP origen y dispositivo.
- Opcion de cerrar sesiones activas y revocar tokens previos.
- Confirmacion por correo para cambios sensibles (correo y contrasena).
- Historial de intentos fallidos y bloqueos temporales de cuenta para deteccion de abuso.
- Dashboard inicial del administrador con resumen de actividad del sistema.

### Entregable

Gestion avanzada de usuarios con trazabilidad de cambios y seguridad de cuentas.

---

## Sprint 6 (4 semanas) - Notificaciones de Reservas e Informacion Institucional

**Objetivo:** Completar el sistema de notificaciones con alertas de estado de salas en tiempo real, registro de inasistencias y comunicacion de cambios institucionales. Adicionalmente, habilitar el dashboard administrativo con analitica de uso.

### Historias incluidas

| Codigo  | Historia |
|---------|----------|
| E3-H4   | Notificacion cuando una sala se libera |
| E3-H6   | Recordatorio de tiempo restante en reserva activa |
| E3-H9   | Notificacion sobre nuevas salas o cambios de politica |
| E3-H10  | Notificacion por inasistencia registrada |
| E3-H11  | Integracion con calendario externo (.ics / Google Calendar) |
| E3-H12  | Resumen semanal de ocupacion para administradores |

### Funcionalidades clave

- Notificacion automatica a estudiantes en lista de espera cuando una sala queda libre.
- Recordatorio enviado 15 minutos antes del fin de la reserva activa (configurable).
- Notificacion masiva cuando el administrador publica nuevas politicas o registra una nueva sala.
- Registro automatico de inasistencia en el historial del estudiante cuando no se presenta.
- Exportacion de reservas a calendario externo y descarga `.ics` desde la reserva.
- Resumen semanal por correo con ocupacion, horas pico y tasa de inasistencia.

### Dashboard Administrativo

| Metrica | Descripcion |
|---------|-------------|
| Tasa de ocupacion por sala | % de uso sobre el tiempo disponible por sala |
| Horas pico de uso | Franjas horarias con mayor demanda |
| Estudiantes con mas reservas | Ranking de usuarios mas activos |
| Tasa de inasistencia | % de reservas sin asistencia registrada |

### Entregable

Version final completa del sistema con notificaciones avanzadas y analitica administrativa.

---

## Balance de historias (R1 vs R2)

- Release 01: 21 historias.
- Release 02: 13 historias (incluye 6 nuevas en este ajuste).
- Total: 34 historias.
- Distribucion aproximada: Release 01 = 61.8% / Release 02 = 38.2% (aprox. objetivo 60/40).
