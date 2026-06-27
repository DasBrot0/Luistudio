# Release 02 – Sprints 5 y 6

Documento ajustado para incluir únicamente historias de usuario pendientes o incompletas al cierre del Release 1.

---

## Tabla de historias del Release 2

| ID     | Épica                 | Historia de usuario                                                                                                                                                                 | Criterios de aceptación (Definición de Listo / QA)                                                                                                                                                                                                 | Sprint   |
| ------ | ---------------------- | ----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- | -------- |
| E2-H2  | Perfiles               | Como usuario, quiero consultar mi información de cuenta, para verificar mis datos registrados sin modificarlos directamente.                                                       | La pantalla "Mi perfil" muestra código, nombres, apellidos, correo, rol y estado en modo solo lectura. No debe permitir editar nombres, apellidos ni correo desde esta vista.                                                                       | Sprint 5 |
| E2-H7  | Perfiles               | Como usuario, quiero ver el historial de actividad de mi cuenta, para revisar eventos relevantes de seguridad.                                                                      | La vista debe mostrar inicios de sesión, cambios sensibles y cierres de sesión, con fecha, IP y dispositivo cuando aplique.                                                                                                                        | Sprint 5 |
| E4-H6  | Seguridad              | Como usuario, quiero recibir una alerta cuando se detecte un acceso desde un dispositivo o ubicación no habitual, para identificar posibles accesos sospechosos.                   | El sistema registra IP, dispositivo o user-agent del acceso. Si detecta un acceso no habitual, encola una alerta por correo. No debe duplicar el bloqueo por intentos fallidos del Release 1.                                                        | Sprint 5 |
| E4-H7  | Seguridad              | Como usuario, quiero cerrar sesiones activas desde mi perfil, para proteger mi cuenta si inicié sesión en otro dispositivo.                                                       | La vista muestra sesiones activas con fecha, IP y dispositivo. El usuario puede cerrar una sesión específica, cerrar todas las sesiones o cerrar la sesión actual desde la zona de configuración.                                                | Sprint 5 |
| E4-H9  | Seguridad              | Como usuario, quiero confirmar por correo los cambios sensibles, para evitar modificaciones no autorizadas en mi cuenta.                                                            | Cambios sensibles como contraseña, desactivación de 2FA o cierre global de sesiones requieren token temporal enviado por correo antes de aplicarse definitivamente.                                                                                | Sprint 5 |
| E4-H10 | Seguridad              | Como administrador, quiero consultar el historial de intentos fallidos y bloqueos de cuenta, para auditar eventos de seguridad.                                                     | Vista administrativa con filtros por usuario, correo, fecha y estado. Debe mostrar intentos fallidos, IP, fecha y bloqueos temporales.                                                                                                               | Sprint 5 |
| E1-H10 | Reservas               | Como usuario, quiero ver un mapa simplificado del campus con el estado actual de las salas, para identificar rápidamente qué espacios están libres, ocupados o en mantenimiento. | La vista muestra un plano simplificado del campus agrupado por pabellón o zona. Cada sala debe mostrar su estado actual: libre, ocupada o en mantenimiento. La información debe actualizarse automáticamente sin recargar manualmente la página. | Sprint 6 |
| E1-H12 | Reservas               | Como estudiante, quiero buscar salas escribiendo lo que necesito en lenguaje natural, para encontrar opciones adecuadas sin usar filtros complejos.                                 | El usuario puede escribir una intención como "quiero un lugar tranquilo para estudiar con mi amigo". La IA convierte la intención en requisitos estructurados y el backend devuelve un ranking determinista de 3 a 5 salas compatibles.            | Sprint 6 |
| E3-H13 | Notificaciones         | Como estudiante, quiero suscribirme a la disponibilidad de una sala ocupada, para recibir aviso cuando vuelva a estar disponible.                                                   | En la vista de salas o reserva, el estudiante puede registrar o cancelar su interés por una sala ocupada. El sistema guarda la suscripción evitando duplicados activos por usuario y sala.                                                         | Sprint 6 |
| E3-H4  | Notificaciones         | Como estudiante, quiero recibir una notificación cuando una sala solicitada se libere, para poder reservarla oportunamente.                                                        | Al cancelarse una reserva o liberarse una sala, el sistema notifica a los estudiantes con suscripción activa a esa sala y marca la notificación como enviada.                                                                                      | Sprint 6 |
| E3-H9  | Notificaciones         | Como administrador, quiero comunicar nuevas salas o cambios de política, para informar a los estudiantes sobre actualizaciones institucionales.                                    | El administrador puede publicar una comunicación institucional. El sistema encola una notificación masiva a los estudiantes.                                                                                                                       | Sprint 6 |
| E3-H10 | Notificaciones         | Como estudiante, quiero recibir una notificación si se registra una inasistencia, para conocer el estado de mi historial de reservas.                                              | Si pasan 15 minutos desde la hora de inicio de la reserva y no existe registro de asistencia, el sistema marca la inasistencia, la registra en el historial y envía una notificación.                                                              | Sprint 6 |
| E5-H1  | Experiencia de Usuario | Como administrador, quiero consultar un dashboard con métricas de uso, para analizar la ocupación y comportamiento de reservas.                                                   | Dashboard con tasa de ocupación por sala, horas pico, ranking de estudiantes con más reservas y tasa de inasistencia. Debe permitir filtrar por rango de fechas.                                                                                   | Sprint 6 |

---

## Sprint 5 – Seguridad avanzada y gestión de cuenta

**Objetivo:** completar las funcionalidades pendientes de perfil y seguridad avanzada, permitiendo al usuario consultar sus datos registrados, revisar su actividad, controlar sesiones activas y confirmar cambios sensibles por correo.

### Historias incluidas

| ID     | Historia                                                     |
| ------ | ------------------------------------------------------------ |
| E2-H2  | Consultar información de cuenta                             |
| E2-H7  | Historial de actividad de cuenta                             |
| E4-H6  | Alerta por acceso desde dispositivo o ubicación no habitual |
| E4-H7  | Cierre de sesiones activas desde el perfil                   |
| E4-H9  | Confirmación por correo para cambios sensibles              |
| E4-H10 | Historial de intentos fallidos y bloqueos de cuenta          |

### Funcionalidades clave

* Pantalla "Mi perfil" en modo solo lectura.
* Historial de actividad de cuenta visible para el usuario.
* Registro de inicios y cierres de sesión.
* Registro de cambios sensibles.
* Registro de accesos con IP, fecha y dispositivo.
* Detección de acceso desde dispositivo o ubicación no habitual.
* Vista de sesiones activas.
* Cierre de sesión actual desde configuración.
* Cierre de sesiones remotas desde el perfil.
* Confirmación por correo para cambios sensibles.
* Vista administrativa de intentos fallidos y bloqueos.

### Entregable

Gestión avanzada de cuenta con trazabilidad, control de sesiones, alertas de seguridad y confirmación de cambios sensibles.

---

## Sprint 6 – Disponibilidad, notificaciones avanzadas, analítica y búsqueda inteligente

**Objetivo:** implementar las funcionalidades pendientes de disponibilidad visual del campus, notificaciones avanzadas, analítica administrativa y búsqueda inteligente de salas mediante interpretación de lenguaje natural con IA y ranking clásico determinista.

### Historias incluidas

| ID     | Historia                                                 |
| ------ | -------------------------------------------------------- |
| E1-H10 | Vista de disponibilidad tipo mapa de campus              |
| E1-H12 | Búsqueda inteligente de salas con IA y ranking clásico |
| E3-H13 | Suscripción a disponibilidad de sala                    |
| E3-H4  | Notificación cuando una sala se libera                  |
| E3-H9  | Notificación sobre nuevas salas o cambios de política  |
| E3-H10 | Notificación por inasistencia registrada                |
| E5-H1  | Dashboard administrativo con métricas de uso            |

### Funcionalidades clave

* Vista tipo mapa/plano simplificado del campus.
* Estado actual de salas: libre, ocupada o en mantenimiento.
* Agrupación visual por pabellón o zona.
* Actualización automática del estado de salas sin recargar manualmente.
* Acceso desde la vista de reservas o salas.
* Suscripción o lista de interés para salas ocupadas.
* Notificación automática cuando una sala queda libre.
* Comunicación institucional masiva sobre nuevas salas o cambios de política.
* Registro automático de inasistencias con tolerancia de 15 minutos.
* Notificación al estudiante por inasistencia registrada.
* Dashboard administrativo con ocupación por sala.
* Métricas de horas pico de uso.
* Ranking de estudiantes con más reservas.
* Tasa de inasistencia.
* Búsqueda inteligente de salas usando IA para interpretar intención.
* Motor clásico de filtros y scoring en backend.
* Ranking de salas compatibles basado en reglas deterministas.

### Entregable

Sistema con mapa de disponibilidad, notificaciones avanzadas, control de inasistencias, analítica administrativa y búsqueda inteligente de salas.
