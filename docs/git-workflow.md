# Git Workflow – Luistudio

## Estrategia de ramas

```
main
└── dev
    ├── feature/auth
    ├── feature/bookings
    ├── feature/rooms
    ├── feature/campus-map
    ├── feature/dashboard
    ├── feature/calendar
    ├── feature/security
    └── feature/darkmode
```

### Descripción de ramas

| Rama                  | Propósito |
|-----------------------|-----------|
| `main`                | Código estable en producción. Solo recibe merges desde `dev` tras revisión. |
| `dev`                 | Rama de integración. Todas las features se fusionan aquí antes de pasar a `main`. |
| `feature/auth`        | Login, roles, recuperación de contraseña, 2FA, bloqueo por intentos fallidos. |
| `feature/bookings`    | CRUD de reservas, límites por estudiante, duración máxima. |
| `feature/rooms`       | CRUD de salas, filtros por ubicación, mantenimiento, mapa del campus. |
| `feature/campus-map`  | Vista de disponibilidad tipo mapa con estado en tiempo real. |
| `feature/dashboard`   | Dashboard administrativo con métricas de ocupación e inasistencia. |
| `feature/calendar`    | Exportación `.ics`, integración con Google Calendar. |
| `feature/security`    | Encriptación, HTTPS, roles y permisos, protección de datos, `audit_log`. |
| `feature/darkmode`    | Implementación del modo oscuro en la interfaz. |

## Flujo de trabajo

```
1. Crear rama desde dev:
   git checkout dev && git pull
   git checkout -b feature/nombre-funcionalidad

2. Desarrollar y commitear:
   git add .
   git commit -m "feat: descripción clara del cambio"

3. Push y Pull Request hacia dev:
   git push origin feature/nombre-funcionalidad
   → Abrir PR en GitHub apuntando a dev

4. Code Review por al menos 1 miembro del equipo

5. Merge a dev tras aprobación del PR

6. Al cerrar un sprint: merge de dev → main tras validación del PO
```

## Convenciones de commits

Seguir el estándar [Conventional Commits](https://www.conventionalcommits.org/):

| Prefijo    | Uso |
|------------|-----|
| `feat:`    | Nueva funcionalidad |
| `fix:`     | Corrección de bug |
| `refactor:`| Mejora de código sin cambio de comportamiento |
| `test:`    | Agregar o actualizar pruebas |
| `docs:`    | Actualización de documentación |
| `chore:`   | Tareas de mantenimiento (dependencias, config) |

## Prioridad de desarrollo por etapa

### Etapa 1 (Release 01 – Sprints 1 y 2)

- `feature/auth` → `feature/bookings` → `feature/rooms`

### Etapa 2 (Release 01 – Sprints 3 y 4)

- Correos y notificaciones → `feature/campus-map` → `feature/calendar` → `feature/dashboard`

### Etapa 3 (Release 02 – Sprints 5 y 6)

- `feature/security` → `feature/darkmode` → deploy final a producción
