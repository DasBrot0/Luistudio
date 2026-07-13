# Frontend - Luistudio (Release 01)

## Stack

- React + TypeScript + Vite
- Tailwind CSS
- Node.js 22 + pnpm 10.13.1

## Ejecución local

1. Instala dependencias:

```bash
corepack enable
pnpm install --frozen-lockfile
```

2. Copia `.env.example` como `.env` y conserva el proxy local por defecto:

```bash
VITE_API_BASE_URL=/api
VITE_PROXY_TARGET=http://localhost:8080
```

Nota: si defines `VITE_API_BASE_URL` sin sufijo `/api`, la app lo agrega automáticamente.

3. Ejecuta (o desde la raíz: `./scripts/start-frontend.ps1`):

```bash
pnpm run dev
```

## Desarrollo con Docker

El `docker-compose.yml` está configurado para desarrollo con recarga automática:

```bash
docker compose up
```

- Frontend: `http://localhost:5173` (Vite aplica HMR al guardar cambios; usa polling para funcionar también con volúmenes de Docker Desktop en Windows).
- Backend: `http://localhost:8080` (un watcher recompila al detectar cambios y Spring Boot DevTools reinicia la aplicación).
- El frontend redirige `/api` al backend dentro de Docker y a `localhost:8080` cuando corre directamente en tu equipo.

El código se monta como volumen, por lo que no es necesario reconstruir ni reiniciar los contenedores después de modificar archivos. La primera ejecución instala dependencias de Node y Maven en volúmenes de Docker.

## Pruebas

```bash
pnpm test
```

El frontend usa exclusivamente pnpm. El campo `packageManager` y `pnpm-lock.yaml` fijan la versión del gestor y las dependencias usadas también durante el despliegue.

## Funcionalidades conectadas a backend

- Login con roles (admin/estudiante) y flujo 2FA.
- Login responsive con panel contextual institucional en escritorio, formulario compacto en móvil, errores accesibles y credenciales de demostración plegables.
- Los textos de marca usan el token semántico `--brand-text`, con una variante clara en modo oscuro independiente del morado empleado en botones y fondos; se aplica también a links, filtros, dashboard, mapa, configuración y perfil.
- Restauración de sesión con pantalla de carga breve para evitar el flash del login al recargar una ruta protegida.
- Tras login, la app navega despues de cargar preferencias y deja la data de la pantalla inicial en background para reducir latencia percibida.
- Verificación 2FA mediante modal propio (sin depender de `window.prompt`).
- Solicitud y confirmación de restablecimiento de contraseña.
- Reserva, edición y cancelación de reservas.
- En listados de reservas, el horario se muestra sin segundos (`HH:mm-HH:mm`) y no se permite cancelar reservas ya finalizadas.
- Reserva con cuadrícula semanal interactiva (semana anterior, actual y siguiente) y selección visual por bloque horario.
- En reserva, los campos inician vacíos y `Inicio/Fin` se sincronizan con la grilla y reglas de sala/campus (sin seleccionar horas inválidas manualmente).
- Fuera de la ventana permitida (semana actual y solo horas futuras; el fin de semana habilita también la semana siguiente) los espacios quedan en blanco y no clickeables.
- En `Personas de la reserva`, se autoincluye el usuario autenticado como persona 1 fija y se gestionan acompañantes agregando por código con validación inmediata contra backend.
- `Editar reserva` usa el mismo esquema moderno de `Reservar` (Recurso, Fecha y hora, Personas de la reserva), sin campo legacy de número de personas.
- `Nueva reserva` usa un layout por bloques (`Recurso`, `Fecha y hora`, `Personas de la reserva`) con proporciones desktop (campus más angosto que ubicación/recurso) y adaptación responsive para móvil.
- `Mis reservas` incluye alternancia de vista compacta/detallada (con campus, recinto y ubicación) sin scroll horizontal en desktop ni móvil.
- `Mis reservas` permite exportar reservas confirmadas del estudiante como archivo `.ics` o abrir Google Calendar con el evento precargado.
- Salas, Perfiles y Reservas registradas ajustan filtros y acciones para reducir altura en desktop, mejorar lectura en móvil y mostrar estados vacíos más claros.
- Vistas separadas por componente para desktop/móvil en listados principales (Mis reservas, Reservas registradas, Salas y Perfiles), evitando mezcla de estilos al redimensionar.
- CRUD de salas (admin).
- Edición completa de sala (campus, ubicación, recurso, capacidad, min/max personas y horario semanal por sala).
- Los guardados de salas y horarios normalizan días cerrados con horas `null` y validan datos básicos antes de llamar al backend.
- Filtro por botón, búsqueda por código/correo/nombres/apellidos, ordenamiento y cambio de estado de perfiles (admin).
- Configuración de límites de reservas (admin).
- Configuración de horario general por campus (admin), con advertencias si entra en conflicto con overrides de salas.
- Configuración de duración por reserva por campus (30/45/60/120 min); las horas de apertura/cierre del horario general se validan contra ese múltiplo.
- Navegación por rol y centro de notificaciones en UI.
- Modo oscuro con persistencia (`localStorage`) y paleta tonal completa para cards, modales, tablas, botones, textos y selectores.
- Favicon configurado con el icono de Luistudio desde `public/favicon.svg`.
- Tema inicial automático según modo del sistema (si no existe preferencia guardada), con persistencia en `localStorage` y sincronización en backend por usuario autenticado.
- Configuración de accesibilidad visual en cliente: escala de texto global persistente (`luistudio_font_scale` en `localStorage`).
- Sincronización de tema y escala de texto con backend en `GET/PUT /api/me/preferences` para usuarios autenticados.
- El autoguardado de preferencias usa debounce de 300 ms y una firma interna sin re-render para evitar cancelar el guardado pendiente.
- Configuración de vista inicial de sesión por rol desde el modal de Configuración (estudiante: Mis reservas/Reservar; admin: Salas/Reservas).
- Configuración incluye subsección Notificaciones con tipos por rol y switches independientes para canal App y Email, persistidos por usuario.
- Mensajes de error/confirmación en modales integrados con la UI (sin `alert`/mensajes legacy).

Notas recientes:

- La jerarquía y los controles visuales toman `Estudiante > Reservar` como referencia: títulos de página, títulos de tarjeta, subtítulos, campos y acciones usan tamaños, alturas, radios, foco y estados deshabilitados compartidos.
- Los filtros mantienen sus desplegables en una cuadrícula estable, con la ordenación en una fila secundaria; los placeholders usan color semántico y los ejemplos textuales comienzan con `Ej.:`.
- Los campos de texto multilínea usan los mismos tokens de color, borde y foco que los demás controles, incluido el modo oscuro.
- Las acciones principales comparten las clases utilitarias de Confirmar reserva: azul sólido, forma pill, íconos semánticos y estados hover/deshabilitado consistentes.
- La búsqueda inteligente de salas está disponible como opción independiente del menú estudiantil; al elegir una recomendación, precarga la sala en Reservar.
- Su formulario usa una presentación simplificada: título con la misma tarjeta y cabecera de Reservas, descripción y un único panel con necesidad, fecha y horario.
- `FilterBar` reutilizable para Salas, Reservas registradas y Perfiles, con comportamiento consistente entre modulos.
- Salas consume `GET /api/rooms` paginado y por defecto sin horarios completos (`includeSchedule=false`) para acelerar listados.
- Reservas registradas consume paginación real del backend (`size=10`) y usa `totalPages` de la API.
- Perfiles muestra apellidos completos, distingue cuentas bloqueadas temporalmente y permite desbloqueo manual.
- Cancelar una reserva ahora solicita confirmación previa mostrando el detalle y el aviso de notificación automática.

## Capa API

- Archivo principal: `src/services/api.ts`
- La sesión autenticada se consume por cookie `HttpOnly`; el frontend no persiste tokens de acceso en `localStorage`.
- Los `GET` no envian `Content-Type` cuando no hay body, evitando preflight innecesario con cookies `HttpOnly`.
- Solo se guarda una marca no sensible para restaurar sesión y decidir si mostrar la pantalla breve de carga al recargar.

## Deploy en Vercel (SPA)

- Este frontend usa React Router, por lo que rutas como `/reservas` o `/salas` deben reescribirse a `index.html` en producción.
- El archivo `frontend/luistudio-app/vercel.json` ya incluye el rewrite global para evitar `404 NOT_FOUND` al recargar con `F5`.

- Notificaciones de reserva: ahora incluyen detalle legible de recurso, ubicación, fecha y horario.
- Para evitar duplicados visuales, el cliente conserva solo la última acción por reserva lógica (usuario + recurso + ubicación + fecha + horario), ignorando diferencias de cantidad de personas.
- Configuración incluye switch de 2FA por usuario con confirmación por código enviado al correo (activar y desactivar).
- Al presionar `Activar 2FA` o `Desactivar 2FA` desde Configuración, se cierra ese modal y se abre el modal de confirmación de 2FA.
- El modal de salas permite marcar una sala como disponible, en mantenimiento o inactiva; los bloqueos por reserva activa se muestran en modal.
- Configuración permite elegir Salas, Perfiles o Reservas como vista inicial para administradores.
- La sección Notificaciones muestra reservas para estudiantes y mantenimiento/perfiles para administradores; desactivar App evita el toast/centro local y desactivar Email se sincroniza con backend.
## Dashboard y búsqueda en lenguaje natural

- `/admin/dashboard` es la primera opción de la navegación administrativa y la vista inicial predeterminada.
- El dashboard permite filtrar y restablecer fechas; presenta cuatro KPI, tendencia diaria, dona de asistencia, mapa de calor semanal, barras horizontales por sala y ranking buscable/paginado. También exporta el ranking en CSV y muestra la hora de la última actualización.
- La opción **Búsqueda inteligente** acepta una descripción libre, fecha y horario. La respuesta muestra la intención interpretada y hasta 3 recomendaciones ordenadas por puntuación cuando existen salas compatibles.
- Elegir una recomendación precarga campus, recinto, sala, fecha y horario en el formulario normal; la confirmación sigue usando todas las validaciones existentes.
- Dashboard, mapa, navegación móvil, perfil y sesiones usan tokens compartidos de superficie, borde, texto, éxito y peligro para mantener contraste en temas claro y oscuro.
- Todos los listados tabulares muestran un máximo de 10 elementos por página y reutilizan `Pagination`: acciones anterior/siguiente en los extremos y el indicador `Página X de Y` centrado en el ancho total. Salas, comunicados, reservas del estudiante, avisos y métricas usan paginación local; reservas administrativas, perfiles y seguridad conservan paginación de backend.

# Mapa de campus E1-H10

La ruta autenticada `/mapa` usa MapLibre y MapTiler, con fallback a una lista accesible. Configure en Vercel las variables de `.env.example` y restrinja la clave MapTiler por dominio/referrer. El mapa se consulta una sola vez al abrir la vista; no realiza sondeos automáticos. El selector conserva todos los campus cargados y centra el mapa al elegir uno. Los edificios habilitados incluyen coordenadas iniciales calibrables; use el control **Centrar mapa**, a la izquierda del botón de información, para volver al centro del campus y su zoom predeterminado. En calibración, arrastrar solo prepara/copia coordenadas; **Guardar ubicación** es la única acción que persiste.

## Perfil, sesiones e inasistencias

- Mi perfil muestra código, nombres, apellidos, correo, rol y estado en modo de solo lectura.
- Actividad separa visualmente IP, dispositivo o navegador cuando están presentes en el evento.
- Configuración > Sesiones activas permite cerrar la sesión actual, revocar una sesión remota o cerrar todas. Seguridad queda reservada a la configuración de 2FA.
- Seguridad administrativa permite combinar filtros por usuario, correo, resultado, bloqueo vigente y fechas.
- Mis reservas incluye una tarjeta de historial de inasistencias. Si no existe una regla de impedimento registrada, lo indica sin inventar una fecha de sanción.
