# Tareas por Épica – Luistudio

Cada historia incluye sus tareas de Front-End, Back-End, tablas de BD y pruebas requeridas.  
Las historias marcadas con 🆕 son funcionalidades nuevas no contempladas en el backlog original.

---

## Épica 1 – Gestionar Reserva

---

### E1-H8 · Reservar una sala de estudio
**Sprint:** 1 · **Prioridad:** Alta · **Tamaño:** L

| Área | Tarea |
|------|-------|
| **Front-End** | Página "Reservar" con selector de sala, fecha y hora. Mensajes claros de confirmación y error. |
| **Back-End** | `POST /bookings`: recibe sala, fecha y hora; verifica que la sala esté libre; si está libre guarda la reserva y devuelve su ID. `GET /rooms/available?fecha&hora` para mostrar salas libres. |
| **Tablas BD** | `rooms(id, nombre, capacidad, ubicacion)`. `bookings(id, user_id, room_id, fecha, hora_ini, hora_fin, estado)`. Índice `(room_id, fecha, hora_ini)`. |
| **Pruebas** | Crear reserva válida; rechazar solapamientos; respuesta contiene `booking_id`. |

---

### E1-H9 · Recibir confirmación de reserva por correo
**Sprint:** 1 · **Prioridad:** Alta · **Tamaño:** S

| Área | Tarea |
|------|-------|
| **Front-End** | Toast de confirmación en pantalla. Sección "Mis reservas" (versión básica). |
| **Back-End** | Después de crear la reserva, enviar correo con ID, sala, fecha y hora. Si el correo falla, reintentarlo automáticamente. |
| **Tablas BD** | `email_outbox(id, to, subject, body, status, intento)`. |
| **Pruebas** | Al crear reserva llega correo; simular fallo y verificar reintento. |

---

### E1-H5 · Ver reservas realizadas
**Sprint:** 1 · **Prioridad:** Alta · **Tamaño:** S

| Área | Tarea |
|------|-------|
| **Front-End** | Vista "Reservas" (admin) con tabla paginada y filtros simples. |
| **Back-End** | `GET /admin/bookings`: lista paginada de reservas. Solo accesible para administradores (403 a estudiantes). |
| **Tablas BD** | Reutiliza `bookings`. Índice por `fecha` para ordenar/filtrar. |
| **Pruebas** | Listado correcto; acceso denegado a estudiante; paginación funciona. |

---

### E1-H1 · Agregar una sala de estudio
**Sprint:** 2 · **Prioridad:** Media · **Tamaño:** S

| Área | Tarea |
|------|-------|
| **Front-End** | Modal "Nueva sala" con campos: nombre, capacidad, ubicación. |
| **Back-End** | `POST /rooms`: guarda una sala nueva. Devuelve el ID. Si falta el nombre, responde con mensaje de error claro. |
| **Tablas BD** | `rooms(id, nombre, capacidad, ubicacion)`. Índice por `ubicacion`. |
| **Pruebas** | Crear sala válida; error si nombre vacío; solo admin puede hacerlo. |

---

### E1-H2 · Editar información de sala
**Sprint:** 2 · **Prioridad:** Baja · **Tamaño:** S

| Área | Tarea |
|------|-------|
| **Front-End** | Modal "Editar sala" con campos precargados. |
| **Back-End** | `PUT /PATCH /rooms/{id}`: actualiza los datos de la sala. Si no existe, responde 404. |
| **Tablas BD** | Sin cambios de esquema. |
| **Pruebas** | Editar y verificar cambios guardados; manejar 404. |

---

### E1-H3 · Eliminar una sala de estudio
**Sprint:** 2 · **Prioridad:** Baja · **Tamaño:** S

| Área | Tarea |
|------|-------|
| **Front-End** | Diálogo de confirmación antes de eliminar. |
| **Back-End** | `DELETE /rooms/{id}`: elimina la sala. Si tiene reservas futuras, bloquea la acción y muestra mensaje. |
| **Tablas BD** | `rooms` puede usar borrado lógico (`activa: boolean`). FK `bookings.room_id` protege reservas. |
| **Pruebas** | Eliminar sala sin reservas; bloqueo con mensaje si tiene reservas futuras. |

---

### E1-H4 · Filtrar salas por ubicación
**Sprint:** 2 · **Prioridad:** Baja · **Tamaño:** M

| Área | Tarea |
|------|-------|
| **Front-End** | Filtro por ubicación/pabellón en pantalla de Salas (dropdown). |
| **Back-End** | `GET /rooms?ubicacion=X`: devuelve solo salas de esa ubicación. |
| **Tablas BD** | Índice por `ubicacion`. |
| **Pruebas** | Filtrar "Ingeniería" y ver solo esas salas. |

---

### E1-H6 · Modificar una reserva
**Sprint:** 2 · **Prioridad:** Media · **Tamaño:** M

| Área | Tarea |
|------|-------|
| **Front-End** | Acción "Editar" desde tabla de reservas con formulario precargado. |
| **Back-End** | `PUT /bookings/{id}`: cambia fecha/hora/sala; verifica disponibilidad antes de guardar. |
| **Tablas BD** | `bookings` agrega `updated_at`, `updated_by`. |
| **Pruebas** | Cambiar horario; rechazar si se solapa; mensaje de actualización correcto. |

---

### E1-H7 · Cancelar una reserva
**Sprint:** 2 · **Prioridad:** Media · **Tamaño:** S

| Área | Tarea |
|------|-------|
| **Front-End** | Acción "Cancelar" con diálogo de confirmación. |
| **Back-End** | `DELETE` o `PATCH /bookings/{id}`: marca la reserva c?mo `Cancelada`. |
| **Tablas BD** | `bookings.estado {Activa, Cancelada}`. |
| **Pruebas** | Cancelar y ver estado "Cancelada"; mensaje de confirmación. |

---

### 🆕 E1-H10 · Vista de disponibilidad – mapa del campus
**Sprint:** 2 · **Prioridad:** Media · **Tamaño:** L

**Descripción:** Mostrar un plano simplificado del campus donde cada sala refleje su estado actual (libre / ocupada / en mantenimiento) en tiempo real, sin necesidad de recargar la página.

| Área | Tarea |
|------|-------|
| **Front-End** | Componente de mapa SVG interactivo por pabellón. Cada sala es un bloque clicable con color según estado: 🟢 libre / 🔴 ocupada / 🟡 en mantenimiento. Al hacer clic en sala libre, redirige directamente al formulario de reserva precargado con esa sala. Requiere polling cada 30 s o WebSocket para actualización en tiempo real. |
| **Back-End** | `GET /campus/map`: devuelve la lista de pabellones, salas y su estado actual calculado (`libre`, `ocupada`, `mantenimiento`). El estado se calcula cruzando `bookings` activos y `rooms_unavailability` contra el timestamp actual. |
| **Tablas BD** | Reutiliza `rooms`, `bookings`, `rooms_unavailability`, `pabellones`. Sin tablas nuevas. Agregar campo `pavillon_id` en `rooms` si aún no existe. |
| **Pruebas** | Sala con reserva activa aparece en rojo; sala libre aparece en verde; sala en mantenimiento aparece en amarillo. Cambio de estado refleja en el mapa sin recargar. |

---

### 🆕 E1-H11 · Límite de reservas simultáneas por estudiante
**Sprint:** 1 (extiende E1-H8) · **Prioridad:** Alta · **Tamaño:** M

**Descripción:** El sistema debe impedir que un estudiante tenga más reservas activas de las permitidas o que una reserva supere la duración máxima configurada. Ambos parámetros deben ser configurables por el administrador.

| Área | Tarea |
|------|-------|
| **Front-End** | Panel de configuración en el área de administración: campos numéricos para "Máximo de reservas simultáneas" y "Duración máxima por reserva (minutos)". Mensaje de error claro en el formulario de reserva si el estudiante alcanzó su límite. |
| **Back-End** | Al ejecutar `POST /bookings`: consultar cuántas reservas activas tiene el usuario → si supera el límite configurado, responder `400` con mensaje explicativo. Validar también que `hora_fin - hora_ini <= duracion_maxima`. `GET /admin/config`: devuelve configuración actual. `PUT /admin/config`: actualiza los límites. |
| **Tablas BD** | Nueva tabla `system_config(id, clave, valor, updated_at)`. Claves iniciales: `max_reservas_simultaneas` (default: 1), `duracion_maxima_minutos` (default: 120). |
| **Pruebas** | Intentar 2ª reserva superando el límite → error correcto. Intentar reserva de 3h con límite de 2h → error correcto. Admin cambia límite a 2 → estudiante puede hacer 2 reservas simultáneas. |

---

## Épica 2 – Gestionar Perfil

---

### E2-H6 · Recuperar contraseña
**Sprint:** 1 · **Prioridad:** Alta · **Tamaño:** S

| Área | Tarea |
|------|-------|
| **Front-End** | Vista "Olvidé mi contraseña" y vista "Nueva contraseña". |
| **Back-End** | `POST /auth/reset-request`: recibe correo, guarda token temporal y envía enlace. `POST /auth/reset-confirm`: recibe token y nueva contraseña, la reemplaza. |
| **Tablas BD** | `password_resets(user_id, token, expira_at)`. Índice por `token`. |
| **Pruebas** | Pedir reset con correo válido/inválido; confirmar con token válido/expirado. |

---

### E2-H1 · Ver todos los perfiles registrados
**Sprint:** 2 · **Prioridad:** Media · **Tamaño:** M

| Área | Tarea |
|------|-------|
| **Front-End** | Pantalla "Usuarios" con tabla paginada (10 por página). Columnas: id, código, correo, nombres, apellidos, estado. Botón toggle Habilitar/Deshabilitar por fila. |
| **Back-End** | `GET /admin/users`: lista usuarios paginados. `PATCH /admin/users/{id}/estado`: cambia estado del usuario. |
| **Tablas BD** | `users.estado {Habilitado, Deshabilitado}`. Índices por `codigo` y `email`. |
| **Pruebas** | Listar usuarios; cambiar estado y reflejarlo en pantalla inmediatamente. |

---

### E2-H5 · Filtrar usuarios por código o correo
**Sprint:** 3 · **Prioridad:** Baja · **Tamaño:** S

| Área | Tarea |
|------|-------|
| **Front-End** | Barra de búsqueda en pantalla "Usuarios". Filtra en tiempo real o al presionar enter. |
| **Back-End** | `GET /admin/users?query=texto`: busca por código o correo sin distinguir mayúsculas/minúsculas (`ILIKE`). Prioriza coincidencia exacta sobre parcial. |
| **Tablas BD** | Índices existentes por `codigo` y `email` son suficientes. |
| **Pruebas** | Buscar exacto y parcial; case-insensitive; rendimiento correcto con 100+ usuarios. |

---

### E2-H2 · Actualizar información personal
**Sprint:** 5 · **Prioridad:** Media · **Tamaño:** S

| Área | Tarea |
|------|-------|
| **Front-End** | Pantalla "Mi perfil" con campos editables: nombre, apellido, correo. Botón "Guardar cambios". |
| **Back-End** | `PATCH /users/{id}`: actualiza nombre, apellido o correo del usuario. |
| **Tablas BD** | Reutiliza `users(id, nombre, apellido, email)`. |
| **Pruebas** | Actualizar campos y verificar mensaje de confirmación `"Se guardaron correctamente los cambios"`. |

---

### E2-H4 · Notificación si la cuenta es modificada
**Sprint:** 5 · **Prioridad:** Baja · **Tamaño:** S

| Área | Tarea |
|------|-------|
| **Front-End** | Sin UI nueva. El backend envía la notificación automáticamente. |
| **Back-End** | Tras `PATCH /users/{id}` exitoso: encolar correo `"Modificaciones realizadas en el perfil"` con detalle de los cambios. |
| **Tablas BD** | Reutiliza `email_outbox`. |
| **Pruebas** | Modificar perfil y verificar correo con los detalles del cambio. |

---

## Épica 3 – Gestionar Notificaciones

---

### E3-H1 · Notificación de confirmación de reserva
**Sprint:** 6 · **Prioridad:** Alta · **Tamaño:** S

| Área | Tarea |
|------|-------|
| **Front-End** | Pantalla de confirmación de reserva exitosa. |
| **Back-End** | `POST /notifications/booking-confirmation`: envía correo con ID, sala, fecha y hora. |
| **Tablas BD** | Reutiliza `email_outbox`. |
| **Pruebas** | Al realizar reserva válida, llega correo con la información completa. |

---

### E3-H2 · Recordatorio automático previo a la reserva
**Sprint:** 3 · **Prioridad:** Media · **Tamaño:** S

| Área | Tarea |
|------|-------|
| **Front-End** | Sin UI nueva. En "Mis reservas" puede mostrarse etiqueta "Próxima reserva". |
| **Back-End** | Job en segundo plano: cada N minutos busca reservas que empiecen dentro de 60 minutos y envía correo recordatorio. No enviar si la reserva fue cancelada. |
| **Tablas BD** | `email_outbox`. `jobs_schedule(id, tipo, run_at, payload)`. |
| **Pruebas** | Recordatorio llega a T-60 min; no llega si la reserva fue cancelada previamente. |

---

### E3-H3 · Notificación de modificación de reserva
**Sprint:** 3 · **Prioridad:** Media · **Tamaño:** M

| Área | Tarea |
|------|-------|
| **Front-End** | Toast "Cambios enviados al correo" tras guardar la modificación. |
| **Back-End** | Al ejecutar `PUT /bookings/{id}`: enviar correo con datos anteriores y nuevos de la reserva. Registrar en `audit_log`. |
| **Tablas BD** | `email_outbox`. `audit_log(id, actor, accion, entidad, entidad_id, fecha)`. |
| **Pruebas** | Editar reserva y verificar correo con cambios; verificar registro en `audit_log`. |

---

### E3-H5 · Notificación de cancelación por administrador
**Sprint:** 3 · **Prioridad:** Media · **Tamaño:** M

| Área | Tarea |
|------|-------|
| **Front-End** | Etiqueta "Cancelada" visible en la lista de reservas del estudiante. |
| **Back-End** | Al cancelar reserva: enviar correo automático con sala, fecha y hora cancelada, e indicar que puede hacer una nueva reserva. |
| **Tablas BD** | Reutiliza `email_outbox`. |
| **Pruebas** | Admin cancela reserva → estudiante recibe correo de cancelación con la información correcta. |

---

### E3-H7 · Notificación de mantenimiento de sala
**Sprint:** 4 · **Prioridad:** Baja · **Tamaño:** S

| Área | Tarea |
|------|-------|
| **Front-End** | Pantalla para crear/gestionar periodos de mantenimiento por sala. |
| **Back-End** | `POST /rooms/{id}/unavailability`: crea periodo de no disponibilidad. `GET /rooms/{id}/unavailability`: lista esos periodos. Al reservar, el backend verifica y bloquea el horario; envía correo a estudiantes afectados. |
| **Tablas BD** | `rooms_unavailability(id, room_id, ini, fin, motivo)`. Índice `(room_id, ini)`. |
| **Pruebas** | No permitir reservas en periodos de mantenimiento; correo enviado al crear el mantenimiento. |

---

### E3-H8 · Configurar preferencias de notificación
**Sprint:** 4 · **Prioridad:** Baja · **Tamaño:** M

| Área | Tarea |
|------|-------|
| **Front-End** | Página "Preferencias" con interruptores (toggles) para cada tipo de notificación (correo / en-app). |
| **Back-End** | `GET /me/preferences`: devuelve preferencias actuales. `PUT /me/preferences`: guarda si quiere correos o no. Respetar preferencias al encolar `email_outbox`. |
| **Tablas BD** | `user_preferences(user_id, email_notifications, in_app_notifications)`. |
| **Pruebas** | Guardar preferencia "sin correo" y verificar que no llegan correos; reflejo correcto al recargar. |

---

### E3-H4 · Notificación cuando una sala se libera
**Sprint:** 6 · **Prioridad:** Baja · **Tamaño:** S

| Área | Tarea |
|------|-------|
| **Front-End** | Sin UI nueva. Backend envía notificación automáticamente. |
| **Back-End** | Al cancelar una reserva: marcar sala c?mo disponible y enviar correo a estudiantes en lista de espera (si existe) o a los suscritos a esa sala. |
| **Tablas BD** | Reutiliza `email_outbox`. Posible tabla `sala_suscripciones(user_id, room_id)` para lista de espera. |
| **Pruebas** | Cancelar reserva → notificación de sala libre llega a los interesados. |

---

### E3-H6 · Recordatorio de tiempo restante
**Sprint:** 6 · **Prioridad:** Baja · **Tamaño:** S

| Área | Tarea |
|------|-------|
| **Front-End** | Mostrar tiempo restante en la tarjeta de reserva activa en el dashboard del estudiante. |
| **Back-End** | Job que detecta reservas activas con menos de 15 minutos restantes (umbral configurable) y envía correo/notificación con sala, hora de finalización y minutos restantes. |
| **Tablas BD** | Reutiliza `email_outbox` y `jobs_schedule`. |
| **Pruebas** | Reserva con 15 min restantes → llega recordatorio. Umbral configurable respetado. |

---

### E3-H9 · Notificación sobre nuevas salas o cambios de política
**Sprint:** 6 · **Prioridad:** Baja · **Tamaño:** S

| Área | Tarea |
|------|-------|
| **Front-End** | Pantalla de configuración de política institucional para el administrador. |
| **Back-End** | `POST /notifications/update-policy`: envía notificación masiva a todos los estudiantes con el detalle del cambio (nueva sala o nueva política). |
| **Tablas BD** | Reutiliza `email_outbox`. |
| **Pruebas** | Admin publica nueva política → todos los estudiantes reciben el correo con el detalle. |

---

### E3-H10 · Notificación por inasistencia
**Sprint:** 6 · **Prioridad:** Baja · **Tamaño:** S

| Área | Tarea |
|------|-------|
| **Front-End** | Sin UI nueva. Administrador puede ver historial de inasistencias en el perfil del estudiante. |
| **Back-End** | Job que, pasada la hora de inicio de una reserva sin registro de asistencia, la marca c?mo inasistencia, registra el evento en el historial del estudiante y envía correo de aviso. |
| **Tablas BD** | Agregar `bookings.asistio {null, true, false}`. `historial_inasistencias(id, user_id, booking_id, fecha)`. |
| **Pruebas** | Reserva sin asistencia → correo enviado; inasistencia registrada en historial. |

---

### 🆕 E3-H11 · Integración con calendario externo (.ics / Google Calendar)
**Sprint:** 3 · **Prioridad:** Media · **Tamaño:** M

**Descripción:** Al confirmar una reserva, el estudiante recibe en el correo de confirmación un archivo `.ics` adjunto y un botón/enlace para agregar el evento directamente a Google Calendar o Apple Calendar.

| Área | Tarea |
|------|-------|
| **Front-End** | En la vista de detalle de reserva y en el correo: botón "Agregar a Google Calendar" (enlace `calendar.google.com/calendar/r/eventedit?...`) y botón "Descargar .ics". |
| **Back-End** | Al confirmar reserva: generar archivo `.ics` usando la librería `ical-generator` (Node.js). Adjuntar el `.ics` al correo de confirmación (`email_outbox.attachments`). Endpoint `GET /bookings/{id}/ics` para descargar el `.ics` en cualquier momento posterior. Los campos del evento deben incluir: título (`"Reserva – [nombre sala]"`), fecha inicio, fecha fin, descripción, ubicación. |
| **Tablas BD** | Sin tablas nuevas. `email_outbox` agrega campo `attachments (jsonb)` para guardar referencia al archivo `.ics`. |
| **Pruebas** | Al reservar, llega correo con `.ics` adjunto válido. El enlace de Google Calendar precarga los datos correctos. Descargar `.ics` desde el endpoint importa correctamente en Apple Calendar. |

---

## Épica 4 – Gestionar Seguridad

---

### E4-H1 · Autenticación de usuario
**Sprint:** 1 · **Prioridad:** Alta · **Tamaño:** M

| Área | Tarea |
|------|-------|
| **Front-End** | Pantallas Login y Home diferenciadas por rol (estudiante / admin). Validación de campos en cliente. |
| **Back-End** | `POST /auth/login`: recibe email y contraseña, compara hash bcrypt, devuelve JWT. `GET /auth/me`: con token devuelve datos básicos del usuario. Si el rol no tiene permisos, responder 403. |
| **Tablas BD** | `users(id, email, pass_hash, role, locked_until)`. `roles` (seed con Administrador y Estudiante). Índice único por `email`. |
| **Pruebas** | Login correcto (alumno/admin); error 401 con credenciales incorrectas; 403 si no tiene permisos. |

---

### E4-H3 · Autenticación de dos factores (2FA)
**Sprint:** 4 · **Prioridad:** Baja · **Tamaño:** M

| Área | Tarea |
|------|-------|
| **Front-End** | UI para ingresar código de 2FA tras el login. Sección en "Configuración de Seguridad" para activar/desactivar. |
| **Back-End** | `POST /auth/2fa/enroll`: envía código al correo para activar. `POST /auth/2fa/verify`: confirma el código. `POST /auth/2fa/disable`: desactiva. En el login, si el usuario tiene 2FA activo, el token inicial es provisional y se exige el código. |
| **Tablas BD** | `two_factor_codes(user_id, code, expira_at)`. `users.has_2fa (boolean)`. |
| **Pruebas** | Activar 2FA; login con código válido/inválido/expirado; desactivar y confirmar que el login vuelve a ser directo. |

---

### E4-H6 · Notificación de acceso no autorizado
**Sprint:** 5 · **Prioridad:** Baja · **Tamaño:** S

| Área | Tarea |
|------|-------|
| **Front-End** | Sin UI nueva. Mostrar aviso en pantalla de login tras el bloqueo ("Cuenta bloqueada temporalmente"). |
| **Back-End** | Cada intento fallido incrementa contador en `security_log`. Al 5.º intento: bloquear cuenta (`users.locked_until = now() + 15 min`) y enviar correo `"Alguien intentó ingresar a tu cuenta. Si no fuiste tú, repórtalo"`. |
| **Tablas BD** | `security_log(id, user_id, ip, evento, fecha)`. `users.locked_until (timestamp)`. |
| **Pruebas** | 5 intentos fallidos → cuenta bloqueada y correo enviado. Tras `locked_until` expirado, login vuelve a funcionar. |

---

### E4-H8 · Buenas prácticas de seguridad de datos
**Sprint:** 4 · **Prioridad:** Baja · **Tamaño:** S

| Área | Tarea |
|------|-------|
| **Front-End** | Sin cambios visibles. Página "About" o "Seguridad" puede listar las prácticas implementadas. |
| **Back-End** | Activar cabeceras de seguridad (CSP, STS, X-Frame-Options). Forzar HTTPS. Contraseñas con bcrypt (factor 12). Restringir orígenes con CORS. Sanitizar inputs contra SQL injection y XSS. |
| **Tablas BD** | Sin tablas nuevas. Solo configuración del servidor. |
| **Pruebas** | Revisar cabeceras HTTP con herramienta; verificar que contraseñas estén cifradas en BD; prueba de carga básica con al menos 95% de disponibilidad. |

---

## 🆕 Adicional - Experiencia de Usuario (UX)

> Funcionalidades transversales que mejoran la experiencia en toda la aplicación.

---

### 🆕 E5-H1 · Dashboard administrativo con métricas de uso
**Sprint:** 6 · **Prioridad:** Media · **Tamaño:** L

**Descripción:** Panel con gráficos interactivos (Recharts) que permita al administrador entender cómo se está usando el sistema.

| Área | Tarea |
|------|-------|
| **Front-End** | Página "Dashboard" accesible solo para administradores. Componentes: gráfico de barras para tasa de ocupación por sala; gráfico de líneas para horas pico; tabla ranking de estudiantes con más reservas; tarjeta de tasa de inasistencia global. Filtros por rango de fechas y por pabellón. Usar `Recharts`. |
| **Back-End** | `GET /admin/analytics/occupancy`: tasa de ocupación `(minutos_reservados / minutos_disponibles * 100)` por sala. `GET /admin/analytics/peak-hours`: distribución de reservas por hora del día (0–23). `GET /admin/analytics/top-students?limit=10`: ranking de estudiantes por número de reservas. `GET /admin/analytics/absence-rate`: `(inasistencias / reservas_totales * 100)`. Todos los endpoints soportan `?desde=&hasta=`. |
| **Tablas BD** | Reutiliza `bookings`, `historial_inasistencias`, `rooms`. Considerar una vista materializada `mv_daily_stats` para no recalcular en cada petición. |
| **Pruebas** | Con datos de prueba, verificar cálculos correctos. Filtro por fecha restringe correctamente el resultado. Acceso denegado a estudiantes (403). |

---

### 🆕 E5-H2 · Modo oscuro consistente en toda la aplicación
**Sprint:** 4 · **Prioridad:** Baja · **Tamaño:** M

**Descripción:** Implementar un modo oscuro que aplique de forma coherente en todas las vistas de la aplicación (no solo en algunos componentes), persista la preferencia del usuario y respete la preferencia del sistema operativo.

| Área | Tarea |
|------|-------|
| **Front-End** | Toggle visible en la barra de navegación (ícono sol/luna). Implementar con CSS custom properties (`--bg-primary`, `--text-primary`, etc.) y la clase `.dark` en el `<html>`. Usar `localStorage` para persistir la preferencia entre sesiones. Detectar `prefers-color-scheme: dark` del sistema operativo al primer acceso. Auditar **todas** las vistas y componentes para que usen las variables de color (no colores hardcodeados). Modo oscuro debe cubrir: navbar, sidebar, tablas, modales, toasts, formularios, mapas del campus, gráficos del dashboard. |
| **Back-End** | Sin cambios. La preferencia se guarda en `localStorage` del navegador. (Opcional: persistir en `user_preferences.dark_mode` para sincronizar entre dispositivos.) |
| **Tablas BD** | Opcional: `user_preferences.dark_mode (boolean)`. |
| **Pruebas** | Toggle activa modo oscuro en todas las vistas sin excepción. Preferencia persiste al recargar. Si el SO está en modo oscuro y el usuario nunca cambió el toggle, la app inicia en oscuro. Contrastar que no haya texto ilegible ni elementos "quemados" en ninguna pantalla. |
