# Stack Tecnológico – Luistudio

## Tecnologías por capa

| Capa                     | Tecnología           |
|--------------------------|----------------------|
| Frontend                 | React + TypeScript   |
| UI / Estilos             | Bootstrap + CSS      |
| Backend                  | NestJS + TypeScript  |
| Base de Datos            | PostgreSQL           |
| ORM                      | Prisma               |
| Autenticación            | JWT + bcrypt         |
| Correos / Notificaciones | Brevo / Resend API   |
| Dashboard                | Recharts             |
| Exportación de calendario| `.ics`               |
| Control de versiones     | Git + GitHub         |
| Despliegue Frontend      | Vercel               |
| Despliegue Backend       | Render / Railway     |
| Hosting Base de Datos    | Supabase / Neon      |

## Lenguajes utilizados

| Contexto      | Lenguaje   |
|---------------|------------|
| Frontend      | TypeScript |
| Backend       | TypeScript |
| Base de datos | SQL        |
| Estilos       | CSS        |

## Arquitectura general

```
Navegador (PC / Móvil)
        ↓  HTTPS
Frontend React (Vercel)
        ↓  REST API
Backend NestJS + Node.js (Render / Railway)
        ↓  TCP/IP :5432
PostgreSQL (Supabase / Neon)
        +
Servidor SMTP externo (Brevo / Resend)
```

### Componentes principales

| Componente              | Archivo               | Rol |
|-------------------------|-----------------------|-----|
| `AplicacionCliente.tsx` | Frontend              | Interfaz de usuario React |
| `SistemaReservasAPI.ts` | Backend               | Exposición de la API REST |
| `ModuloPersistencia.ts` | Backend               | Acceso a datos vía Prisma |
| `ModuloAutenticacion.ts`| Backend               | JWT, bcrypt, 2FA |
| `ServicioNotificaciones.ts` | Backend           | Envío de correos y alertas |
| `luistudio_db`          | PostgreSQL            | Base de datos principal |

## Estructura del repositorio

```
Luistudio/
├── frontend/        → Aplicación React + TypeScript
├── backend/         → API NestJS + TypeScript
├── docs/            → Documentación del proyecto
├── README.md
└── .gitignore
```
