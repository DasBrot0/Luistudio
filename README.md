# Luistudio

Sistema web universitario para la reserva y gestión de salas de estudio en bibliotecas y campus académicos.

## ¿Qué es Luistudio?

**Luistudio** combina *"Luis"* (en honor a una persona especial del equipo) y *"Studio"* (espacio para realizar actividades). El sistema permite que los estudiantes consulten disponibilidad en tiempo real, reserven, modifiquen y cancelen salas de manera rápida y segura, mientras que los administradores gestionan el catálogo de salas, supervisan reservas y configuran periodos de mantenimiento.

## Propósito

Optimizar la gestión de salas de estudio en entornos universitarios, eliminando métodos manuales (papel, hojas de cálculo, formularios) mediante un control centralizado y automatizado.

## Alcance

El sistema está orientado a bibliotecas universitarias o campus académicos con múltiples salas de estudio. La interfaz es completamente accesible desde navegadores web, adaptada a computadoras y dispositivos móviles.

### Cuatro módulos principales

| Módulo | Descripción |
|--------|-------------|
| **Gestión de Reservas** | Reservar, modificar y cancelar salas; visualizar disponibilidad en tiempo real |
| **Gestión de Perfiles** | Registro, autenticación, actualización de datos y gestión de roles |
| **Gestión de Notificaciones** | Confirmaciones, recordatorios y alertas automáticas por correo |
| **Gestión de Seguridad** | Autenticación, 2FA, bloqueo por intentos fallidos y encriptación |

## Metodología

Desarrollo ágil con **Scrum**, estructurado en 2 releases de 8 semanas cada uno (6 sprints en total).

## Supuestos

- La institución cuenta con conexión a Internet estable y continua.
- Los usuarios disponen de dispositivos con navegadores web compatibles.
- El Product Owner participará activamente en las revisiones de cada sprint.
- Los datos ingresados por los usuarios serán veraces y completos.

## Restricciones

- El sistema debe implementarse dentro del plazo establecido (16 semanas totales).
- Acceso exclusivamente vía navegador web con conexión a Internet.
- Los datos personales deben estar protegidos y cumplir regulaciones de privacidad.
- El equipo debe ajustarse al presupuesto definido; no se contemplan gastos adicionales en licencias o infraestructura.

## Contribución a los ODS

- **ODS 4 – Educación de Calidad:** Facilita el acceso equitativo a espacios de aprendizaje colaborativo e individual.
- **ODS 11 – Ciudades y Comunidades Sostenibles:** Promueve el uso responsable de la infraestructura universitaria, evitando saturación y desperdicio de recursos.

## Resultado esperado

Un sistema web que permita:

- Reservar y administrar espacios de estudio
- Gestionar usuarios y roles
- Visualizar disponibilidad en tiempo real
- Recibir notificaciones automáticas
- Integración con calendario (`.ics` / Google Calendar)
- Dashboard administrativo con analítica de uso
- Seguridad avanzada con 2FA
- Interfaz con modo oscuro
- Acceso desde PC y dispositivos móviles

## 🗂️ Estructura del proyecto

La aplicación está separada en backend Spring Boot y frontend React/Vite. Esta vista prioriza los archivos `.java`, `.tsx`, `.ts` y `.css` propios del proyecto, además de otros archivos relevantes para entender cómo se organiza Luistudio.

```text
Luistudio/
├── backend/
│   ├── README_BACKEND.md                 # 📘 Guía técnica del backend
│   └── reservas/
│       ├── pom.xml                       # 📦 Dependencias y build Maven
│       └── src/
│           ├── main/
│           │   ├── java/com/luistudio/reservas/
│           │   │   ├── ReservasApplication.java        # 🚀 Punto de entrada de Spring Boot
│           │   │   ├── config/                         # ⚙️ Configuración transversal
│           │   │   │   └── SecurityConfig.java         # 🔐 Reglas de seguridad HTTP, CORS y filtros JWT
│           │   │   ├── controller/                     # 🌐 Endpoints REST
│           │   │   │   ├── AdminController.java        # 🛠️ Configuración, usuarios y horarios administrativos
│           │   │   │   ├── AuthController.java         # 🔑 Login, logout, sesión, 2FA y recuperación
│           │   │   │   ├── BookingController.java      # 📅 CRUD y consulta de reservas
│           │   │   │   ├── PreferenceController.java   # 🔔 Preferencias de notificación del usuario
│           │   │   │   ├── RoomController.java         # 🚪 Gestión y consulta de salas
│           │   │   │   └── UserLookupController.java   # 👤 Búsqueda ligera de usuarios
│           │   │   ├── dto/                            # 📄 Contratos de entrada y salida
│           │   │   │   ├── admin/                      # 🛠️ DTOs de administración
│           │   │   │   ├── auth/                       # 🔑 DTOs de autenticación
│           │   │   │   ├── booking/                    # 📅 DTOs de reservas
│           │   │   │   ├── common/                     # 🧩 Respuestas comunes
│           │   │   │   ├── room/                       # 🚪 DTOs de salas y mantenimiento
│           │   │   │   └── user/                       # 👤 DTOs de usuarios y preferencias
│           │   │   ├── exception/                      # 🚨 Manejo de errores
│           │   │   │   ├── BusinessException.java      # ⚠️ Error de regla de negocio
│           │   │   │   ├── GlobalExceptionHandler.java # 🧯 Convierte excepciones en respuestas HTTP
│           │   │   │   └── NotFoundException.java      # 🔎 Error para recursos inexistentes
│           │   │   ├── model/                          # 🗃️ Entidades JPA y enums
│           │   │   ├── repository/                     # 🧱 Acceso a datos con Spring Data JPA
│           │   │   ├── security/                       # 🛡️ JWT, cookies y usuario autenticado
│           │   │   ├── service/                        # 🧠 Lógica de negocio
│           │   │   │   ├── auth/strategy/              # 🔐 Estrategias de login estándar y 2FA
│           │   │   │   ├── booking/command/            # ⏰ Comandos y scheduler de recordatorios
│           │   │   │   ├── booking/rule/               # ✅ Reglas de validación de reservas
│           │   │   │   ├── email/gateway/              # ✉️ Adaptadores para envío de correo
│           │   │   │   └── factory/                    # 🏭 Creación consistente de entidades
│           │   │   └── util/
│           │   │       └── CalendarUtils.java          # 🗓️ Generación de archivos/calendarios ICS
│           │   └── resources/                          # 🧾 Configuración y recursos del backend
│           └── test/java/com/luistudio/reservas/
│               ├── ReservasApplicationTests.java       # ✅ Prueba de carga del contexto Spring
│               ├── security/                           # 🧪 Tests de seguridad
│               └── service/factory/                    # 🧪 Tests de factories
├── frontend/
│   ├── README_FRONTEND.md                # 📘 Guía técnica del frontend
│   └── luistudio-app/
│       ├── index.html                    # 🌎 HTML base donde monta React
│       ├── package.json                  # 📦 Scripts y dependencias npm
│       ├── vite.config.ts                # ⚡ Configuración de Vite
│       └── src/
│           ├── App.tsx                   # 🧭 Componente raíz, estado global básico y rutas
│           ├── App.css                   # 🎨 Estilos específicos de la app
│           ├── index.css                 # 🎨 Estilos globales y Tailwind
│           ├── main.tsx                  # 🚀 Punto de entrada de React
│           ├── models/                   # 🧾 Tipos TypeScript del dominio
│           ├── services/                 # 🔌 Cliente HTTP hacia el backend
│           ├── utils/                    # 🧰 Funciones auxiliares
│           ├── viewmodels/               # 🧭 Definición de rutas/vistas
│           └── views/                    # 🖥️ Páginas y componentes visuales
├── database/                             # 🗄️ Scripts SQL iniciales
├── docs/                                 # 📚 Documentación funcional y técnica
└── scripts/                              # 🧪 Scripts de arranque y automatización
```

### ☕ Backend: detalle de archivos Java

**🌐 Controllers**

| Archivo | Descripción breve |
|---|---|
| `AdminController.java` | Expone operaciones administrativas para configuración del sistema, usuarios, mapa de campus y horarios. |
| `AuthController.java` | Maneja autenticación, sesión actual, logout, verificación 2FA y recuperación de contraseña. |
| `BookingController.java` | Publica endpoints para crear, listar, actualizar, cancelar y consultar disponibilidad de reservas. |
| `PreferenceController.java` | Permite leer y actualizar preferencias de notificación del usuario autenticado. |
| `RoomController.java` | Gestiona salas, estados, mantenimiento y consultas del catálogo. |
| `UserLookupController.java` | Resuelve búsquedas simples de usuarios para formularios o flujos administrativos. |

**📄 DTOs**

| Archivo | Descripción breve |
|---|---|
| `AdminConfigResponse.java` | Respuesta con la configuración administrativa vigente. |
| `AdminConfigUpdateRequest.java` | Entrada para actualizar parámetros administrativos del sistema. |
| `CampusMapResponse.java` | Respuesta usada para representar el mapa/estructura de campus y pabellones. |
| `CampusScheduleDayInput.java` | Entrada de horario por día para configurar atención del campus. |
| `CampusScheduleDayResponse.java` | Respuesta de horario diario del campus. |
| `CampusScheduleListResponse.java` | Respuesta agrupada con horarios de campus. |
| `CampusScheduleResponse.java` | Respuesta de un horario de campus específico. |
| `CampusScheduleUpdateRequest.java` | Entrada para actualizar horarios del campus. |
| `AuthUserResponse.java` | Datos del usuario autenticado que se devuelven al frontend. |
| `LoginRequest.java` | Credenciales de inicio de sesión. |
| `LoginResponse.java` | Resultado de login, token/sesión y estado de 2FA. |
| `ResetConfirmInput.java` | Entrada para confirmar restablecimiento de contraseña. |
| `ResetRequestInput.java` | Entrada para solicitar recuperación de contraseña. |
| `TwoFactorCodeInput.java` | Entrada para pedir o reenviar código 2FA. |
| `TwoFactorVerifyInput.java` | Entrada para verificar un código 2FA. |
| `BookingResponse.java` | Representación de una reserva enviada al frontend. |
| `BookingUpsertRequest.java` | Entrada para crear o actualizar una reserva. |
| `ApiError.java` | Formato estándar para errores de API. |
| `MessageResponse.java` | Respuesta simple con mensaje de confirmación. |
| `PageResponse.java` | Envoltorio para respuestas paginadas. |
| `MaintenanceRequest.java` | Entrada para registrar o actualizar mantenimiento de sala. |
| `MaintenanceResponse.java` | Respuesta con información de mantenimiento. |
| `RoomResponse.java` | Datos públicos/administrativos de una sala. |
| `RoomScheduleInput.java` | Entrada para definir horario de una sala. |
| `RoomScheduleResponse.java` | Respuesta de horario configurado para una sala. |
| `RoomUpsertRequest.java` | Entrada para crear o editar salas. |
| `NotificationChannelPreference.java` | Preferencia por canal de notificación. |
| `NotificationPreferencesResponse.java` | Preferencias actuales de notificación del usuario. |
| `NotificationPreferencesUpdateRequest.java` | Entrada para actualizar preferencias de notificación. |
| `UserLookupResponse.java` | Resultado reducido de búsqueda de usuario. |
| `UserResponse.java` | Representación de usuario para vistas administrativas. |
| `UserStatusUpdateRequest.java` | Entrada para cambiar estado de usuario. |

**🚨 Excepciones**

| Archivo | Descripción breve |
|---|---|
| `BusinessException.java` | Excepción para reglas de negocio inválidas. |
| `GlobalExceptionHandler.java` | Centraliza la traducción de errores Java a respuestas HTTP. |
| `NotFoundException.java` | Excepción para recursos no encontrados. |

**🗃️ Modelos**

| Archivo | Descripción breve |
|---|---|
| `AuditLogEntity.java` | Entidad para trazabilidad de acciones relevantes. |
| `CampusScheduleEntity.java` | Entidad de horarios generales del campus. |
| `EmailOutboxEntity.java` | Entidad de cola de correos pendientes o enviados. |
| `EmailStatus.java` | Enum de estados del correo en la cola. |
| `LoginAttemptEntity.java` | Entidad para intentos de inicio de sesión. |
| `MaintenanceEntity.java` | Entidad de mantenimientos programados para salas. |
| `MaintenanceStatus.java` | Enum de estados de mantenimiento. |
| `NotificationPreferenceEntity.java` | Entidad de preferencias de notificación por usuario. |
| `PabellonEntity.java` | Entidad que representa pabellones del campus. |
| `PasswordResetEntity.java` | Entidad de tokens o solicitudes de recuperación de contraseña. |
| `ReservationEntity.java` | Entidad principal de reservas. |
| `ReservationStatus.java` | Enum de estados de reserva. |
| `RoleEntity.java` | Entidad de roles del sistema. |
| `RoleName.java` | Enum con nombres de roles permitidos. |
| `RoomEntity.java` | Entidad de salas reservables. |
| `RoomScheduleEntity.java` | Entidad de horarios específicos por sala. |
| `RoomState.java` | Enum de estados operativos de sala. |
| `SystemConfigEntity.java` | Entidad de configuración global del sistema. |
| `TwoFactorCodeEntity.java` | Entidad de códigos 2FA emitidos. |
| `UserEntity.java` | Entidad de usuarios. |
| `UserStatus.java` | Enum de estados de usuario. |

**🧱 Repositories**

| Archivo | Descripción breve |
|---|---|
| `AuditLogRepository.java` | Acceso a registros de auditoría. |
| `CampusScheduleRepository.java` | Consultas de horarios de campus. |
| `EmailOutboxRepository.java` | Consultas y actualización de la cola de emails. |
| `LoginAttemptRepository.java` | Persistencia de intentos de login. |
| `MaintenanceRepository.java` | Acceso a mantenimientos de salas. |
| `NotificationPreferenceRepository.java` | Acceso a preferencias de notificación. |
| `PabellonRepository.java` | Acceso a pabellones. |
| `PasswordResetRepository.java` | Acceso a solicitudes de recuperación de contraseña. |
| `ReservationRepository.java` | Consultas de reservas, disponibilidad y conflictos. |
| `RoleRepository.java` | Acceso a roles. |
| `RoomRepository.java` | Consultas y persistencia de salas. |
| `RoomScheduleRepository.java` | Acceso a horarios de sala. |
| `SystemConfigRepository.java` | Acceso a configuración global. |
| `TwoFactorCodeRepository.java` | Acceso a códigos 2FA. |
| `UserRepository.java` | Consultas y persistencia de usuarios. |

**🛡️ Seguridad**

| Archivo | Descripción breve |
|---|---|
| `AuthCookieService.java` | Crea, limpia y configura cookies de autenticación. |
| `AuthPrincipal.java` | Representa al usuario autenticado dentro de Spring Security. |
| `CurrentUserProvider.java` | Obtiene el usuario actual desde el contexto de seguridad. |
| `JwtAuthenticationFilter.java` | Lee y valida JWT en cada request protegido. |
| `JwtService.java` | Genera, firma y valida tokens JWT. |
| `SecretHashService.java` | Aplica hashing seguro a secretos como códigos o tokens. |

**🧠 Services**

| Archivo | Descripción breve |
|---|---|
| `AccessGuard.java` | Valida permisos y restricciones de acceso entre roles/usuarios. |
| `AuditService.java` | Registra acciones relevantes para auditoría. |
| `AuthService.java` | Orquesta login, sesión, 2FA, recuperación y reglas de seguridad. |
| `BookingService.java` | Implementa la lógica central de reservas y disponibilidad. |
| `CampusMapService.java` | Construye la información del mapa de campus y pabellones. |
| `DtoMapper.java` | Convierte entidades JPA en DTOs de respuesta. |
| `EmailOutboxService.java` | Gestiona la cola de correos y sus reintentos/envíos. |
| `PreferenceService.java` | Administra preferencias de notificación de usuarios. |
| `RoomCatalogTranslator.java` | Traduce datos del catálogo de salas a formatos de respuesta. |
| `RoomScheduleService.java` | Valida y administra horarios de salas. |
| `RoomService.java` | Implementa reglas de creación, edición, estado y mantenimiento de salas. |
| `SystemConfigService.java` | Lee y actualiza configuración global del sistema. |
| `UserService.java` | Gestiona usuarios, estados y consultas administrativas. |

**🔐 Estrategias de autenticación**

| Archivo | Descripción breve |
|---|---|
| `LoginStrategy.java` | Contrato común para estrategias de login. |
| `StandardLoginStrategy.java` | Implementa login estándar con credenciales. |
| `TwoFactorLoginStrategy.java` | Implementa el flujo de login que requiere segundo factor. |

**⏰ Comandos y reglas de reserva**

| Archivo | Descripción breve |
|---|---|
| `BookingReminderCommand.java` | Contrato para comandos de recordatorio de reservas. |
| `BookingReminderScheduler.java` | Programa la ejecución periódica de recordatorios. |
| `SendEndingSoonReservationReminderCommand.java` | Envía avisos de reservas próximas a terminar. |
| `SendUpcomingReservationReminderCommand.java` | Envía avisos de reservas próximas a iniciar. |
| `BookingRuleContext.java` | Contexto compartido para validar una reserva. |
| `BookingValidationRule.java` | Contrato de regla de validación de reserva. |
| `BookingWindowAndSlotRule.java` | Valida ventana permitida y bloques horarios. |
| `CapacityRule.java` | Valida capacidad solicitada contra la sala. |
| `EndTimeAfterStartRule.java` | Valida que la hora de fin sea posterior al inicio. |
| `MaxActiveBookingsRule.java` | Limita reservas activas simultáneas por usuario. |
| `MaxDurationRule.java` | Valida duración máxima de una reserva. |
| `RoomAvailabilityRule.java` | Valida disponibilidad de la sala y conflictos. |

**✉️ Email y factories**

| Archivo | Descripción breve |
|---|---|
| `EmailTemplateService.java` | Construye contenido de emails del sistema. |
| `EmailGateway.java` | Contrato para proveedores de envío de correo. |
| `EmailGatewayFactory.java` | Selecciona el gateway de email configurado. |
| `GmailEmailGateway.java` | Envía correos usando Gmail/SMTP. |
| `LogEmailGateway.java` | Simula envío registrando correos en logs. |
| `ResendEmailGateway.java` | Envía correos usando Resend. |
| `MaintenanceFactory.java` | Crea entidades de mantenimiento consistentes. |
| `ReservationFactory.java` | Crea entidades de reserva con valores iniciales correctos. |
| `RoomFactory.java` | Crea entidades de sala. |
| `SecurityEntityFactory.java` | Crea entidades relacionadas con seguridad, tokens y 2FA. |
| `CalendarUtils.java` | Genera contenido de calendario para integraciones `.ics`. |

**🧪 Tests Java**

| Archivo | Descripción breve |
|---|---|
| `ReservasApplicationTests.java` | Verifica que el contexto de Spring Boot cargue correctamente. |
| `JwtServiceTest.java` | Prueba generación y validación de tokens JWT. |
| `SecurityEntityFactoryTest.java` | Prueba la creación de entidades de seguridad. |

### ⚛️ Frontend: detalle de archivos TS/TSX/CSS

| Archivo | Descripción breve |
|---|---|
| `index.html` | HTML base usado por Vite para montar la aplicación React. |
| `vite.config.ts` | Configura Vite, plugins y comportamiento del build/dev server. |
| `App.tsx` | Componente raíz; conecta rutas, sesión, layout y pantallas principales. |
| `main.tsx` | Punto de entrada que monta React en el DOM. |
| `App.css` | Estilos específicos del componente principal y ajustes visuales de la app. |
| `index.css` | Estilos globales, base visual y directivas de Tailwind. |
| `models/types.ts` | Tipos TypeScript compartidos para usuarios, salas, reservas y respuestas. |
| `services/api.ts` | Cliente centralizado para consumir endpoints del backend. |
| `utils/helpers.ts` | Funciones auxiliares reutilizables para formato, fechas o datos de UI. |
| `viewmodels/routes.ts` | Definición de rutas y metadatos de navegación. |
| `views/components/filters/FilterBar.tsx` | Barra de filtros para buscar o refinar listados. |
| `views/components/layout/AppHeader.tsx` | Encabezado contextual de la aplicación. |
| `views/components/layout/GlobalTopbar.tsx` | Barra superior global con navegación y acciones de sesión. |
| `views/components/layout/Subnav.tsx` | Navegación secundaria entre secciones relacionadas. |
| `views/components/modals/BookingSuccessModal.tsx` | Modal de confirmación al crear una reserva. |
| `views/components/modals/ConfirmCancelBookingModal.tsx` | Modal para confirmar cancelación de reserva. |
| `views/components/modals/DeleteRoomModal.tsx` | Modal para confirmar eliminación o baja de sala. |
| `views/components/modals/EditBookingModal.tsx` | Modal para editar datos de una reserva. |
| `views/components/modals/ForgotPasswordModal.tsx` | Modal para iniciar recuperación de contraseña. |
| `views/components/modals/MessageModal.tsx` | Modal genérico para mensajes de éxito, error o aviso. |
| `views/components/modals/RoomFormModal.tsx` | Modal con formulario de creación/edición de sala. |
| `views/components/modals/RoomSuccessModal.tsx` | Modal de confirmación para operaciones de sala. |
| `views/components/modals/TwoFactorModal.tsx` | Modal para ingresar o validar códigos 2FA. |
| `views/pages/AdminReservasPage.tsx` | Pantalla administrativa para supervisar reservas. |
| `views/pages/LoginPage.tsx` | Pantalla de inicio de sesión y acceso al sistema. |
| `views/pages/MainPage.tsx` | Pantalla principal o dashboard inicial. |
| `views/pages/MisReservasPage.tsx` | Pantalla de reservas del usuario autenticado. |
| `views/pages/PerfilesPage.tsx` | Pantalla de administración o consulta de perfiles. |
| `views/pages/ReservasPage.tsx` | Pantalla para buscar disponibilidad y reservar salas. |
| `views/pages/ResetPasswordPage.tsx` | Pantalla para completar restablecimiento de contraseña. |
| `views/pages/SalasPage.tsx` | Pantalla de catálogo y administración de salas. |

---

> Documentación completa en la carpeta [`/docs`](./docs/)

## Arranque local con `.env` en raiz

1. Copia `.env.example` a `.env` y ajusta valores.
2. Base de datos:
   - Crear `luistudio_db`.
   - Ejecutar `database/001_init.sql`.
3. Levantar backend:
   - `powershell -ExecutionPolicy Bypass -File scripts/start-backend.ps1`
4. Levantar frontend:
   - `powershell -ExecutionPolicy Bypass -File scripts/start-frontend.ps1`
