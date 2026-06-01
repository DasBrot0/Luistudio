# Arquitectura inicial y diseno estructural - Luistudio

Este documento reune el contenido base para redactar las secciones 5 y 6 del informe del proyecto Luistudio. Esta escrito como insumo academico: puede copiarse al informe final y ajustarse segun el formato requerido por el curso.

## 5. Arquitectura inicial y diseno del software

Luistudio se disena como una aplicacion web cliente-servidor para gestionar reservas de salas de estudio. La solucion separa la interfaz de usuario, la logica de negocio, la seguridad, la persistencia y los servicios externos con el objetivo de mantener el sistema mantenible, testeable y extensible.

La arquitectura inicial se compone de los siguientes bloques:

| Bloque | Tecnologia | Responsabilidad principal |
| --- | --- | --- |
| Cliente web | React, TypeScript, Tailwind CSS, Vite | Presentar la interfaz, gestionar estado de sesion y consumir la API REST. |
| API backend | Java 21, Spring Boot 3 | Exponer endpoints, validar permisos, ejecutar reglas de negocio y coordinar persistencia. |
| Seguridad | Spring Security, JWT, bcrypt, 2FA | Autenticar usuarios, proteger rutas, emitir tokens y controlar acceso por rol. |
| Persistencia | Spring Data JPA, Hibernate, PostgreSQL | Guardar usuarios, salas, reservas, horarios, preferencias, auditoria y correos pendientes. |
| Notificaciones | Email outbox, Gmail API, Resend API o log fallback | Encolar y enviar correos de recuperacion, 2FA, confirmaciones y recordatorios. |
| Despliegue | Vercel, Render, Supabase | Publicar frontend, backend y base de datos administrada. |

### Objetivos arquitectonicos

- Separar responsabilidades para evitar clases con demasiadas razones de cambio.
- Centralizar reglas de reserva en el backend para proteger la integridad del negocio.
- Evitar acoplamiento directo con proveedores externos mediante abstracciones.
- Facilitar cambios futuros en reglas, proveedores de correo, vistas del frontend y configuraciones por campus.
- Proteger informacion sensible mediante autenticacion JWT, control por rol y hash de contrasenas.
- Mantener una API REST estable para que el frontend no dependa de detalles internos de persistencia.

## 5.1 Vista logica de la arquitectura

La vista logica muestra los modulos principales del sistema y sus dependencias conceptuales. Luistudio aplica una arquitectura por capas en backend y una organizacion por responsabilidades en frontend.

```mermaid
flowchart TB
    User[Usuario web]
    Browser[Navegador]
    Frontend[Frontend React + TypeScript]
    Api[API REST Spring Boot]
    Security[Seguridad JWT + 2FA]
    Controllers[Controladores REST]
    Services[Servicios de negocio]
    Rules[Reglas y patrones de negocio]
    Repositories[Repositorios Spring Data JPA]
    Database[(PostgreSQL)]
    Outbox[EmailOutboxService]
    Gateway[EmailGateway]
    ExternalEmail[Proveedor correo: Gmail/Resend/Log]

    User --> Browser
    Browser --> Frontend
    Frontend -->|HTTPS/JSON| Api
    Api --> Security
    Security --> Controllers
    Controllers --> Services
    Services --> Rules
    Services --> Repositories
    Repositories --> Database
    Services --> Outbox
    Outbox --> Gateway
    Gateway --> ExternalEmail
```

### Vista logica por capas del backend

```mermaid
flowchart LR
    subgraph Presentation[Presentacion HTTP]
        AuthController
        BookingController
        RoomController
        AdminController
        PreferenceController
        UserLookupController
    end

    subgraph Application[Aplicacion / Servicios]
        AuthService
        BookingService
        RoomService
        UserService
        SystemConfigService
        PreferenceService
        EmailOutboxService
        AuditService
        AccessGuard
    end

    subgraph Domain[Dominio y reglas]
        Entities[Entidades JPA]
        BookingRules[BookingValidationRule]
        LoginStrategies[LoginStrategy]
        Factories[Factories]
        Commands[BookingReminderCommand]
    end

    subgraph Infrastructure[Infraestructura]
        Repositories[Repositorios JPA]
        JwtService
        EmailGateways[EmailGateway adapters]
        GlobalExceptionHandler
    end

    Presentation --> Application
    Application --> Domain
    Application --> Infrastructure
    Infrastructure --> PostgreSQL[(PostgreSQL)]
    Infrastructure --> Providers[Correo externo]
```

### Vista logica del frontend

```mermaid
flowchart TB
    App[App.tsx]
    Routes[viewmodels/routes.ts]
    Pages[views/pages]
    Components[views/components]
    Services[services/api.ts]
    Models[models/types.ts]
    Backend[Backend /api]
    Storage[localStorage]

    App --> Routes
    Routes --> Pages
    Pages --> Components
    Pages --> Services
    Services --> Models
    Services --> Backend
    Pages --> Storage
    Components --> Storage
```

### Modulos funcionales principales

| Modulo | Componentes principales | Descripcion |
| --- | --- | --- |
| Autenticacion y seguridad | `AuthController`, `AuthService`, `JwtService`, `JwtAuthenticationFilter`, `LoginStrategy` | Login, JWT, 2FA, recuperacion de contrasena y bloqueo por intentos fallidos. |
| Reservas | `BookingController`, `BookingService`, `BookingValidationRule`, `ReservationRepository` | Creacion, edicion, cancelacion, consulta e integracion calendario `.ics`. |
| Salas y disponibilidad | `RoomController`, `RoomService`, `RoomScheduleService`, `RoomRepository` | CRUD de salas, disponibilidad, horarios por campus/sala y mantenimientos. |
| Administracion | `AdminController`, `UserService`, `SystemConfigService` | Gestion de usuarios, configuracion de limites y horarios generales. |
| Preferencias | `PreferenceController`, `PreferenceService` | Preferencias de correo, recordatorios, tema visual, escala de fuente y vista inicial. |
| Notificaciones | `EmailOutboxService`, `EmailGatewayFactory`, `EmailGateway` | Cola transaccional de correos y envio mediante adaptadores. |
| Auditoria | `AuditService`, `AuditLogRepository` | Registro de acciones relevantes, como actualizaciones de reservas. |

## 5.3 Explicacion y justificacion del diseno

### Separacion cliente-servidor

La aplicacion usa un frontend React separado de un backend Spring Boot. Esta decision permite que la interfaz evolucione de forma independiente del negocio y que las reglas criticas queden protegidas en el servidor. El navegador solo solicita operaciones por API REST; no decide si una reserva es valida, si un usuario es administrador o si una sala esta disponible.

### Backend por capas

El backend se organiza en controladores, servicios, repositorios, modelos, DTOs, seguridad y excepciones. Esta separacion cumple SRP porque cada capa tiene una razon de cambio distinta:

| Capa | Justificacion |
| --- | --- |
| Controller | Traduce peticiones HTTP a llamadas de aplicacion y devuelve respuestas JSON. |
| Service | Contiene reglas de negocio y coordina casos de uso. |
| Repository | Encapsula acceso a base de datos mediante Spring Data JPA. |
| Model | Representa entidades persistentes del dominio. |
| DTO | Define contratos externos sin exponer entidades JPA directamente. |
| Security | Aisla autenticacion, autorizacion y manejo de JWT. |
| Exception | Centraliza respuestas de error con `GlobalExceptionHandler`. |

Esta estructura reduce acoplamiento, simplifica pruebas unitarias y permite cambiar detalles de persistencia o presentacion sin reescribir reglas centrales.

### API REST y DTOs

La comunicacion entre frontend y backend se realiza con JSON sobre HTTP. Los DTOs (`BookingResponse`, `RoomResponse`, `LoginResponse`, etc.) actuan como contratos publicos y evitan que el cliente dependa de atributos internos de las entidades JPA. Esto protege el modelo de dominio y permite ajustar tablas o relaciones sin romper necesariamente la interfaz externa.

### Validacion centralizada de reservas

La creacion y edicion de reservas se valida en `BookingService` mediante una lista de objetos `BookingValidationRule`. Cada regla revisa una condicion concreta: capacidad, duracion maxima, horario permitido, disponibilidad de sala, cantidad maxima de reservas activas y consistencia entre hora inicial/final. Esta decision aplica OCP: para agregar una nueva restriccion se crea una nueva regla sin modificar el flujo principal.

### Seguridad por JWT, roles y 2FA

El sistema usa tokens JWT para autenticar solicitudes posteriores al login. `JwtAuthenticationFilter` interpreta el token y coloca la identidad del usuario en el contexto de seguridad. `AccessGuard` concentra verificaciones como usuario autenticado o administrador, lo que evita repetir logica de permisos en cada controlador.

La autenticacion en dos pasos se modela como una estrategia de login. Si el usuario no tiene 2FA, recibe un token final. Si tiene 2FA, recibe un token provisional y debe confirmar un codigo enviado por correo. Este diseno permite extender el flujo de autenticacion sin saturar `AuthService` con condicionales complejos.

### Persistencia con JPA y repositorios

Las entidades JPA modelan los conceptos principales: usuario, rol, sala, reserva, horario, mantenimiento, preferencias, codigos 2FA, reseteo de contrasena, auditoria y correos en cola. Los repositorios Spring Data JPA encapsulan consultas y operaciones CRUD. Esta eleccion reduce codigo repetitivo y mantiene las consultas cerca del agregado que representan.

### Outbox para notificaciones

El sistema no envia correos directamente dentro del caso de uso principal. Primero registra una fila en `email_outbox` y luego `EmailOutboxService` procesa los pendientes con un scheduler. Esto mejora resiliencia: si el proveedor de correo falla, la reserva ya puede quedar registrada y el correo se reintenta. Tambien evita que una falla externa bloquee completamente la operacion del usuario.

### Adaptadores para proveedores externos

El envio de correo depende de la interfaz `EmailGateway`. Las implementaciones concretas (`GmailEmailGateway`, `ResendEmailGateway`, `LogEmailGateway`) adaptan proveedores distintos al mismo contrato interno. Esta decision evita que el dominio dependa de APIs externas y facilita cambiar proveedor por variables de entorno.

### Frontend organizado por responsabilidades

El frontend separa modelos, servicios API, viewmodels, paginas y componentes. `services/api.ts` centraliza endpoints, token JWT y manejo de errores HTTP. Las paginas se concentran en interaccion y presentacion. Los componentes reutilizables, como modales y layouts, evitan duplicacion visual.

### Justificacion global

La arquitectura elegida es adecuada para Luistudio porque el sistema necesita seguridad, reglas de disponibilidad, administracion, notificaciones y crecimiento funcional. Una arquitectura por capas con patrones puntuales mantiene el proyecto entendible para un equipo universitario y suficientemente robusto para evolucionar sin reescrituras grandes.

## 5.4 Patron de diseno aplicado

El detalle operativo de patrones esta documentado en `docs/principios/application.md`. En esta seccion se resume su aplicacion para el informe.

| Patron | Tipo | Ubicacion | Problema que resuelve |
| --- | --- | --- | --- |
| Controller-Service-Repository | Arquitectonico / Spring | `controller`, `service`, `repository` | Separa entrada HTTP, negocio y persistencia. |
| DTO Mapper | Estructural de soporte | `DtoMapper` | Evita exponer entidades JPA al frontend y centraliza conversiones. |
| Strategy | Comportamiento | `service/booking/rule/*`, `service/auth/strategy/*` | Permite intercambiar o agregar reglas/algoritmos sin modificar servicios principales. |
| Factory Method | Creacional | `service/factory/*`, `EmailGatewayFactory` | Centraliza creacion de entidades y seleccion de gateway. |
| Adapter | Estructural | `service/email/gateway/*Gateway` | Encapsula diferencias entre Gmail, Resend y log. |
| Command | Comportamiento | `service/booking/command/*` | Encapsula acciones programadas de recordatorio de reservas. |
| Repository | Persistencia | `repository/*Repository` | Abstrae operaciones de base de datos. |
| Global Exception Handler | Manejo transversal | `GlobalExceptionHandler` | Normaliza errores de negocio y validacion para la API. |

### Patron principal destacado: Strategy en validacion de reservas

`BookingService` no implementa todas las validaciones con una cadena larga de `if`. En su lugar recibe una lista de `BookingValidationRule` por inyeccion de dependencias. Cada regla implementa el mismo contrato y valida una condicion especifica.

```mermaid
classDiagram
    class BookingService {
        -List~BookingValidationRule~ bookingValidationRules
        +createBooking(userId, request)
        +updateBooking(bookingId, actorUserId, request)
        -validateBookingRules(user, room, request, excludeBookingId)
    }

    class BookingValidationRule {
        <<interface>>
        +validate(context)
    }

    class CapacityRule
    class EndTimeAfterStartRule
    class MaxDurationRule
    class MaxActiveBookingsRule
    class RoomAvailabilityRule
    class BookingWindowAndSlotRule

    BookingService --> BookingValidationRule
    BookingValidationRule <|.. CapacityRule
    BookingValidationRule <|.. EndTimeAfterStartRule
    BookingValidationRule <|.. MaxDurationRule
    BookingValidationRule <|.. MaxActiveBookingsRule
    BookingValidationRule <|.. RoomAvailabilityRule
    BookingValidationRule <|.. BookingWindowAndSlotRule
```

La ventaja es que una nueva regla, por ejemplo bloqueo por feriados o prioridad por tipo de usuario, puede agregarse como otra clase que implemente `BookingValidationRule` sin reescribir `BookingService`.

## 6. Diseno estructural

El diseno estructural describe las clases, relaciones y colaboraciones estaticas principales. En Luistudio existen dos estructuras relevantes: el modelo de dominio persistente y la estructura de servicios/controladores que ejecutan los casos de uso.

## 6.1 Diagrama de clases de diseno

### Diagrama general de dominio

```mermaid
classDiagram
    class RoleEntity {
        +Long id
        +String nombre
        +String descripcion
    }

    class UserEntity {
        +Long id
        +String codigo
        +String nombres
        +String apellidos
        +String correo
        +String passwordHash
        +UserStatus estado
        +OffsetDateTime lockedUntil
        +Boolean has2fa
    }

    class PabellonEntity {
        +Long id
        +String codigo
        +String nombre
        +String campus
        +String venue
    }

    class RoomEntity {
        +Long id
        +String codigo
        +String nombre
        +Integer capacidad
        +String campus
        +String venue
        +String ubicacion
        +Integer minimoPersonas
        +Integer maximoPersonas
        +RoomState estado
    }

    class ReservationEntity {
        +Long id
        +LocalDate fecha
        +LocalTime horaInicio
        +LocalTime horaFin
        +ReservationStatus estado
        +Integer cantidadPersonas
        +String observacion
        +Long updatedBy
    }

    class RoomScheduleEntity {
        +Long id
        +Integer diaSemana
        +LocalTime horaApertura
        +LocalTime horaCierre
        +Boolean cerrado
    }

    class CampusScheduleEntity {
        +Long id
        +String campus
        +Integer diaSemana
        +LocalTime horaApertura
        +LocalTime horaCierre
        +Boolean cerrado
        +Integer slotMinutes
    }

    class MaintenanceEntity {
        +Long id
        +OffsetDateTime inicio
        +OffsetDateTime fin
        +String motivo
        +MaintenanceStatus estado
    }

    class NotificationPreferenceEntity {
        +Long id
        +Boolean emailHabilitado
        +Boolean reminderHabilitado
        +Boolean cambiosReservaHabilitado
        +String themeMode
        +Integer fontScale
        +String loginLandingView
    }

    class EmailOutboxEntity {
        +Long id
        +String destinatario
        +String asunto
        +String cuerpo
        +EmailStatus estado
        +Integer intentos
    }

    RoleEntity "1" --> "0..*" UserEntity : asigna rol
    UserEntity "1" --> "0..*" ReservationEntity : realiza
    PabellonEntity "1" --> "0..*" RoomEntity : agrupa
    RoomEntity "1" --> "0..*" ReservationEntity : se reserva
    RoomEntity "1" --> "0..*" RoomScheduleEntity : define override
    RoomEntity "1" --> "0..*" MaintenanceEntity : tiene mantenimiento
    UserEntity "1" --> "0..1" NotificationPreferenceEntity : configura
    UserEntity "1" --> "0..*" EmailOutboxEntity : recibe
```

### Diagrama de servicios, controladores y repositorios

```mermaid
classDiagram
    class AuthController
    class BookingController
    class RoomController
    class AdminController
    class PreferenceController

    class AuthService
    class BookingService
    class RoomService
    class UserService
    class SystemConfigService
    class PreferenceService
    class EmailOutboxService
    class AccessGuard
    class DtoMapper

    class UserRepository
    class ReservationRepository
    class RoomRepository
    class RoleRepository
    class EmailOutboxRepository
    class NotificationPreferenceRepository

    AuthController --> AuthService
    BookingController --> BookingService
    BookingController --> AccessGuard
    RoomController --> RoomService
    RoomController --> BookingService
    RoomController --> AccessGuard
    AdminController --> UserService
    AdminController --> SystemConfigService
    AdminController --> AccessGuard
    PreferenceController --> PreferenceService

    AuthService --> UserRepository
    AuthService --> DtoMapper
    AuthService --> EmailOutboxService
    BookingService --> ReservationRepository
    BookingService --> RoomService
    BookingService --> UserService
    BookingService --> EmailOutboxService
    BookingService --> DtoMapper
    RoomService --> RoomRepository
    UserService --> UserRepository
    PreferenceService --> NotificationPreferenceRepository
    EmailOutboxService --> EmailOutboxRepository
```

### Diagrama de patrones aplicados

```mermaid
classDiagram
    class EmailGateway {
        <<interface>>
        +send(email)
    }
    class LogEmailGateway
    class ResendEmailGateway
    class GmailEmailGateway
    class EmailGatewayFactory {
        +createGateway()
    }
    class EmailOutboxService {
        +enqueue(recipient, subject, body, payload)
        +processPendingEmails()
    }

    EmailGateway <|.. LogEmailGateway
    EmailGateway <|.. ResendEmailGateway
    EmailGateway <|.. GmailEmailGateway
    EmailGatewayFactory --> EmailGateway
    EmailOutboxService --> EmailGatewayFactory
    EmailOutboxService --> EmailGateway

    class LoginStrategy {
        <<interface>>
        +supports(user)
        +buildResponse(user)
    }
    class StandardLoginStrategy
    class TwoFactorLoginStrategy
    class AuthService

    LoginStrategy <|.. StandardLoginStrategy
    LoginStrategy <|.. TwoFactorLoginStrategy
    AuthService --> LoginStrategy

    class BookingReminderCommand {
        <<interface>>
        +execute()
    }
    class SendUpcomingReservationReminderCommand
    class SendEndingSoonReservationReminderCommand
    class BookingReminderScheduler

    BookingReminderCommand <|.. SendUpcomingReservationReminderCommand
    BookingReminderCommand <|.. SendEndingSoonReservationReminderCommand
    BookingReminderScheduler --> BookingReminderCommand
```

## 6.2 Descripcion de clases

### Clases de presentacion HTTP

| Clase | Responsabilidad | Colaboradores |
| --- | --- | --- |
| `AuthController` | Expone endpoints de login, 2FA, usuario actual y recuperacion de contrasena. | `AuthService`, `CurrentUserProvider`. |
| `BookingController` | Expone endpoints para crear, editar, cancelar, listar reservas y descargar `.ics`. | `BookingService`, `AccessGuard`. |
| `RoomController` | Expone consulta de salas, disponibilidad, CRUD de salas y mantenimientos. | `RoomService`, `BookingService`, `AccessGuard`. |
| `AdminController` | Expone endpoints administrativos de usuarios, configuracion, horarios y mapa de campus. | `UserService`, `SystemConfigService`, `RoomScheduleService`, `CampusMapService`. |
| `PreferenceController` | Gestiona preferencias del usuario autenticado. | `PreferenceService`, `AccessGuard`. |
| `UserLookupController` | Permite buscar usuarios por codigo para agregarlos como participantes. | `UserService`, `AccessGuard`. |

### Clases de servicios de aplicacion

| Clase | Responsabilidad | Decisiones importantes |
| --- | --- | --- |
| `AuthService` | Gestiona login, 2FA, bloqueo por intentos fallidos y recuperacion de contrasena. | Usa `LoginStrategy`, `SecurityEntityFactory`, `JwtService` y `EmailOutboxService`. |
| `BookingService` | Ejecuta casos de uso de reservas. | Valida reglas con `BookingValidationRule`, reutiliza reservas duplicadas logicas, genera `.ics` y encola correos. |
| `RoomService` | Gestiona salas, disponibilidad y mantenimientos. | Coordina catalogo, horarios, capacidad y estados de sala. |
| `RoomScheduleService` | Administra horarios por campus y sala. | Permite validar bloques y detectar conflictos con reservas futuras. |
| `UserService` | Lista usuarios, cambia estados y obtiene usuarios por id/codigo. | Mantiene reglas de perfiles y permisos administrativos. |
| `PreferenceService` | Lee y actualiza preferencias del usuario. | Sincroniza tema, escala de fuente, notificaciones y vista inicial. |
| `SystemConfigService` | Maneja parametros globales y por campus. | Valida cambios de duracion de bloque segun reservas futuras. |
| `EmailOutboxService` | Encola y procesa correos pendientes. | Aplica outbox, reintentos y fallback de gateway. |
| `AuditService` | Registra acciones relevantes. | Apoya trazabilidad de operaciones administrativas o sensibles. |
| `AccessGuard` | Centraliza validacion de usuario autenticado y rol admin. | Evita duplicar verificaciones de permisos. |
| `DtoMapper` | Convierte entidades a DTOs de respuesta. | Reduce duplicacion y protege el modelo persistente. |

### Clases de dominio

| Clase | Descripcion |
| --- | --- |
| `UserEntity` | Representa usuarios del sistema, sus credenciales, estado, rol y 2FA. |
| `RoleEntity` | Representa roles como administrador y estudiante. |
| `RoomEntity` | Representa salas o recursos reservables con capacidad, ubicacion, campus, pabellon y estado. |
| `ReservationEntity` | Representa una reserva con usuario, sala, fecha, hora, estado, cantidad de personas y observacion. |
| `RoomScheduleEntity` | Define horarios especificos por sala. |
| `CampusScheduleEntity` | Define horarios y duracion de bloque por campus. |
| `MaintenanceEntity` | Representa indisponibilidades o mantenimientos programados de una sala. |
| `NotificationPreferenceEntity` | Guarda preferencias de notificacion y accesibilidad visual por usuario. |
| `EmailOutboxEntity` | Representa un correo pendiente, enviado o fallido dentro de la cola transaccional. |
| `AuditLogEntity` | Guarda eventos de auditoria para trazabilidad. |
| `TwoFactorCodeEntity` | Guarda codigos temporales de autenticacion en dos pasos. |
| `PasswordResetEntity` | Guarda tokens temporales de recuperacion de contrasena. |
| `LoginAttemptEntity` | Registra intentos de login para bloqueo temporal y seguridad. |

### Interfaces y clases de patrones

| Clase o interfaz | Patron | Funcion |
| --- | --- | --- |
| `BookingValidationRule` | Strategy | Contrato comun para reglas de validacion de reservas. |
| `CapacityRule` | Strategy | Verifica minimo/maximo de personas y capacidad. |
| `EndTimeAfterStartRule` | Strategy | Verifica que la hora final sea posterior a la inicial. |
| `MaxDurationRule` | Strategy | Limita duracion segun configuracion del campus. |
| `MaxActiveBookingsRule` | Strategy | Limita cantidad de reservas activas por usuario. |
| `RoomAvailabilityRule` | Strategy | Evita choques con reservas existentes o indisponibilidades. |
| `BookingWindowAndSlotRule` | Strategy | Valida ventana permitida y alineacion con bloques horarios. |
| `LoginStrategy` | Strategy | Define respuesta de login segun configuracion del usuario. |
| `StandardLoginStrategy` | Strategy | Emite JWT final para login sin 2FA. |
| `TwoFactorLoginStrategy` | Strategy | Emite token provisional y envia codigo 2FA. |
| `ReservationFactory` | Factory Method | Crea reservas activas con valores iniciales consistentes. |
| `RoomFactory` | Factory Method | Crea entidades de sala a partir de solicitudes. |
| `MaintenanceFactory` | Factory Method | Crea mantenimientos o indisponibilidades. |
| `SecurityEntityFactory` | Factory Method | Crea codigos 2FA, tokens de reseteo e intentos de login. |
| `EmailGateway` | Adapter | Interfaz comun para proveedores de correo. |
| `GmailEmailGateway` | Adapter | Adapta Gmail API al contrato interno. |
| `ResendEmailGateway` | Adapter | Adapta Resend API al contrato interno. |
| `LogEmailGateway` | Adapter | Implementa fallback local por logs. |
| `EmailGatewayFactory` | Factory Method | Selecciona gateway segun variables de entorno disponibles. |
| `BookingReminderCommand` | Command | Contrato para acciones programadas de recordatorio. |
| `BookingReminderScheduler` | Command invoker | Ejecuta comandos de recordatorio mediante scheduler. |

### Clases/archivos relevantes del frontend

| Archivo | Responsabilidad |
| --- | --- |
| `src/App.tsx` | Composicion general de la aplicacion y navegacion principal. |
| `src/viewmodels/routes.ts` | Define rutas y metadatos de navegacion por rol. |
| `src/services/api.ts` | Centraliza llamadas HTTP, base URL, JWT, manejo de errores y contratos API. |
| `src/models/types.ts` | Tipos compartidos de dominio y presentacion. |
| `src/views/pages/LoginPage.tsx` | Flujo de login y recuperacion. |
| `src/views/pages/ReservasPage.tsx` | Creacion de reservas y seleccion de horarios/salas. |
| `src/views/pages/MisReservasPage.tsx` | Consulta, edicion y cancelacion de reservas propias. |
| `src/views/pages/SalasPage.tsx` | Administracion de salas. |
| `src/views/pages/AdminReservasPage.tsx` | Revision administrativa de reservas. |
| `src/views/pages/PerfilesPage.tsx` | Gestion administrativa de usuarios. |
| `src/views/components/modals/*` | Modales reutilizables de confirmacion, edicion, 2FA y mensajes. |

## 6.3 Modelo dinamico

El modelo dinamico describe como cambian los objetos y como colaboran durante los casos de uso. En Luistudio los flujos mas importantes son autenticacion, reserva, cancelacion, administracion y envio de notificaciones.

### 6.3.1 Estados principales

#### Ciclo de vida de una reserva

```mermaid
stateDiagram-v2
    [*] --> ACTIVA: crear reserva valida
    ACTIVA --> ACTIVA: editar fecha/hora/sala/personas
    ACTIVA --> CANCELADA: cancelar antes de finalizar
    ACTIVA --> COMPLETADA: finaliza horario de reserva
    CANCELADA --> ACTIVA: nueva reserva con misma identidad logica
    CANCELADA --> [*]
    COMPLETADA --> [*]
```

Notas:

- Una reserva nace como `ACTIVA` si supera todas las reglas de negocio.
- La cancelacion solo se permite si la reserva aun no finalizo.
- Si el usuario intenta reservar el mismo recurso, fecha y horario, se reutiliza el registro existente y se actualiza su informacion.
- `COMPLETADA` representa el estado esperado para reservas que ya concluyeron segun la fecha/hora.

#### Ciclo de vida de autenticacion con 2FA

```mermaid
stateDiagram-v2
    [*] --> CredencialesIngresadas
    CredencialesIngresadas --> LoginRechazado: credenciales invalidas
    CredencialesIngresadas --> Bloqueado: demasiados intentos fallidos
    CredencialesIngresadas --> Autenticado: usuario sin 2FA
    CredencialesIngresadas --> TokenProvisional: usuario con 2FA
    TokenProvisional --> Autenticado: codigo valido
    TokenProvisional --> LoginRechazado: codigo invalido o expirado
    Autenticado --> [*]
    LoginRechazado --> [*]
    Bloqueado --> [*]
```

#### Ciclo de vida de correo en outbox

```mermaid
stateDiagram-v2
    [*] --> PENDIENTE: enqueue
    PENDIENTE --> ENVIADO: gateway envia correctamente
    PENDIENTE --> PENDIENTE: falla con intentos menores a 3
    PENDIENTE --> ERROR: falla con 3 intentos
    ENVIADO --> [*]
    ERROR --> [*]
```

## 6.3.2 Diagramas de secuencia

### Secuencia 1: Login estandar sin 2FA

```mermaid
sequenceDiagram
    actor Usuario
    participant UI as Frontend LoginPage
    participant API as services/api.ts
    participant AuthController
    participant AuthService
    participant UserRepository
    participant PasswordEncoder
    participant StandardLoginStrategy
    participant JwtService

    Usuario->>UI: Ingresa correo y contrasena
    UI->>API: login(email, password)
    API->>AuthController: POST /api/auth/login
    AuthController->>AuthService: login(request, ip)
    AuthService->>UserRepository: findByCorreoIgnoreCase(email)
    UserRepository-->>AuthService: UserEntity
    AuthService->>PasswordEncoder: matches(password, hash)
    PasswordEncoder-->>AuthService: true
    AuthService->>StandardLoginStrategy: buildResponse(user)
    StandardLoginStrategy->>JwtService: generateToken(userId, email, role)
    JwtService-->>StandardLoginStrategy: JWT
    StandardLoginStrategy-->>AuthService: LoginResponse(token)
    AuthService-->>AuthController: LoginResponse
    AuthController-->>API: JSON token + usuario
    API-->>UI: respuesta login
    UI->>UI: Guarda token y navega segun rol/preferencia
```

### Secuencia 2: Login con 2FA

```mermaid
sequenceDiagram
    actor Usuario
    participant UI as Frontend LoginPage + TwoFactorModal
    participant API as services/api.ts
    participant AuthController
    participant AuthService
    participant TwoFactorLoginStrategy
    participant SecurityEntityFactory
    participant TwoFactorCodeRepository
    participant EmailOutboxService
    participant JwtService

    Usuario->>UI: Ingresa credenciales
    UI->>API: login(email, password)
    API->>AuthController: POST /api/auth/login
    AuthController->>AuthService: login(request, ip)
    AuthService->>TwoFactorLoginStrategy: buildResponse(user con has2fa=true)
    TwoFactorLoginStrategy->>SecurityEntityFactory: newTwoFactorCode(user, 10)
    SecurityEntityFactory-->>TwoFactorLoginStrategy: TwoFactorCodeEntity
    TwoFactorLoginStrategy->>TwoFactorCodeRepository: save(code)
    TwoFactorLoginStrategy->>EmailOutboxService: enqueue(user, codigo 2FA)
    TwoFactorLoginStrategy->>JwtService: generateProvisionalToken(...)
    JwtService-->>TwoFactorLoginStrategy: token provisional
    TwoFactorLoginStrategy-->>AuthService: LoginResponse(twoFactorRequired=true)
    AuthService-->>UI: requiere 2FA
    Usuario->>UI: Ingresa codigo recibido
    UI->>API: verify2fa(provisionalToken, code)
    API->>AuthController: POST /api/auth/2fa/verify
    AuthController->>AuthService: verify2fa(request)
    AuthService->>JwtService: validate(provisionalToken)
    AuthService->>TwoFactorCodeRepository: findTopByUsuarioAndUsadoFalseOrderByIdDesc(user)
    TwoFactorCodeRepository-->>AuthService: codigo vigente
    AuthService->>TwoFactorCodeRepository: save(usado=true)
    AuthService->>JwtService: generateToken(userId, email, role)
    JwtService-->>AuthService: JWT final
    AuthService-->>UI: LoginResponse(token final)
    UI->>UI: Guarda token y entra al sistema
```

### Secuencia 3: Crear reserva

```mermaid
sequenceDiagram
    actor Estudiante
    participant UI as ReservasPage
    participant API as services/api.ts
    participant BookingController
    participant AccessGuard
    participant BookingService
    participant UserService
    participant RoomService
    participant Rule as BookingValidationRule[]
    participant ReservationFactory
    participant ReservationRepository
    participant EmailOutboxService
    participant DtoMapper

    Estudiante->>UI: Selecciona sala, fecha, horario y personas
    UI->>API: createBooking(token, payload)
    API->>BookingController: POST /api/bookings + JWT
    BookingController->>AccessGuard: requireUser()
    AccessGuard-->>BookingController: AuthPrincipal
    BookingController->>BookingService: createBooking(userId, request)
    BookingService->>UserService: getById(userId)
    UserService-->>BookingService: UserEntity
    BookingService->>RoomService: getRoomEntity(roomId)
    RoomService-->>BookingService: RoomEntity
    BookingService->>ReservationRepository: buscar reserva con misma identidad logica
    ReservationRepository-->>BookingService: Optional reserva existente
    loop Por cada regla
        BookingService->>Rule: validate(context)
        Rule-->>BookingService: ok o BusinessException
    end
    alt No existe reserva previa
        BookingService->>ReservationFactory: createActiveReservation(user, room, request)
        ReservationFactory-->>BookingService: ReservationEntity ACTIVA
    else Existe reserva previa
        BookingService->>BookingService: actualiza estado/datos de reserva existente
    end
    BookingService->>ReservationRepository: save(reservation)
    ReservationRepository-->>BookingService: ReservationEntity guardada
    BookingService->>EmailOutboxService: enqueue(confirmacion + .ics)
    BookingService->>DtoMapper: toBooking(saved)
    DtoMapper-->>BookingService: BookingResponse
    BookingService-->>BookingController: BookingResponse
    BookingController-->>UI: reserva confirmada
```

### Secuencia 4: Cancelar reserva

```mermaid
sequenceDiagram
    actor Usuario
    participant UI as MisReservasPage
    participant API as services/api.ts
    participant BookingController
    participant AccessGuard
    participant BookingService
    participant ReservationRepository
    participant EmailOutboxService
    participant DtoMapper

    Usuario->>UI: Solicita cancelar reserva
    UI->>API: cancelBooking(token, bookingId)
    API->>BookingController: PATCH /api/bookings/{id}/cancel
    BookingController->>AccessGuard: requireUser()
    AccessGuard-->>BookingController: AuthPrincipal
    BookingController->>BookingService: cancelBooking(id, actorUserId, adminCancel)
    BookingService->>ReservationRepository: findById(id)
    ReservationRepository-->>BookingService: ReservationEntity
    alt Reserva ya finalizo
        BookingService-->>BookingController: BusinessException 400
        BookingController-->>UI: error de negocio
    else Reserva cancelable
        BookingService->>ReservationRepository: save(estado=CANCELADA)
        BookingService->>EmailOutboxService: enqueue(correo cancelacion)
        BookingService->>DtoMapper: toBooking(saved)
        DtoMapper-->>BookingService: BookingResponse
        BookingService-->>BookingController: BookingResponse
        BookingController-->>UI: reserva cancelada
    end
```

### Secuencia 5: Administrador actualiza una sala

```mermaid
sequenceDiagram
    actor Admin
    participant UI as SalasPage
    participant API as services/api.ts
    participant RoomController
    participant AccessGuard
    participant RoomService
    participant RoomRepository
    participant RoomScheduleService
    participant DtoMapper

    Admin->>UI: Edita datos y horario de sala
    UI->>API: updateRoom(token, roomId, payload)
    API->>RoomController: PUT /api/rooms/{roomId}
    RoomController->>AccessGuard: requireAdmin()
    AccessGuard-->>RoomController: AuthPrincipal admin
    RoomController->>RoomService: updateRoom(roomId, request)
    RoomService->>RoomRepository: findById(roomId)
    RoomRepository-->>RoomService: RoomEntity
    RoomService->>RoomService: valida datos, capacidad y estado
    RoomService->>RoomRepository: save(room)
    RoomService->>RoomScheduleService: sincroniza horarios si aplica
    RoomService->>DtoMapper: toRoom(saved)
    DtoMapper-->>RoomService: RoomResponse
    RoomService-->>RoomController: RoomResponse
    RoomController-->>UI: sala actualizada
```

### Secuencia 6: Procesamiento asincrono de correos

```mermaid
sequenceDiagram
    participant Scheduler as Spring Scheduler
    participant EmailOutboxService
    participant EmailOutboxRepository
    participant EmailGateway
    participant Provider as Gmail/Resend/Log

    Scheduler->>EmailOutboxService: processPendingEmails() cada 60s
    EmailOutboxService->>EmailOutboxRepository: findReadyToProcess(PENDIENTE, now)
    EmailOutboxRepository-->>EmailOutboxService: correos pendientes
    loop Por cada correo
        EmailOutboxService->>EmailGateway: send(email)
        EmailGateway->>Provider: enviar segun adaptador
        alt envio correcto
            Provider-->>EmailGateway: ok
            EmailOutboxService->>EmailOutboxRepository: save(estado=ENVIADO, enviadoEn)
        else falla proveedor
            Provider-->>EmailGateway: error
            EmailOutboxService->>EmailOutboxRepository: save(intentos+1, PENDIENTE o ERROR)
        end
    end
```

## Notas para redaccion final

- En el informe final se puede usar `5.2` para vista fisica/de despliegue si el formato lo exige, aunque no fue solicitado en este documento.
- Si el docente pide UML formal, los diagramas Mermaid pueden convertirse a imagen o replicarse en StarUML, Visual Paradigm, draw.io o PlantUML.
- Para no sobrecargar el diagrama de clases, se recomienda mostrar solo entidades y servicios principales, dejando DTOs y repositorios secundarios en tablas descriptivas.
- La seccion de patrones debe referenciar `docs/principios/application.md` como documento tecnico interno del equipo.
