# Release 02 – Sprints 5 y 6 (8 semanas)

---

## Sprint 5 (4 semanas) – Seguridad y Gestión de Perfiles

**Objetivo:** Implementar las funcionalidades que permitan a los usuarios actualizar su información personal y recibir alertas automáticas ante cualquier modificación o intento de acceso no autorizado. El sprint se considera alcanzado cuando las historias estén desarrolladas, probadas y validadas por el Product Owner, garantizando la integridad y trazabilidad de las cuentas de usuario.

### Historias incluidas

| Código  | Historia                                           |
|---------|----------------------------------------------------|
| E2-H2   | Actualizar información personal                    |
| E2-H4   | Notificación si la cuenta es modificada            |
| E4-H6   | Notificación de acceso sospechoso / no autorizado  |

### Funcionalidades clave

- Formulario de actualización de perfil (nombre, apellido, correo, contraseña).
- Correo automático de alerta cuando se modifica cualquier dato de la cuenta.
- Notificación por correo al detectar acceso desde dispositivo o ubicación no reconocida.
- Dashboard inicial del administrador con resumen de actividad del sistema.

### Entregable

Gestión avanzada de usuarios con trazabilidad de cambios y seguridad de cuentas.

---

## Sprint 6 (4 semanas) – Notificaciones de Reservas e Información Institucional

**Objetivo:** Completar el sistema de notificaciones con alertas de estado de salas en tiempo real, registro de inasistencias y comunicación de cambios institucionales. Adicionalmente, habilitar el dashboard administrativo con analítica de uso.

### Historias incluidas

| Código  | Historia                                                   |
|---------|------------------------------------------------------------|
| E3-H4   | Notificación cuando una sala se libera                     |
| E3-H6   | Recordatorio de tiempo restante en reserva activa          |
| E3-H9   | Notificación sobre nuevas salas o cambios de política      |
| E3-H10  | Notificación por inasistencia registrada                   |

### Funcionalidades clave

- Notificación automática a estudiantes en lista de espera cuando una sala queda libre.
- Recordatorio enviado 15 minutos antes del fin de la reserva activa (configurable).
- Notificación masiva cuando el administrador publica nuevas políticas o registra una nueva sala.
- Registro automático de inasistencia en el historial del estudiante cuando no se presenta.

### Dashboard Administrativo

| Métrica                            | Descripción |
|------------------------------------|-------------|
| Tasa de ocupación por sala         | % de uso sobre el tiempo disponible por sala |
| Horas pico de uso                  | Franjas horarias con mayor demanda |
| Estudiantes con más reservas       | Ranking de usuarios más activos |
| Tasa de inasistencia               | % de reservas sin asistencia registrada |

### Entregable

Versión final completa del sistema con notificaciones avanzadas y analítica administrativa.
