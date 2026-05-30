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
- Solicitud y confirmacion de restablecimiento de contrasena.
- Reserva, edicion y cancelacion de reservas.
- CRUD de salas (admin).
- Filtro y cambio de estado de perfiles (admin).
- Configuracion de limites de reservas (admin).
- Navegacion por rol y centro de notificaciones en UI.
- Modo oscuro con persistencia (`localStorage`) y paleta tonal completa para cards, modales, tablas, botones, textos y selectores.
- Configuracion de accesibilidad visual en cliente: escala de texto global persistente (`luistudio_font_scale` en `localStorage`).
- Sincronizacion de tema y escala de texto con backend en `GET/PUT /api/me/preferences` para usuarios autenticados.

## Capa API

- Archivo principal: `src/services/api.ts`
- Token JWT en `localStorage` con clave `luistudio_token`.
