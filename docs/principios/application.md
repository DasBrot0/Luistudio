# Aplicacion de Patrones en Luistudio

## Objetivo

Este documento resume los patrones aplicados en el proyecto y su ubicacion principal en codigo.

## Principios OO (guia de referencia)

### SRP (Single Responsibility Principle)

- Backend por capas:
  - `controller`: expone endpoints HTTP.
  - `service`: concentra reglas de negocio.
  - `repository`: acceso a datos.
- Ejemplos:
  - `backend/reservas/src/main/java/com/luistudio/reservas/controller/*`
  - `backend/reservas/src/main/java/com/luistudio/reservas/service/*`
  - `backend/reservas/src/main/java/com/luistudio/reservas/repository/*`

### DIP (Dependency Inversion Principle)

- Dependencias inyectadas por constructor en controladores y servicios.
- Ejemplos:
  - `backend/reservas/src/main/java/com/luistudio/reservas/service/BookingService.java`
  - `backend/reservas/src/main/java/com/luistudio/reservas/service/AuthService.java`

### DRY

- Mapeo centralizado entidad->DTO:
  - `backend/reservas/src/main/java/com/luistudio/reservas/service/DtoMapper.java`

## Patrones Creacionales

### Factory Method

- Creacion de entidades de reserva:
  - `backend/reservas/src/main/java/com/luistudio/reservas/service/factory/ReservationFactory.java`
- Creacion de entidades de sala y mantenimiento:
  - `backend/reservas/src/main/java/com/luistudio/reservas/service/factory/RoomFactory.java`
  - `backend/reservas/src/main/java/com/luistudio/reservas/service/factory/MaintenanceFactory.java`
- Creacion de entidades de seguridad (2FA, reset, intentos):
  - `backend/reservas/src/main/java/com/luistudio/reservas/service/factory/SecurityEntityFactory.java`
- Seleccion de gateway de correo:
  - `backend/reservas/src/main/java/com/luistudio/reservas/service/email/gateway/EmailGatewayFactory.java`

## Patrones Estructurales

### Adapter

- Abstraccion para envio de correo:
  - `backend/reservas/src/main/java/com/luistudio/reservas/service/email/gateway/EmailGateway.java`
- Adaptador concreto para Resend:
  - `backend/reservas/src/main/java/com/luistudio/reservas/service/email/gateway/ResendEmailGateway.java`
- Adaptador concreto para Gmail API:
  - `backend/reservas/src/main/java/com/luistudio/reservas/service/email/gateway/GmailEmailGateway.java`
- Adaptador de fallback por logs:
  - `backend/reservas/src/main/java/com/luistudio/reservas/service/email/gateway/LogEmailGateway.java`

## Patrones de Comportamiento

### Strategy

- Reglas de validacion de reservas (OCP):
  - `backend/reservas/src/main/java/com/luistudio/reservas/service/booking/rule/BookingValidationRule.java`
  - `backend/reservas/src/main/java/com/luistudio/reservas/service/booking/rule/*Rule.java`
  - Integracion: `backend/reservas/src/main/java/com/luistudio/reservas/service/BookingService.java`
- Estrategias de respuesta de login (2FA vs login estandar):
  - `backend/reservas/src/main/java/com/luistudio/reservas/service/auth/strategy/LoginStrategy.java`
  - `backend/reservas/src/main/java/com/luistudio/reservas/service/auth/strategy/TwoFactorLoginStrategy.java`
  - `backend/reservas/src/main/java/com/luistudio/reservas/service/auth/strategy/StandardLoginStrategy.java`

### Command

- Comandos para recordatorios programados:
  - `backend/reservas/src/main/java/com/luistudio/reservas/service/booking/command/BookingReminderCommand.java`
  - `backend/reservas/src/main/java/com/luistudio/reservas/service/booking/command/SendUpcomingReservationReminderCommand.java`
  - `backend/reservas/src/main/java/com/luistudio/reservas/service/booking/command/SendEndingSoonReservationReminderCommand.java`
  - Scheduler invocador: `backend/reservas/src/main/java/com/luistudio/reservas/service/booking/command/BookingReminderScheduler.java`

## Patrones de Spring usados en backend

- `Controller-Service-Repository`:
  - `controller`: entrada HTTP y contrato REST.
  - `service`: casos de uso y reglas de negocio.
  - `repository`: persistencia con Spring Data JPA.
- `Repository` (Spring Data JPA):
  - `backend/reservas/src/main/java/com/luistudio/reservas/repository/*`
- `Controller + Service`:
  - `backend/reservas/src/main/java/com/luistudio/reservas/controller/*`
  - `backend/reservas/src/main/java/com/luistudio/reservas/service/*`
- `DTO Mapper`:
  - `backend/reservas/src/main/java/com/luistudio/reservas/service/DtoMapper.java`
- `Global Exception Handler`:
  - `backend/reservas/src/main/java/com/luistudio/reservas/exception/GlobalExceptionHandler.java`

## Frontend (organizacion aplicada)

- Estructura separada por responsabilidades en `frontend/luistudio-app/src`:
  - `models/` (tipos de dominio)
  - `services/` (acceso a API)
  - `viewmodels/` (rutas y estado de presentacion)
  - `views/` (componentes y paginas)
