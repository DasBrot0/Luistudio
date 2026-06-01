# Frontend - Luistudio (Release 01)

## Stack

- React + TypeScript + Vite
- Tailwind CSS

## Ejecución local

1. Instala dependencias:

```bash
npm install
```

2. Configura variable de entorno (opcional):

```bash
VITE_API_BASE_URL=http://localhost:8080/api
```

Nota: si defines `VITE_API_BASE_URL` sin sufijo `/api`, la app lo agrega automÃ¡ticamente.

3. Ejecuta:

```bash
npm run dev
```

## Funcionalidades conectadas a backend

- Login con roles (admin/estudiante) y flujo 2FA.
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
- Vistas separadas por componente para desktop/móvil en listados principales (Mis reservas, Reservas registradas, Salas y Perfiles), evitando mezcla de estilos al redimensionar.
- CRUD de salas (admin).
- Edición completa de sala (campus, ubicación, recurso, capacidad, min/max personas y horario semanal por sala).
- Filtro y cambio de estado de perfiles (admin).
- Configuración de límites de reservas (admin).
- Configuración de horario general por campus (admin), con advertencias si entra en conflicto con overrides de salas.
- Configuración de duración por bloque de reserva por campus (30/45/60/120 min).
- Navegación por rol y centro de notificaciones en UI.
- Modo oscuro con persistencia (`localStorage`) y paleta tonal completa para cards, modales, tablas, botones, textos y selectores.
- Tema inicial automático según modo del sistema (si no existe preferencia guardada), con persistencia en `localStorage` y sincronización en backend por usuario autenticado.
- Configuración de accesibilidad visual en cliente: escala de texto global persistente (`luistudio_font_scale` en `localStorage`).
- Sincronización de tema y escala de texto con backend en `GET/PUT /api/me/preferences` para usuarios autenticados.
- Configuración de vista inicial de sesión por rol desde el modal de Configuración (estudiante: Mis reservas/Reservar; admin: Salas/Reservas).
- Mensajes de error/confirmación en modales integrados con la UI (sin `alert`/mensajes legacy).

## Capa API

- Archivo principal: `src/services/api.ts`
- Token JWT en `localStorage` con clave `luistudio_token`.

## Deploy en Vercel (SPA)

- Este frontend usa React Router, por lo que rutas como `/reservas` o `/salas` deben reescribirse a `index.html` en produccion.
- El archivo `frontend/luistudio-app/vercel.json` ya incluye el rewrite global para evitar `404 NOT_FOUND` al recargar con `F5`.

- Notificaciones de reserva: ahora incluyen detalle legible de recurso, ubicacion, fecha y horario.
- Para evitar duplicados visuales, el cliente conserva solo la ultima accion por reserva logica (usuario + recurso + ubicacion + fecha + horario), ignorando diferencias de cantidad de personas.
- Configuracion incluye switch de 2FA por usuario con confirmacion por codigo enviado al correo (activar y desactivar).
