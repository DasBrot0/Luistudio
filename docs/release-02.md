# Release 02 — Sprints 5, 6 y 7

Este documento describe el alcance vigente del Release 2. Ante diferencias con planes anteriores, prevalece este comportamiento.

## Historias del Release 2

| Issue | ID | Historia | Criterios de aceptación vigentes | Sprint |
|---|---|---|---|---|
| #20 | E2-H2 | Consultar información de cuenta | “Mi perfil” muestra código, nombres, apellidos, correo, rol y estado en modo de solo lectura. | 5 |
| #21 | E2-H7 | Historial de actividad | Muestra accesos, cierres de sesión y cambios sensibles con fecha; IP y dispositivo se presentan de forma legible cuando aplican. | 5 |
| #22 | E4-H6 | Alerta de acceso no habitual | Registra IP y user-agent. Después del primer acceso, una IP o tipo de dispositivo no reconocido encola una alerta por correo. No interviene en el bloqueo por intentos fallidos. | 5 |
| #23 | E4-H7 | Cierre de sesiones activas | Desde Configuración > Sesiones activas, el usuario puede cerrar la sesión actual, una sesión remota o todas sus sesiones, sin autorización administrativa. | 5 |
| #24 | E4-H9 | Confirmación de cambios sensibles | La desactivación de 2FA requiere confirmación por correo. El cambio de contraseña no forma parte de este release. La revocación de sesiones es una acción personal y no requiere correo ni aprobación administrativa. | 5 |
| #25 | E4-H10 | Auditoría de intentos de acceso | El administrador puede filtrar por usuario, correo, resultado, bloqueo vigente y rango de fechas. Se muestran IP, dispositivo, fecha y bloqueo temporal. | 5 |
| #26 | E1-H10 | Mapa de disponibilidad | El mapa agrupa salas por pabellón o zona y muestra su estado actual: libre, ocupada o en mantenimiento. La actualización se realiza al abrir la vista o mediante la acción manual de actualizar. | 6 |
| #27 | E1-H12 | Búsqueda inteligente de salas | Interpreta texto libre, genera requisitos estructurados y devuelve un ranking determinista de hasta 3 salas compatibles. Puede devolver menos cuando no existan suficientes opciones. | 7 |
| #28 | E3-H13 | Suscripción a disponibilidad | El estudiante puede suscribirse o cancelar el interés por una sala ocupada. Solo puede existir una suscripción activa por usuario y sala. Al cancelar por sala se cancelan sus suscripciones activas asociadas a ella. | 6 |
| #29 | E3-H4 | Notificación cuando una sala se libera | Se comprueba periódicamente la disponibilidad y también se encola el aviso al cancelar o modificar una reserva. La suscripción usa `EN_COLA` y se marca como notificada únicamente después del envío exitoso. | 6 |
| #30 | E3-H9 | Comunicados institucionales | El administrador publica comunicaciones persistidas y el sistema recorre por lotes a todos los estudiantes habilitados para encolar los correos, sin un límite total de 1000 destinatarios. | 6 |
| #31 | E3-H10 | Registro de inasistencias | A los 15 minutos sin asistencia se registra la inasistencia y se notifica al estudiante. “Mis reservas” incluye el historial e indica que este release no registra una fecha de impedimento ni crea sanciones automáticas. | 6 |
| #32 | E5-H1 | Dashboard administrativo | Muestra KPI de ocupación, reservas, hora pico e inasistencia; tendencia diaria, asistencia, mapa de calor semanal, ocupación por sala y ranking de estudiantes. Incluye rango de fechas, restablecimiento y exportación CSV. | 7 |

## Sprint 5 — Cuenta y seguridad

El usuario consulta sus datos institucionales, revisa su actividad y administra sus propias sesiones desde la configuración. El primer inicio de sesión establece la referencia de acceso; a partir de allí se alertan IP o tipos de dispositivo no reconocidos. La confirmación por correo se conserva únicamente para desactivar 2FA.

La auditoría administrativa permite combinar filtros de usuario, correo, resultado del intento, bloqueo y fechas.

## Sprint 6 — Disponibilidad y notificaciones

El mapa presenta la disponibilidad actual y ofrece actualización manual. Las suscripciones evitan más de un registro activo para la misma pareja usuario-sala. Un proceso periódico detecta horarios liberados; las cancelaciones y ediciones también disparan la comprobación. El aviso pasa por `EN_COLA` y cambia a `NOTIFICADA` después de que el correo haya sido enviado correctamente.

Los comunicados se procesan en páginas de 200 usuarios hasta cubrir a todos los estudiantes habilitados. Las inasistencias se registran automáticamente y se muestran en el historial del estudiante; no se presume una sanción que no esté registrada por una regla de negocio.

## Sprint 7 — Inteligencia y analítica

La búsqueda inteligente combina interpretación de intención con filtros y puntuación deterministas. Devuelve como máximo tres recomendaciones compatibles, ordenadas por puntaje y código de sala como desempate.

El dashboard administrativo presenta las métricas del rango seleccionado mediante tarjetas KPI, tendencia diaria, gráfico de asistencia, mapa de calor por día/hora, barras horizontales por sala y una tabla buscable y paginada de estudiantes. La vista mantiene tema claro/oscuro, valores textuales además del color y exportación CSV del ranking.

## Comandos de verificación

Backend:

```bash
cd backend/reservas
./mvnw test
```

Frontend:

```bash
cd frontend/luistudio-app
pnpm test
```
