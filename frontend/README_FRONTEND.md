# Frontend - Luistudio (Release 01)

## Stack

- React + TypeScript + Vite
- Tailwind CSS

## Ejecucion local

1. Instala dependencias:

```bash
npm install
```

2. Configura variable de entorno (opcional):

```bash
VITE_API_BASE_URL=http://localhost:8080/api
```

3. Ejecuta:

```bash
npm run dev
```

## Funcionalidades conectadas a backend

- Login con roles (admin/estudiante) y flujo 2FA.
- Verificacion 2FA mediante modal propio (sin depender de `window.prompt`).
- Solicitud y confirmación de restablecimiento de contraseña.
- Reserva, edicion y cancelacion de reservas.
- En listados de reservas, el horario se muestra sin segundos (`HH:mm-HH:mm`) y no se permite cancelar reservas ya finalizadas.
- Reserva con cuadrícula semanal interactiva (semana anterior, actual y siguiente) y selección visual por bloque horario.
- En reserva, los campos inician vacios y `Inicio/Fin` se sincronizan con la grilla y reglas de sala/campus (sin seleccionar horas invalidas manualmente).
- Fuera de la ventana permitida (semana actual y solo horas futuras; el fin de semana habilita tambien la semana siguiente) los espacios quedan en blanco y no clickeables.
- CRUD de salas (admin).
- Edicion completa de sala (campus, ubicacion, recurso, capacidad, min/max personas y horario semanal por sala).
- Filtro y cambio de estado de perfiles (admin).
- Configuracion de limites de reservas (admin).
- Configuracion de horario general por campus (admin), con advertencias si entra en conflicto con overrides de salas.
- Configuracion de duracion por bloque de reserva por campus (30/45/60/120 min).
- Navegación por rol y centro de notificaciones en UI.
- Modo oscuro con persistencia (`localStorage`) y paleta tonal completa para cards, modales, tablas, botones, textos y selectores.
- Configuracion de accesibilidad visual en cliente: escala de texto global persistente (`luistudio_font_scale` en `localStorage`).
- Sincronizacion de tema y escala de texto con backend en `GET/PUT /api/me/preferences` para usuarios autenticados.
- Mensajes de error/confirmación en modales integrados con la UI (sin `alert`/mensajes legacy).

## Capa API

- Archivo principal: `src/services/api.ts`
- Token JWT en `localStorage` con clave `luistudio_token`.
