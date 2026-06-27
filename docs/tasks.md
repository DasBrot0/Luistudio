# Tareas por Épica – Luistudio

---

# Release 1

## Épica 1 – Reservas

---

### E1-H8 · Reservar una sala de estudio

**Sprint:** 1 · **Prioridad:** Alta · **Tamaño:** L

| Área         | Tarea                                                                                                                                                                            |
| ------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Front-End     | Formulario transaccional para reservar sala con fecha, hora de inicio y hora de fin. Mostrar mensaje: "Reserva registrada con éxito. Tu reserva fue registrada correctamente.". |
| Back-End      | Implementar`POST /bookings`, validando disponibilidad de sala, horario permitido, usuario autenticado y ausencia de solapamientos.                                             |
| Base de datos | Reutilizar`bookings`, `rooms` y `users`. Mantener índices para búsqueda por sala, fecha y horario.                                                                       |
| Pruebas       | Crear reserva válida, rechazar reservas solapadas y validar que se muestre la confirmación correcta.                                                                           |

---

### E1-H9 · Confirmación de reserva por correo

**Sprint:** 1 · **Prioridad:** Alta · **Tamaño:** S

| Área         | Tarea                                                                 |
| ------------- | --------------------------------------------------------------------- |
| Front-End     | Mostrar confirmación visual al estudiante después de reservar.      |
| Back-End      | Enviar o encolar correo automático al concretar la reserva.          |
| Base de datos | Reutilizar`email_outbox`.                                           |
| Pruebas       | Verificar que al crear una reserva se genere correo de confirmación. |

---

### E1-H5 · Ver reservas realizadas

**Sprint:** 1 · **Prioridad:** Alta · **Tamaño:** S

| Área         | Tarea                                                                                                            |
| ------------- | ---------------------------------------------------------------------------------------------------------------- |
| Front-End     | Vista administrativa de reservas activas.                                                                        |
| Back-End      | Implementar`GET /admin/bookings` para listar reservas realizadas.                                              |
| Base de datos | Reutilizar`bookings`.                                                                                          |
| Pruebas       | Verificar que el administrador pueda ver reservas y que el estudiante no tenga acceso a la vista administrativa. |

---

### E1-H11 · Configurar límites de reserva

**Sprint:** 1 · **Prioridad:** Alta · **Tamaño:** M

| Área         | Tarea                                                                                                         |
| ------------- | ------------------------------------------------------------------------------------------------------------- |
| Front-End     | Panel administrativo para configurar cantidad máxima de reservas simultáneas y duración máxima permitida. |
| Back-End      | Implementar consulta y actualización de configuración. Validar límites al crear una reserva.               |
| Base de datos | Reutilizar o crear configuración del sistema para límites de reserva.                                       |
| Pruebas       | Rechazar reservas que superen la cantidad máxima o duración máxima configurada.                            |

---

### E1-H1 · Registrar salas de estudio

**Sprint:** 2 · **Prioridad:** Media · **Tamaño:** S

| Área         | Tarea                                                                                  |
| ------------- | -------------------------------------------------------------------------------------- |
| Front-End     | Formulario o modal para registrar una nueva sala de estudio.                           |
| Back-End      | Implementar`POST /rooms` con validación de datos obligatorios.                      |
| Base de datos | Reutilizar`rooms`.                                                                   |
| Pruebas       | Crear sala válida, validar campos obligatorios y restringir acceso a administradores. |

---

### E1-H4 · Filtrar salas por ubicación

**Sprint:** 2 · **Prioridad:** Baja · **Tamaño:** M

| Área         | Tarea                                                          |
| ------------- | -------------------------------------------------------------- |
| Front-End     | Filtro por ubicación en la vista de salas.                    |
| Back-End      | Implementar`GET /rooms?ubicacion=X`.                         |
| Base de datos | Reutilizar`rooms` e índice por ubicación si corresponde.   |
| Pruebas       | Filtrar salas por ubicación y verificar resultados correctos. |

---

### E1-H6 · Modificar una reserva

**Sprint:** 2 · **Prioridad:** Media · **Tamaño:** M

| Área         | Tarea                                                                                             |
| ------------- | ------------------------------------------------------------------------------------------------- |
| Front-End     | Acción "Editar" en la tabla de reservas con formulario precargado.                               |
| Back-End      | Implementar`PUT /bookings/{id}` para modificar sala, fecha u horario, validando disponibilidad. |
| Base de datos | Reutilizar`bookings`. Registrar `updated_at` y `updated_by` si aplica.                      |
| Pruebas       | Modificar una reserva, rechazar solapamientos y confirmar actualización correcta.                |

---

## Épica 2 – Perfiles

---

### E2-H1 · Ver perfiles registrados

**Sprint:** 2 · **Prioridad:** Media · **Tamaño:** M

| Área         | Tarea                                                                                                                  |
| ------------- | ---------------------------------------------------------------------------------------------------------------------- |
| Front-End     | Pantalla de usuarios con tabla paginada de 10 por página. Columnas: id, código, correo, nombres, apellidos y estado. |
| Back-End      | Implementar`GET /admin/users`.                                                                                       |
| Base de datos | Reutilizar`users`.                                                                                                   |
| Pruebas       | Listar usuarios y validar paginación.                                                                                 |

---

### E2-H5 · Buscar usuarios por código o correo

**Sprint:** 3 · **Prioridad:** Baja · **Tamaño:** S

| Área         | Tarea                                                                                                                      |
| ------------- | -------------------------------------------------------------------------------------------------------------------------- |
| Front-End     | Barra de búsqueda en la pantalla de usuarios.                                                                             |
| Back-End      | Implementar búsqueda por código o correo institucional usando coincidencia parcial insensible a mayúsculas/minúsculas. |
| Base de datos | Reutilizar índices por código y correo si existen.                                                                       |
| Pruebas       | Buscar por código, correo completo y coincidencia parcial.                                                                |

---

## Épica 3 – Notificaciones

---

### E3-H2 · Recordatorio automático antes de la reserva

**Sprint:** 3 · **Prioridad:** Media · **Tamaño:** S

| Área         | Tarea                                                                                        |
| ------------- | -------------------------------------------------------------------------------------------- |
| Front-End     | Opcional: mostrar etiqueta de próxima reserva en "Mis reservas".                            |
| Back-End      | Job que envía correo antes de la hora de reserva.                                           |
| Base de datos | Reutilizar`bookings` y `email_outbox`.                                                   |
| Pruebas       | Verificar envío antes del inicio de la reserva y evitar envío si la reserva fue cancelada. |

---

### E3-H3 · Notificación por modificación de reserva

**Sprint:** 3 · **Prioridad:** Media · **Tamaño:** M

| Área         | Tarea                                                                        |
| ------------- | ---------------------------------------------------------------------------- |
| Front-End     | Mostrar mensaje de confirmación al guardar cambios.                         |
| Back-End      | Enviar correo con datos anteriores y nuevos cuando se modifique una reserva. |
| Base de datos | Reutilizar`email_outbox` y `audit_log` si aplica.                        |
| Pruebas       | Editar reserva y verificar correo con comparación antes/después.           |

---

### E3-H5 · Notificación por cancelación de reserva

**Sprint:** 3 · **Prioridad:** Media · **Tamaño:** M

| Área         | Tarea                                                                  |
| ------------- | ---------------------------------------------------------------------- |
| Front-End     | Mostrar estado "Cancelada" en las vistas correspondientes.             |
| Back-End      | Enviar correo automático cuando el administrador cancela una reserva. |
| Base de datos | Reutilizar`email_outbox` y `bookings`.                             |
| Pruebas       | Cancelar reserva y verificar notificación al estudiante.              |

---

### E3-H7 · Registrar mantenimiento de salas

**Sprint:** 4 · **Prioridad:** Baja · **Tamaño:** S

| Área         | Tarea                                                                                |
| ------------- | ------------------------------------------------------------------------------------ |
| Front-End     | Formulario para registrar periodos de mantenimiento o no disponibilidad de una sala. |
| Back-End      | Implementar endpoints para crear y consultar periodos de no disponibilidad.          |
| Base de datos | Reutilizar o crear`rooms_unavailability`.                                          |
| Pruebas       | Bloquear reservas durante periodos de mantenimiento.                                 |

---

### E3-H8 · Configurar preferencias de notificación

**Sprint:** 4 · **Prioridad:** Baja · **Tamaño:** M

| Área         | Tarea                                                                                                  |
| ------------- | ------------------------------------------------------------------------------------------------------ |
| Front-End     | Vista de preferencias con interruptores por tipo de notificación.                                     |
| Back-End      | Implementar consulta y guardado de preferencias del usuario. Respetar preferencias al encolar correos. |
| Base de datos | Reutilizar preferencias de usuario.                                                                    |
| Pruebas       | Desactivar correo y validar que no se encolen notificaciones deshabilitadas.                           |

---

## Épica 4 – Seguridad

---

### E4-H1 · Inicio de sesión con roles

**Sprint:** 1 · **Prioridad:** Alta · **Tamaño:** M

| Área         | Tarea                                                                       |
| ------------- | --------------------------------------------------------------------------- |
| Front-End     | Pantalla de login y redirección según rol.                                |
| Back-End      | Implementar login con validación bcrypt y sesión/token de autenticación. |
| Base de datos | Reutilizar`users`, `roles` y campos de seguridad necesarios.            |
| Pruebas       | Login correcto, credenciales incorrectas y redirección por rol.            |

---

### E4-H5 · Restablecer contraseña

**Sprint:** 1 · **Prioridad:** Alta · **Tamaño:** S

| Área         | Tarea                                                                 |
| ------------- | --------------------------------------------------------------------- |
| Front-End     | Pantallas para solicitar recuperación y registrar nueva contraseña. |
| Back-End      | Implementar flujo con token temporal enviado por correo.              |
| Base de datos | `password_resets`.                                                  |
| Pruebas       | Token válido, expirado e inválido.                                  |

---

### E4-H1 · Bloqueo por múltiples intentos fallidos

**Sprint:** 3 · **Prioridad:** Media · **Tamaño:** S

| Área         | Tarea                                                                                  |
| ------------- | -------------------------------------------------------------------------------------- |
| Front-End     | Mostrar aviso de cuenta bloqueada temporalmente.                                       |
| Back-End      | Registrar intentos fallidos y bloquear temporalmente la cuenta tras múltiples fallos. |
| Base de datos | Reutilizar`login_attempts`, `security_log` o `users.locked_until`.               |
| Pruebas       | Varios intentos fallidos bloquean la cuenta temporalmente.                             |

---

### E4-H3 · Autenticación en dos pasos

**Sprint:** 4 · **Prioridad:** Baja · **Tamaño:** M

| Área         | Tarea                                                                            |
| ------------- | -------------------------------------------------------------------------------- |
| Front-End     | Pantalla para ingresar código 2FA y sección para activar/desactivar.           |
| Back-End      | Implementar enrolamiento, verificación y desactivación de 2FA.                 |
| Base de datos | `two_factor_codes` y campo de estado 2FA en usuario.                           |
| Pruebas       | Activar 2FA, verificar código válido, rechazar código inválido y desactivar. |

---

### E4-H8 · Buenas prácticas de seguridad

**Sprint:** 4 · **Prioridad:** Baja · **Tamaño:** S

| Área         | Tarea                                                                                      |
| ------------- | ------------------------------------------------------------------------------------------ |
| Front-End     | Sin cambios visibles obligatorios.                                                         |
| Back-End      | Aplicar HTTPS, CORS restringido, roles/permisos, bcrypt y protección de datos personales. |
| Base de datos | Sin tablas nuevas obligatorias.                                                            |
| Pruebas       | Verificar cabeceras, cifrado de contraseñas y restricciones de acceso.                    |

---

# Release 2 – Historias pendientes o incompletas

## Épica 1 – Reservas

---

### E1-H10 · Vista de disponibilidad tipo mapa de campus

**Sprint:** 6 · **Prioridad:** Media · **Tamaño:** M

**Descripción:**
Mostrar en un plano simplificado del campus qué salas están libres, ocupadas o en mantenimiento en el momento actual, para que el usuario pueda identificar rápidamente espacios disponibles.

| Área         | Tarea                                                                                                                                                         |
| ------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Front-End     | Crear vista o componente`CampusMapPage` / `CampusMapView` accesible desde la navegación principal, desde la vista de reservas o desde la vista de salas. |
| Front-End     | Agregar ruta en`frontend/luistudio-app/src/viewmodels/routes.ts`, por ejemplo `/campus/map` o `/mapa-campus`.                                           |
| Front-End     | Agregar opción de navegación visible para estudiantes y administradores.                                                                                    |
| Front-End     | Consumir`GET /api/campus/map` usando el método `getCampusMap` ya existente en `frontend/luistudio-app/src/services/api.ts`.                            |
| Front-End     | Representar pabellones o zonas como bloques/secciones del plano simplificado.                                                                                 |
| Front-End     | Mostrar cada sala como tarjeta o bloque visual con color/estado: libre, ocupada o en mantenimiento.                                                           |
| Front-End     | Permitir que al hacer clic en una sala libre se precargue el formulario de reserva con esa sala.                                                              |
| Front-End     | Agregar actualización automática mediante polling cada 30 segundos o intervalo similar.                                                                     |
| Front-End     | Mostrar leyenda de colores y estado: libre, ocupada, mantenimiento.                                                                                           |
| Back-End      | Reutilizar`GET /api/campus/map`, que ya calcula estado actual de salas según reservas activas y mantenimientos.                                            |
| Back-End      | Revisar si el endpoint necesita incluir coordenadas, orden visual, zona, piso o metadata adicional para dibujar mejor el plano.                               |
| Back-End      | Si se requiere ubicación visual fija, extender el DTO`CampusMapResponse` con campos como `positionX`, `positionY`, `floor` o `displayOrder`.       |
| Back-End      | Validar que el estado de sala se calcule usando zona horaria`America/Lima` de forma consistente.                                                            |
| Base de datos | Reutilizar`rooms`, `buildings`, `bookings` y `maintenances`.                                                                                          |
| Base de datos | Opcional: agregar metadata visual de sala, por ejemplo`map_position_x`, `map_position_y`, `floor`, `display_order` o una tabla `room_map_metadata`. |
| Pruebas       | Sala con reserva activa aparece como ocupada.                                                                                                                 |
| Pruebas       | Sala sin reserva activa aparece como libre.                                                                                                                   |
| Pruebas       | Sala con mantenimiento activo aparece como en mantenimiento.                                                                                                  |
| Pruebas       | El mapa actualiza estados sin recargar manualmente.                                                                                                           |
| Pruebas       | Al seleccionar una sala libre, el formulario de reserva queda precargado correctamente.                                                                       |

---

### E1-H12 · Búsqueda inteligente de salas con IA y ranking clásico

**Sprint:** 6 · **Prioridad:** Media · **Tamaño:** L

**Descripción:**
Permitir que el estudiante escriba en lenguaje natural lo que necesita y que el sistema interprete esa intención con IA. La IA no decide la sala final; solo convierte el texto en requisitos estructurados. Luego el backend filtra y ordena salas con reglas deterministas.

**Ejemplo de entrada del usuario:**

> "Quiero un lugar tranquilo para estudiar con mi amigo."

**Ejemplo de intención estructurada:**

```json
{
  "tipo": "estudio_grupal",
  "capacidad_min": 2,
  "ruido_max": "bajo",
  "requiere_concentracion": true
}
```

| Área          | Tarea                                                                                                                                               |
| -------------- | --------------------------------------------------------------------------------------------------------------------------------------------------- |
| Front-End      | Agregar caja de búsqueda inteligente en la vista de reserva o salas. Debe permitir texto libre y mostrar top 3 a 5 recomendaciones.                |
| Front-End      | Mostrar chips o resumen de criterios detectados: capacidad mínima, nivel de ruido, equipamiento, ubicación preferida o tipo de uso.               |
| Front-End      | Agregar método`smartSearchRooms` en `frontend/luistudio-app/src/services/api.ts`.                                                              |
| Front-End      | Permitir seleccionar una sala recomendada para precargar el formulario de reserva.                                                                  |
| Back-End       | Crear endpoint`POST /api/rooms/smart-search`.                                                                                                     |
| Back-End       | Crear DTOs:`SmartRoomSearchRequest`, `SmartRoomSearchResponse`, `RoomIntentDto`, `RankedRoomResponse`.                                      |
| Back-End       | Crear servicio`GroqIntentService` para enviar el texto del usuario a Groq y recibir JSON estructurado.                                            |
| Back-End       | Crear servicio`RoomRankingService` para filtrar y ordenar salas según reglas clásicas.                                                          |
| Back-End       | El ranking debe considerar disponibilidad, capacidad, nivel de ruido, equipamiento, ubicación, estado de la sala y coincidencia con la intención. |
| Back-End       | Si Groq falla o devuelve JSON inválido, usar fallback clásico con búsqueda por texto y filtros existentes.                                       |
| Base de datos  | Agregar metadata rica para salas: nivel de ruido, equipamiento, etiquetas de uso y descripción. Puede ser en columnas nuevas o tabla relacionada.  |
| Base de datos  | Crear migración para`rooms.noise_level`, `rooms.equipment`, `rooms.tags`, `rooms.description` o tabla `room_metadata`.                   |
| Configuración | Agregar variables de entorno`GROQ_API_KEY` y `GROQ_MODEL`.                                                                                      |
| Pruebas        | Probar intención grupal, intención silenciosa, intención con equipamiento y caso de IA no disponible.                                            |
| Pruebas        | Verificar que la IA solo interpreta intención y que la selección final la realiza el backend mediante reglas deterministas.                       |

---

## Épica 2 – Perfiles

---

### E2-H2 · Consultar información de cuenta

**Sprint:** 5 · **Prioridad:** Media · **Tamaño:** S

| Área         | Tarea                                                                                                                                 |
| ------------- | ------------------------------------------------------------------------------------------------------------------------------------- |
| Front-End     | Crear o ajustar pantalla "Mi perfil" para mostrar código, nombres, apellidos, correo, rol y estado del usuario en modo solo lectura. |
| Front-End     | Quitar cualquier campo editable para nombres, apellidos o correo.                                                                     |
| Front-End     | Desde esta zona solo se debe permitir acceder a acciones de seguridad o configuración, no modificar datos personales.                |
| Back-End      | Reutilizar`GET /auth/me` o crear endpoint de perfil en modo lectura si se necesita más información.                               |
| Base de datos | No requiere cambios. Reutiliza`users`.                                                                                              |
| Pruebas       | Verificar que los datos se muestran correctamente y que no existe opción para editar nombres, apellidos ni correo.                   |

---

### E2-H7 · Historial de actividad de cuenta

**Sprint:** 5 · **Prioridad:** Media · **Tamaño:** M

| Área         | Tarea                                                                                                                                                   |
| ------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Front-End     | Crear vista "Actividad de cuenta" dentro de perfil o configuración.                                                                                    |
| Front-End     | Mostrar eventos con fecha, tipo de evento, IP, dispositivo y estado.                                                                                    |
| Back-End      | Implementar endpoint`GET /api/me/activity`.                                                                                                           |
| Back-End      | Registrar y consultar eventos de inicio de sesión, cambios sensibles y cierres de sesión.                                                             |
| Back-End      | No incluir cambios de perfil como evento principal, porque el usuario no podrá editar nombres, apellidos ni correo.                                    |
| Base de datos | Reutilizar`audit_log` o crear `account_activity`.                                                                                                   |
| Base de datos | Si se usa`audit_log`, agregar acciones como `LOGIN_SUCCESS`, `SENSITIVE_CHANGE_CONFIRMED`, `LOGOUT_CURRENT`, `LOGOUT_REMOTE`, `LOGOUT_ALL`. |
| Pruebas       | Verificar que se registren inicios de sesión, cambios sensibles y cierres de sesión.                                                                  |
| Pruebas       | Verificar orden descendente por fecha y acceso solo al propio historial.                                                                                |

---

## Épica 3 – Notificaciones

---

### E3-H13 · Suscripción a disponibilidad de sala

**Sprint:** 6 · **Prioridad:** Media · **Tamaño:** M

| Área         | Tarea                                                                                                                                                                            |
| ------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Front-End     | En la vista de salas, reservas o mapa de campus, mostrar opción "Avisarme cuando se libere" cuando una sala esté ocupada o no disponible en el horario consultado.             |
| Front-End     | Permitir cancelar la suscripción activa del usuario.                                                                                                                            |
| Back-End      | Crear endpoint`POST /api/rooms/{roomId}/availability-subscriptions`.                                                                                                           |
| Back-End      | Crear endpoint`DELETE /api/rooms/{roomId}/availability-subscriptions/me`.                                                                                                      |
| Back-End      | Crear endpoint opcional`GET /api/me/availability-subscriptions`.                                                                                                               |
| Base de datos | Crear tabla`room_availability_subscriptions` con `id`, `user_id`, `room_id`, `target_date`, `start_time`, `end_time`, `status`, `created_at`, `notified_at`. |
| Base de datos | Agregar restricción única para evitar suscripciones activas duplicadas del mismo usuario para la misma sala, fecha y horario.                                                  |
| Pruebas       | Registrar suscripción, evitar duplicado activo y cancelar suscripción.                                                                                                         |
| Pruebas       | Verificar que solo el usuario autenticado pueda gestionar sus propias suscripciones.                                                                                             |

---

### E3-H4 · Notificación cuando una sala se libera

**Sprint:** 6 · **Prioridad:** Baja · **Tamaño:** S

| Área         | Tarea                                                                                                      |
| ------------- | ---------------------------------------------------------------------------------------------------------- |
| Front-End     | Mostrar confirmación cuando el usuario active la opción "Avisarme cuando se libere".                     |
| Front-End     | Mostrar notificación o correo recibido cuando la sala se libera.                                          |
| Back-End      | Al cancelar una reserva o cambiar la disponibilidad de una sala, buscar suscripciones activas compatibles. |
| Back-End      | Encolar correo en`email_outbox` para los usuarios suscritos.                                             |
| Back-End      | Marcar la suscripción como notificada para evitar envíos duplicados.                                     |
| Base de datos | Reutilizar`room_availability_subscriptions` y `email_outbox`.                                          |
| Pruebas       | Cancelar una reserva y verificar que se notifica a usuarios suscritos.                                     |
| Pruebas       | Verificar que una suscripción notificada no vuelva a enviar correo por el mismo evento.                   |

---

### E3-H9 · Notificación sobre nuevas salas o cambios de política

**Sprint:** 6 · **Prioridad:** Baja · **Tamaño:** S

| Área         | Tarea                                                                                                    |
| ------------- | -------------------------------------------------------------------------------------------------------- |
| Front-End     | Crear formulario administrativo para redactar comunicado institucional o aviso de nueva sala.            |
| Front-End     | Permitir seleccionar tipo de comunicado: nueva sala, cambio de política u otro aviso institucional.     |
| Back-End      | Crear endpoint`POST /api/admin/announcements`.                                                         |
| Back-End      | Encolar notificación masiva a estudiantes activos.                                                      |
| Back-End      | Reutilizar`EmailOutboxService` para respetar preferencias de notificación cuando corresponda.         |
| Base de datos | Crear tabla opcional`institutional_announcements` con título, contenido, tipo, autor, fecha y estado. |
| Base de datos | Reutilizar`email_outbox` para el envío.                                                               |
| Pruebas       | Crear comunicado como administrador y verificar envío a usuarios objetivo.                              |
| Pruebas       | Verificar que un estudiante no pueda publicar comunicados.                                               |

---

### E3-H10 · Notificación por inasistencia

**Sprint:** 6 · **Prioridad:** Media · **Tamaño:** M

| Área         | Tarea                                                                                                                                            |
| ------------- | ------------------------------------------------------------------------------------------------------------------------------------------------ |
| Front-End     | Mostrar historial de inasistencias en vista administrativa del estudiante o en dashboard.                                                        |
| Front-End     | Si se implementa vista del estudiante, mostrar inasistencias propias con fecha, sala y horario.                                                  |
| Back-End      | Crear job programado que revise reservas activas cuya hora de inicio ya pasó por más de 15 minutos.                                            |
| Back-End      | Si no existe registro de asistencia dentro de la tolerancia de 15 minutos, marcar la reserva como inasistencia.                                  |
| Back-End      | Encolar correo de aviso al estudiante.                                                                                                           |
| Back-End      | Evitar duplicar inasistencias ya registradas.                                                                                                    |
| Base de datos | Agregar columna`bookings.attendance_status` o `bookings.asistio`.                                                                            |
| Base de datos | Crear tabla`attendance_records` o `historial_inasistencias` con `id`, `user_id`, `booking_id`, `recorded_at`, `tolerance_minutes`. |
| Base de datos | Agregar índice por`booking_id` y `user_id`.                                                                                                 |
| Pruebas       | Reserva sin asistencia después de 15 minutos genera inasistencia.                                                                               |
| Pruebas       | Reserva dentro de los primeros 15 minutos no debe marcarse como inasistencia.                                                                    |
| Pruebas       | Verificar que no se dupliquen registros para la misma reserva.                                                                                   |

---

## Épica 4 – Seguridad

---

### E4-H6 · Alerta por acceso desde dispositivo o ubicación no habitual

**Sprint:** 5 · **Prioridad:** Baja · **Tamaño:** S

| Área         | Tarea                                                                                                             |
| ------------- | ----------------------------------------------------------------------------------------------------------------- |
| Front-End     | Opcional: mostrar aviso informativo en historial de actividad o configuración de seguridad.                      |
| Back-End      | Capturar IP y user-agent en login. Actualmente el login registra IP, pero no user-agent ni dispositivo.           |
| Back-End      | Crear lógica para comparar el acceso actual contra sesiones o accesos previos del usuario.                       |
| Back-End      | Si el dispositivo/IP no es habitual, encolar correo de alerta.                                                    |
| Back-End      | No confundir esta alerta con el bloqueo por intentos fallidos ya existente.                                       |
| Base de datos | Extender`login_attempts` con `user_agent`, `device_label` y/o crear `login_sessions`/`security_events`. |
| Pruebas       | Login desde nuevo dispositivo o user-agent genera alerta.                                                         |
| Pruebas       | Login repetido desde dispositivo conocido no debe generar alerta innecesaria.                                     |

---

### E4-H7 · Cierre de sesiones activas desde el perfil

**Sprint:** 5 · **Prioridad:** Media · **Tamaño:** M

| Área         | Tarea                                                                                                                                                                            |
| ------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Front-End     | Crear sección "Sesiones activas" dentro de configuración o perfil.                                                                                                             |
| Front-End     | Mostrar sesión actual y sesiones remotas con fecha, IP y dispositivo.                                                                                                           |
| Front-End     | Agregar botón para cerrar una sesión remota.                                                                                                                                   |
| Front-End     | Agregar opción "Cerrar todas las sesiones".                                                                                                                                     |
| Front-End     | Ajustar la zona actual de cierre de sesión para que también registre el cierre de la sesión actual.                                                                           |
| Back-End      | Crear endpoint`GET /api/me/sessions`.                                                                                                                                          |
| Back-End      | Crear endpoint`DELETE /api/me/sessions/{sessionId}` para cerrar sesión remota.                                                                                                |
| Back-End      | Crear endpoint`DELETE /api/me/sessions` para cerrar todas las sesiones.                                                                                                        |
| Back-End      | Ajustar`POST /api/auth/logout` para registrar evento de cierre de sesión actual. Actualmente solo limpia la cookie de autenticación.                                         |
| Base de datos | Crear tabla`login_sessions` o `refresh_tokens` con `id`, `user_id`, `token_id`, `ip`, `user_agent`, `created_at`, `last_seen_at`, `revoked_at`, `current`. |
| Base de datos | Agregar índice por`user_id` y `revoked_at`.                                                                                                                                 |
| Pruebas       | Cerrar sesión actual limpia cookie y registra evento.                                                                                                                           |
| Pruebas       | Cerrar sesión remota invalida token/sesión seleccionada.                                                                                                                       |
| Pruebas       | Cerrar todas las sesiones revoca todas las sesiones activas del usuario.                                                                                                         |

---

### E4-H9 · Confirmación por correo para cambios sensibles

**Sprint:** 5 · **Prioridad:** Media · **Tamaño:** M

| Área         | Tarea                                                                                                                                               |
| ------------- | --------------------------------------------------------------------------------------------------------------------------------------------------- |
| Front-End     | Crear flujo de confirmación para acciones sensibles desde configuración de seguridad.                                                             |
| Front-End     | Mostrar mensaje indicando que se envió un correo de confirmación.                                                                                 |
| Back-End      | Crear endpoint para solicitar confirmación de cambio sensible.                                                                                     |
| Back-End      | Crear endpoint para confirmar token temporal.                                                                                                       |
| Back-End      | Aplicar confirmación para cambio de contraseña desde perfil, desactivación de 2FA y cierre global de sesiones si se decide proteger esa acción. |
| Base de datos | Crear`sensitive_change_tokens` o reutilizar un mecanismo similar a `password_resets`.                                                           |
| Base de datos | Campos sugeridos:`id`, `user_id`, `action_type`, `token`, `payload`, `expires_at`, `used`, `created_at`.                            |
| Pruebas       | Token válido aplica el cambio.                                                                                                                     |
| Pruebas       | Token expirado o usado no aplica el cambio.                                                                                                         |
| Pruebas       | Acción sensible no debe ejecutarse sin confirmación.                                                                                              |

---

### E4-H10 · Historial de intentos fallidos y bloqueos

**Sprint:** 5 · **Prioridad:** Baja · **Tamaño:** S

| Área         | Tarea                                                                                                  |
| ------------- | ------------------------------------------------------------------------------------------------------ |
| Front-End     | Crear vista administrativa de eventos de seguridad.                                                    |
| Front-End     | Agregar filtros por usuario, correo, fecha, éxito/fallo y estado de bloqueo.                          |
| Back-End      | Crear endpoint`GET /api/admin/security/login-attempts`.                                              |
| Back-End      | Permitir consultar intentos fallidos, IP, usuario, fecha y bloqueo temporal.                           |
| Back-End      | Reutilizar la información de`login_attempts`, que actualmente registra usuario, fecha, éxito e IP. |
| Base de datos | Reutilizar`login_attempts` y `users.locked_until`.                                                 |
| Base de datos | Agregar índices si la consulta por fecha o usuario se vuelve pesada.                                  |
| Pruebas       | Ver historial filtrado por usuario, fecha y estado.                                                    |
| Pruebas       | Validar que solo administrador pueda acceder.                                                          |

---

## Épica 5 – Experiencia de Usuario

---

### E5-H1 · Dashboard administrativo con métricas de uso

**Sprint:** 6 · **Prioridad:** Media · **Tamaño:** 

# Tareas por Épica – Luistudio

Este archivo unifica las tareas por historia de usuario del proyecto Luistudio.

---

# Release 1 – Historias oficiales

## Épica 1 – Reservas

---

### E1-H8 · Reservar una sala de estudio

**Sprint:** 1 · **Prioridad:** Alta · **Tamaño:** L

| Área         | Tarea                                                                                                                                                                            |
| ------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------------------------------- |
| Front-End     | Formulario transaccional para reservar sala con fecha, hora de inicio y hora de fin. Mostrar mensaje: "Reserva registrada con éxito. Tu reserva fue registrada correctamente.". |
| Back-End      | Implementar`POST /bookings`, validando disponibilidad de sala, horario permitido, usuario autenticado y ausencia de solapamientos.                                             |
| Base de datos | Reutilizar`bookings`, `rooms` y `users`. Mantener índices para búsqueda por sala, fecha y horario.                                                                       |
| Pruebas       | Crear reserva válida, rechazar reservas solapadas y validar que se muestre la confirmación correcta.                                                                           |

---

### E1-H9 · Confirmación de reserva por correo

**Sprint:** 1 · **Prioridad:** Alta · **Tamaño:** S

| Área         | Tarea                                                                 |
| ------------- | --------------------------------------------------------------------- |
| Front-End     | Mostrar confirmación visual al estudiante después de reservar.      |
| Back-End      | Enviar o encolar correo automático al concretar la reserva.          |
| Base de datos | Reutilizar`email_outbox`.                                           |
| Pruebas       | Verificar que al crear una reserva se genere correo de confirmación. |

---

### E1-H5 · Ver reservas realizadas

**Sprint:** 1 · **Prioridad:** Alta · **Tamaño:** S

| Área         | Tarea                                                                                                            |
| ------------- | ---------------------------------------------------------------------------------------------------------------- |
| Front-End     | Vista administrativa de reservas activas.                                                                        |
| Back-End      | Implementar`GET /admin/bookings` para listar reservas realizadas.                                              |
| Base de datos | Reutilizar`bookings`.                                                                                          |
| Pruebas       | Verificar que el administrador pueda ver reservas y que el estudiante no tenga acceso a la vista administrativa. |

---

### E1-H11 · Configurar límites de reserva

**Sprint:** 1 · **Prioridad:** Alta · **Tamaño:** M

| Área         | Tarea                                                                                                         |
| ------------- | ------------------------------------------------------------------------------------------------------------- |
| Front-End     | Panel administrativo para configurar cantidad máxima de reservas simultáneas y duración máxima permitida. |
| Back-End      | Implementar consulta y actualización de configuración. Validar límites al crear una reserva.               |
| Base de datos | Reutilizar o crear configuración del sistema para límites de reserva.                                       |
| Pruebas       | Rechazar reservas que superen la cantidad máxima o duración máxima configurada.                            |

---

### E1-H1 · Registrar salas de estudio

**Sprint:** 2 · **Prioridad:** Media · **Tamaño:** S

| Área         | Tarea                                                                                  |
| ------------- | -------------------------------------------------------------------------------------- |
| Front-End     | Formulario o modal para registrar una nueva sala de estudio.                           |
| Back-End      | Implementar`POST /rooms` con validación de datos obligatorios.                      |
| Base de datos | Reutilizar`rooms`.                                                                   |
| Pruebas       | Crear sala válida, validar campos obligatorios y restringir acceso a administradores. |

---

### E1-H4 · Filtrar salas por ubicación

**Sprint:** 2 · **Prioridad:** Baja · **Tamaño:** M

| Área         | Tarea                                                          |
| ------------- | -------------------------------------------------------------- |
| Front-End     | Filtro por ubicación en la vista de salas.                    |
| Back-End      | Implementar`GET /rooms?ubicacion=X`.                         |
| Base de datos | Reutilizar`rooms` e índice por ubicación si corresponde.   |
| Pruebas       | Filtrar salas por ubicación y verificar resultados correctos. |

---

### E1-H6 · Modificar una reserva

**Sprint:** 2 · **Prioridad:** Media · **Tamaño:** M

| Área         | Tarea                                                                                             |
| ------------- | ------------------------------------------------------------------------------------------------- |
| Front-End     | Acción "Editar" en la tabla de reservas con formulario precargado.                               |
| Back-End      | Implementar`PUT /bookings/{id}` para modificar sala, fecha u horario, validando disponibilidad. |
| Base de datos | Reutilizar`bookings`. Registrar `updated_at` y `updated_by` si aplica.                      |
| Pruebas       | Modificar una reserva, rechazar solapamientos y confirmar actualización correcta.                |

---

## Épica 2 – Perfiles

---

### E2-H1 · Ver perfiles registrados

**Sprint:** 2 · **Prioridad:** Media · **Tamaño:** M

| Área         | Tarea                                                                                                                  |
| ------------- | ---------------------------------------------------------------------------------------------------------------------- |
| Front-End     | Pantalla de usuarios con tabla paginada de 10 por página. Columnas: id, código, correo, nombres, apellidos y estado. |
| Back-End      | Implementar`GET /admin/users`.                                                                                       |
| Base de datos | Reutilizar`users`.                                                                                                   |
| Pruebas       | Listar usuarios y validar paginación.                                                                                 |

---

### E2-H5 · Buscar usuarios por código o correo

**Sprint:** 3 · **Prioridad:** Baja · **Tamaño:** S

| Área         | Tarea                                                                                                                      |
| ------------- | -------------------------------------------------------------------------------------------------------------------------- |
| Front-End     | Barra de búsqueda en la pantalla de usuarios.                                                                             |
| Back-End      | Implementar búsqueda por código o correo institucional usando coincidencia parcial insensible a mayúsculas/minúsculas. |
| Base de datos | Reutilizar índices por código y correo si existen.                                                                       |
| Pruebas       | Buscar por código, correo completo y coincidencia parcial.                                                                |

---

## Épica 3 – Notificaciones

---

### E3-H2 · Recordatorio automático antes de la reserva

**Sprint:** 3 · **Prioridad:** Media · **Tamaño:** S

| Área         | Tarea                                                                                        |
| ------------- | -------------------------------------------------------------------------------------------- |
| Front-End     | Opcional: mostrar etiqueta de próxima reserva en "Mis reservas".                            |
| Back-End      | Job que envía correo antes de la hora de reserva.                                           |
| Base de datos | Reutilizar`bookings` y `email_outbox`.                                                   |
| Pruebas       | Verificar envío antes del inicio de la reserva y evitar envío si la reserva fue cancelada. |

---

### E3-H3 · Notificación por modificación de reserva

**Sprint:** 3 · **Prioridad:** Media · **Tamaño:** M

| Área         | Tarea                                                                        |
| ------------- | ---------------------------------------------------------------------------- |
| Front-End     | Mostrar mensaje de confirmación al guardar cambios.                         |
| Back-End      | Enviar correo con datos anteriores y nuevos cuando se modifique una reserva. |
| Base de datos | Reutilizar`email_outbox` y `audit_log` si aplica.                        |
| Pruebas       | Editar reserva y verificar correo con comparación antes/después.           |

---

### E3-H5 · Notificación por cancelación de reserva

**Sprint:** 3 · **Prioridad:** Media · **Tamaño:** M

| Área         | Tarea                                                                  |
| ------------- | ---------------------------------------------------------------------- |
| Front-End     | Mostrar estado "Cancelada" en las vistas correspondientes.             |
| Back-End      | Enviar correo automático cuando el administrador cancela una reserva. |
| Base de datos | Reutilizar`email_outbox` y `bookings`.                             |
| Pruebas       | Cancelar reserva y verificar notificación al estudiante.              |

---

### E3-H7 · Registrar mantenimiento de salas

**Sprint:** 4 · **Prioridad:** Baja · **Tamaño:** S

| Área         | Tarea                                                                                |
| ------------- | ------------------------------------------------------------------------------------ |
| Front-End     | Formulario para registrar periodos de mantenimiento o no disponibilidad de una sala. |
| Back-End      | Implementar endpoints para crear y consultar periodos de no disponibilidad.          |
| Base de datos | Reutilizar o crear`rooms_unavailability`.                                          |
| Pruebas       | Bloquear reservas durante periodos de mantenimiento.                                 |

---

### E3-H8 · Configurar preferencias de notificación

**Sprint:** 4 · **Prioridad:** Baja · **Tamaño:** M

| Área         | Tarea                                                                                                  |
| ------------- | ------------------------------------------------------------------------------------------------------ |
| Front-End     | Vista de preferencias con interruptores por tipo de notificación.                                     |
| Back-End      | Implementar consulta y guardado de preferencias del usuario. Respetar preferencias al encolar correos. |
| Base de datos | Reutilizar preferencias de usuario.                                                                    |
| Pruebas       | Desactivar correo y validar que no se encolen notificaciones deshabilitadas.                           |

---

## Épica 4 – Seguridad

---

### E4-H1 · Inicio de sesión con roles

**Sprint:** 1 · **Prioridad:** Alta · **Tamaño:** M

| Área         | Tarea                                                                       |
| ------------- | --------------------------------------------------------------------------- |
| Front-End     | Pantalla de login y redirección según rol.                                |
| Back-End      | Implementar login con validación bcrypt y sesión/token de autenticación. |
| Base de datos | Reutilizar`users`, `roles` y campos de seguridad necesarios.            |
| Pruebas       | Login correcto, credenciales incorrectas y redirección por rol.            |

---

### E4-H5 · Restablecer contraseña

**Sprint:** 1 · **Prioridad:** Alta · **Tamaño:** S

| Área         | Tarea                                                                 |
| ------------- | --------------------------------------------------------------------- |
| Front-End     | Pantallas para solicitar recuperación y registrar nueva contraseña. |
| Back-End      | Implementar flujo con token temporal enviado por correo.              |
| Base de datos | `password_resets`.                                                  |
| Pruebas       | Token válido, expirado e inválido.                                  |

---

### E4-H1 · Bloqueo por múltiples intentos fallidos

**Sprint:** 3 · **Prioridad:** Media · **Tamaño:** S

| Área         | Tarea                                                                                  |
| ------------- | -------------------------------------------------------------------------------------- |
| Front-End     | Mostrar aviso de cuenta bloqueada temporalmente.                                       |
| Back-End      | Registrar intentos fallidos y bloquear temporalmente la cuenta tras múltiples fallos. |
| Base de datos | Reutilizar`login_attempts`, `security_log` o `users.locked_until`.               |
| Pruebas       | Varios intentos fallidos bloquean la cuenta temporalmente.                             |

---

### E4-H3 · Autenticación en dos pasos

**Sprint:** 4 · **Prioridad:** Baja · **Tamaño:** M

| Área         | Tarea                                                                            |
| ------------- | -------------------------------------------------------------------------------- |
| Front-End     | Pantalla para ingresar código 2FA y sección para activar/desactivar.           |
| Back-End      | Implementar enrolamiento, verificación y desactivación de 2FA.                 |
| Base de datos | `two_factor_codes` y campo de estado 2FA en usuario.                           |
| Pruebas       | Activar 2FA, verificar código válido, rechazar código inválido y desactivar. |

---

### E4-H8 · Buenas prácticas de seguridad

**Sprint:** 4 · **Prioridad:** Baja · **Tamaño:** S

| Área         | Tarea                                                                                      |
| ------------- | ------------------------------------------------------------------------------------------ |
| Front-End     | Sin cambios visibles obligatorios.                                                         |
| Back-End      | Aplicar HTTPS, CORS restringido, roles/permisos, bcrypt y protección de datos personales. |
| Base de datos | Sin tablas nuevas obligatorias.                                                            |
| Pruebas       | Verificar cabeceras, cifrado de contraseñas y restricciones de acceso.                    |

---

# Release 2 – Historias pendientes o incompletas

## Épica 2 – Perfiles

---

### E2-H2 · Actualizar información personal

**Sprint:** 5 · **Prioridad:** Media · **Tamaño:** S

| Área         | Tarea                                                                           |
| ------------- | ------------------------------------------------------------------------------- |
| Front-End     | Crear pantalla "Mi perfil" con campos editables de nombres, apellidos y correo. |
| Back-End      | Implementar endpoint para actualizar datos personales del usuario autenticado.  |
| Base de datos | Reutilizar`users`.                                                            |
| Pruebas       | Guardar cambios y mostrar mensaje: "Se guardaron correctamente los cambios".    |

---

### E2-H4 · Notificación si la cuenta es modificada

**Sprint:** 5 · **Prioridad:** Baja · **Tamaño:** S

| Área         | Tarea                                                                                |
| ------------- | ------------------------------------------------------------------------------------ |
| Front-End     | Sin UI adicional obligatoria.                                                        |
| Back-End      | Después de actualizar perfil, encolar correo con detalle de los campos modificados. |
| Base de datos | Reutilizar`email_outbox`.                                                          |
| Pruebas       | Modificar perfil y verificar correo con detalle del cambio.                          |

---

### E2-H7 · Historial de actividad de cuenta

**Sprint:** 5 · **Prioridad:** Media · **Tamaño:** M

| Área         | Tarea                                                                                                                   |
| ------------- | ----------------------------------------------------------------------------------------------------------------------- |
| Front-End     | Crear vista "Actividad de cuenta" dentro del perfil del usuario.                                                        |
| Back-End      | Implementar endpoint para listar eventos del usuario: login, cambios de perfil, cambios sensibles y cierres de sesión. |
| Base de datos | Reutilizar`audit_log` o crear `account_activity`.                                                                   |
| Pruebas       | Registrar eventos y mostrarlos ordenados por fecha descendente.                                                         |

---

## Épica 3 – Notificaciones

---

### E3-H4 · Notificación cuando una sala se libera

**Sprint:** 6 · **Prioridad:** Baja · **Tamaño:** S

| Área         | Tarea                                                                                                   |
| ------------- | ------------------------------------------------------------------------------------------------------- |
| Front-End     | Permitir que el estudiante se suscriba a una sala ocupada o ingrese a lista de espera.                  |
| Back-End      | Al cancelarse una reserva o liberarse una sala, notificar a estudiantes suscritos o en lista de espera. |
| Base de datos | Crear`room_subscriptions` o `waiting_list`. Reutilizar `email_outbox`.                            |
| Pruebas       | Cancelar reserva y verificar notificación a interesados.                                               |

---

### E3-H9 · Notificación sobre nuevas salas o cambios de política

**Sprint:** 6 · **Prioridad:** Baja · **Tamaño:** S

| Área         | Tarea                                                                                         |
| ------------- | --------------------------------------------------------------------------------------------- |
| Front-End     | Crear formulario administrativo para redactar comunicado institucional o aviso de nueva sala. |
| Back-End      | Implementar endpoint para enviar notificación masiva a estudiantes.                          |
| Base de datos | Reutilizar`email_outbox`. Opcional: crear `institutional_announcements`.                  |
| Pruebas       | Crear comunicado y verificar envío a usuarios objetivo.                                      |

---

### E3-H10 · Notificación por inasistencia

**Sprint:** 6 · **Prioridad:** Baja · **Tamaño:** S

| Área         | Tarea                                                                                          |
| ------------- | ---------------------------------------------------------------------------------------------- |
| Front-End     | Mostrar historial de inasistencias al administrador y, si aplica, al estudiante.               |
| Back-End      | Job que detecta reservas sin asistencia registrada, marca inasistencia y encola notificación. |
| Base de datos | Agregar o utilizar`bookings.asistio` y crear `historial_inasistencias`.                    |
| Pruebas       | Reserva sin asistencia genera registro y correo de aviso.                                      |

---

## Épica 4 – Seguridad

---

### E4-H6 · Alerta por acceso desde dispositivo o ubicación no habitual

**Sprint:** 5 · **Prioridad:** Baja · **Tamaño:** S

| Área         | Tarea                                                                                           |
| ------------- | ----------------------------------------------------------------------------------------------- |
| Front-End     | Opcional: mostrar aviso informativo en el perfil o historial de actividad.                      |
| Back-End      | Registrar IP, user-agent y dispositivo. Detectar acceso no habitual y encolar correo de alerta. |
| Base de datos | Crear o extender`login_sessions`, `security_events` o `login_attempts`.                   |
| Pruebas       | Login desde nuevo dispositivo/IP genera alerta sin duplicar el bloqueo por intentos fallidos.   |

---

### E4-H7 · Cierre de sesiones activas desde el perfil

**Sprint:** 5 · **Prioridad:** Media · **Tamaño:** M

| Área         | Tarea                                                                                                 |
| ------------- | ----------------------------------------------------------------------------------------------------- |
| Front-End     | Crear vista de sesiones activas con botón "Cerrar sesión" por dispositivo y opción "Cerrar todas". |
| Back-End      | Implementar endpoints para listar y revocar sesiones/tokens activos.                                  |
| Base de datos | Crear o reutilizar`login_sessions` o `refresh_tokens`.                                            |
| Pruebas       | Cerrar sesión remota invalida token y mantiene activa la sesión actual si corresponde.              |

---

### E4-H9 · Confirmación por correo para cambios sensibles

**Sprint:** 5 · **Prioridad:** Media · **Tamaño:** M

| Área         | Tarea                                                                          |
| ------------- | ------------------------------------------------------------------------------ |
| Front-End     | Crear flujo de confirmación para cambio de correo o contraseña desde perfil. |
| Back-End      | Generar token temporal enviado por correo antes de aplicar cambios sensibles.  |
| Base de datos | Crear`sensitive_change_tokens` o reutilizar mecanismo de tokens temporales.  |
| Pruebas       | Token válido aplica cambio; token inválido o expirado lo rechaza.            |

---

### E4-H10 · Historial de intentos fallidos y bloqueos

**Sprint:** 5 · **Prioridad:** Baja · **Tamaño:** S

| Área         | Tarea                                                                                 |
| ------------- | ------------------------------------------------------------------------------------- |
| Front-End     | Crear vista administrativa de eventos de seguridad con filtros.                       |
| Back-End      | Implementar endpoint para consultar intentos fallidos, IP, usuario, fecha y bloqueos. |
| Base de datos | Reutilizar`login_attempts`, `users.locked_until` o `security_log`.              |
| Pruebas       | Ver historial filtrado por usuario, fecha y estado.                                   |

---

## Épica 5 – Experiencia de Usuario

---

### E5-H1 · Dashboard administrativo con métricas de uso

**Sprint:** 6 · **Prioridad:** Media · **Tamaño:** L

| Área         | Tarea                                                                                                                   |
| ------------- | ----------------------------------------------------------------------------------------------------------------------- |
| Front-End     | Crear dashboard con gráficos/tarjetas de ocupación, horas pico, estudiantes con más reservas y tasa de inasistencia. |
| Back-End      | Implementar endpoints de analítica: ocupación por sala, horas pico, top estudiantes y tasa de inasistencia.           |
| Base de datos | Reutilizar`bookings`, `rooms`, `users` e `historial_inasistencias`. Opcional: crear vista o consulta agregada.  |
| Pruebas       | Validar cálculos con datos conocidos y restringir acceso solo a administrador.                                         |
