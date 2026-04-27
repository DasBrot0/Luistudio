# Release 01 – Sprints 1 a 4 (8 semanas)

---

## Sprint 1 (Semanas 1–2) – MVP utilizable de reservas

**Objetivo:** Desarrollar un flujo mínimo funcional que permita a los estudiantes autenticados reservar salas y recibir confirmaciones automáticas, y a los administradores visualizar dichas reservas. El sprint se considera exitoso cuando el 100% del proceso básico de reserva esté operativo y validado mediante pruebas de usuario.

### Historias incluidas

| Código  | Historia                               |
|---------|----------------------------------------|
| E4-H1   | Autenticación de usuario (login + roles) |
| E4-H5   | Restablecer contraseña                 |
| E1-H8   | Reservar una sala de estudio           |
| E1-H9   | Recibir confirmación de reserva por correo |
| E1-H5   | Ver reservas realizadas                |

### Funcionalidades clave

- Inicio de sesión con validación de credenciales y redirección por rol (Administrador / Estudiante).
- Flujo completo de recuperación de contraseña vía correo electrónico.
- Formulario de reserva de sala con confirmación en pantalla (`"Se realizó la reserva con ID: <ID>"`).
- Confirmación automática por correo al reservar.
- Vista de reservas activas para el administrador.
- Límite de reservas simultáneas por estudiante.
- Duración máxima configurable por reserva.

### Entregable

Sistema funcional básico de reservas con autenticación y confirmación por correo.

---

## Sprint 2 (Semanas 3–4) – Catálogo de salas y gestión básica

**Objetivo:** Implementar el módulo completo de gestión de salas y habilitar el control de reservas existentes. El sprint se considera exitoso cuando el 100% del CRUD de salas y las funcionalidades de control estén disponibles y verificadas mediante pruebas funcionales.

### Historias incluidas

| Código  | Historia                          |
|---------|-----------------------------------|
| E1-H1   | Agregar una sala de estudio       |
| E1-H2   | Editar información de sala        |
| E1-H3   | Eliminar una sala de estudio      |
| E1-H4   | Filtrar salas por ubicación       |
| E1-H6   | Modificar una reserva             |
| E1-H7   | Cancelar una reserva              |
| E2-H1   | Ver todos los perfiles registrados|

### Funcionalidades clave

- CRUD completo de salas (crear, editar, eliminar).
- Filtro de salas por ubicación/pabellón (`GET /rooms?ubicacion=X`).
- Modificación y cancelación de reservas por el administrador.
- Vista de perfiles registrados con tabla paginada (10 por página), con columnas: id, código, correo, nombres, apellidos, estado.
- Control de estado del estudiante (Habilitado / Deshabilitado) desde la vista de perfiles.
- Vista de disponibilidad tipo mapa del campus.
- Estado en tiempo real de salas libres / ocupadas.

### Entregable

Módulo completo de gestión de salas con CRUD funcional y control de reservas.

---

## Sprint 3 (Semanas 5–6) – Experiencia completa y gobierno

**Objetivo:** Integrar funcionalidades avanzadas de notificaciones y control de seguridad básico. El sprint se considera exitoso cuando las nuevas funciones estén operativas con un índice de error inferior al 5% en pruebas de QA.

### Historias incluidas

| Código  | Historia                                           |
|---------|----------------------------------------------------|
| E3-H2   | Recordatorio automático previo a la reserva        |
| E3-H3   | Notificación de modificación de reserva            |
| E3-H5   | Notificación de cancelación por administrador      |
| E2-H5   | Búsqueda / filtro de usuarios por código o correo  |
| E4-H1 *(extensión)* | Bloqueo por intentos fallidos de login |

### Funcionalidades clave

- Recordatorio automático enviado por correo antes de la hora de reserva.
- Notificación por correo cuando se modifica una reserva (incluye datos anteriores y nuevos).
- Notificación por correo cuando el administrador cancela una reserva.
- Búsqueda de usuarios por código de estudiante o correo institucional.
- Exportar reserva a Google Calendar; archivo `.ics` adjunto al correo de confirmación.
- Bloqueo automático de cuenta tras múltiples intentos fallidos de inicio de sesión.
- Registro en `audit_log` de modificaciones de reservas.
- Cola `email_outbox` para gestión de correos salientes.

### Entregable

Sistema integrado de notificaciones, calendario y gobierno básico de seguridad.

---

## Sprint 4 (Semanas 7–8) – Optimización y endurecimiento

**Objetivo:** Asegurar que el sistema sea robusto, seguro y configurable antes del cierre del Release 01.

### Historias incluidas

| Código  | Historia                                        |
|---------|-------------------------------------------------|
| E3-H7   | Notificación de mantenimiento de sala           |
| E3-H8   | Configurar preferencias de notificación         |
| E4-H3   | Autenticación de dos factores (2FA)             |
| E4-H8   | Cumplimiento de buenas prácticas de seguridad   |

### Funcionalidades clave

- Gestión de periodos de mantenimiento de salas (bloquea disponibilidad automáticamente).
- Configuración de preferencias de notificación por usuario.
- Autenticación en dos pasos (2FA) opcional por usuario.
- Protección de datos personales y encriptación de información sensible.
- HTTPS obligatorio en todas las comunicaciones.
- Sistema de roles y permisos reforzado.
- **Modo oscuro (Dark Mode)** en la interfaz.

### Entregable

Sistema seguro, configurable y optimizado listo para producción.
