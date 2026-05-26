### Frontend — React + TypeScript + Tailwind

```
src/
├── assets/          # Logos, íconos SVG, imágenes estáticas
│
├── components/      # Componentes reutilizables sin lógica de negocio
│   ├── ui/          # Botones, inputs, modals, badges, tablas genéricas
│   ├── layout/      # Sidebar, Topbar, NotificationBell, PageWrapper
│   └── charts/      # Wrappers de Recharts (ej. ReservasChart)
│
├── pages/           # Vista principal del router
│   ├── auth/        # Login, ForgotPassword
│   ├── reservas/    # Reservar, MisReservas, Calendario
│   ├── salas/       # ListaSalas, DetalleSala
│   ├── admin/
│   │   ├── usuarios/      # GestionUsuarios, Perfiles
│   │   ├── mantenimiento/ # BloquearSala
│   │   ├── seguridad/     # RegistroSeguridad, ConfigSeguridad
│   │   └── auditoria/     # Auditoria
│   └── perfil/      # MiPerfil, Notificaciones, Preferencias2FA
│
├── router/          # Definición de rutas, guards por rol
│
├── hooks/           # useAuth, useReservas, useNotificaciones, etc.
│
├── services/        # Llamadas a la API agrupadas por dominio
│   ├── auth.service.ts
│   ├── reservas.service.ts
│   ├── salas.service.ts
│   └── usuarios.service.ts
│
├── store/           # Estado global (Context o Zustand)
│   ├── auth/        # Usuario actual, rol, token
│   └── notificaciones/
│
├── types/           # Interfaces y tipos TypeScript compartidos
│
└── utils/           # Helpers: formatFecha, exportICS, calcularDuracion
```
