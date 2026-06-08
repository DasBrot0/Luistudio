# Stack Tecnológico – Luistudio

## Tecnologías por capa

| Capa                      | Tecnología         |
| ------------------------- | ------------------ |
| Frontend                  | React + TypeScript |
| UI / Estilos              | Tailwind CSS       |
| Backend                   | Spring Boot + Java |
| Base de Datos             | PostgreSQL         |
| ORM                       | Spring Data JPA + Hibernate             |
| Autenticación             | JWT + bcrypt       |
| Correos / Notificaciones  | Brevo / Resend API |
| Dashboard                 | Recharts           |
| Exportación de calendario | `.ics`             |
| Control de versiones      | Git + GitHub       |
| Despliegue Frontend       | Vercel             |
| Despliegue Backend        | Render             |
| Hosting Base de Datos     | Supabase           |

---

## Lenguajes utilizados

| Contexto      | Lenguaje   |
| ------------- | ---------- |
| Frontend      | TypeScript |
| Backend       | Java       |
| Base de datos | SQL        |
| Estilos       | CSS        |

---

## Arquitectura general

```
Navegador (PC / Móvil)
        ↓  HTTPS
Frontend React (Vercel)
        ↓  REST API
Backend Spring Boot + Java (Render)
        ↓  TCP/IP :5432
PostgreSQL (Supabase)
        +
Servidor SMTP externo (Resend)
```

---

## Componentes principales

| Componente                    | Archivo    | Rol                        |
| ----------------------------- | ---------- | -------------------------- |
| `AplicacionCliente.tsx`       | Frontend   | Interfaz de usuario React  |
| `SistemaReservasAPI.java`     | Backend    | Exposición de la API REST  |
| `ModuloPersistencia.java`     | Backend    | Acceso a datos             |
| `ModuloAutenticacion.java`    | Backend    | JWT, bcrypt, 2FA           |
| `ServicioNotificaciones.java` | Backend    | Envío de correos y alertas |
| `luistudio_db`                | PostgreSQL | Base de datos principal    |

---

## Estructura del repositorio

```
Luistudio/
├── frontend/        → Aplicación React + TypeScript
├── backend/         → API Spring Boot + Java
├── docs/            → Documentación del proyecto
├── README.md
└── .gitignore
```
