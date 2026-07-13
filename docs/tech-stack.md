# Stack tecnológico — Luistudio

## Tecnologías verificadas

| Capa | Tecnología actual |
|---|---|
| Frontend | React 19, TypeScript 6 y Vite 8 |
| UI y estilos | Tailwind CSS 3, CSS propio y variables semánticas |
| Mapa | MapLibre GL; teselas de MapTiler configurables |
| Dashboard | SVG y CSS nativos, sin Recharts ni shadcn/ui |
| Pruebas frontend | Vitest, Testing Library y jsdom |
| Gestor frontend | pnpm 10.13.1, fijado en `packageManager` y `pnpm-lock.yaml` |
| Backend | Spring Boot, Java 21 y Maven Wrapper |
| Persistencia | Spring Data JPA, Hibernate y PostgreSQL; H2 para pruebas/contexto local |
| Autenticación | JWT firmado, JTI persistido, cookies HttpOnly y bcrypt |
| Correos | Outbox persistente con adaptadores Log, Resend y Gmail |
| Cache | Redis opcional para el mapa, con fallback a PostgreSQL |
| Calendario | Exportación `.ics` y enlaces de Google Calendar |
| Cobertura Java | JaCoCo |
| Contenedores | Docker y Docker Compose |

## Arquitectura ejecutable

```text
Navegador
  → React/Vite
  → REST /api (Spring Boot)
  → PostgreSQL
  ↘ Redis opcional para cache del mapa
  ↘ email_outbox → proveedor de correo configurado
```

Los puntos de entrada son `frontend/luistudio-app/src/main.tsx` y
`backend/reservas/src/main/java/com/luistudio/reservas/ReservasApplication.java`.

## Comandos

```bash
cd frontend/luistudio-app
pnpm install --frozen-lockfile
pnpm test
pnpm run build
```

```bash
cd backend/reservas
./mvnw test
```
