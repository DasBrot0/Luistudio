# Aplicacion de Patrones en Luistudio

## Objetivo

Este documento resume los patrones que se aplican en el backend y su ubicación principal en código.

## Strategy

El login usa Strategy con un contexto explícito:

- `backend/reservas/src/main/java/com/luistudio/reservas/service/auth/strategy/LoginContext.java`
- `backend/reservas/src/main/java/com/luistudio/reservas/service/auth/strategy/LoginStrategy.java`
- `backend/reservas/src/main/java/com/luistudio/reservas/service/auth/strategy/StandardLoginStrategy.java`
- `backend/reservas/src/main/java/com/luistudio/reservas/service/auth/strategy/TwoFactorLoginStrategy.java`

`LoginService` elige la estrategia seg?n el usuario tenga 2FA activo y la asigna al `LoginContext` mediante `setLoginStrategy(...)`.

## Adapter

El env?o de correo usa Adapter porque el sistema habla con el Target `EmailGateway`, mientras Gmail y Resend tienen APIs externas distintas:

- Target:
  - `backend/reservas/src/main/java/com/luistudio/reservas/service/email/gateway/EmailGateway.java`
- Adapters:
  - `backend/reservas/src/main/java/com/luistudio/reservas/service/email/gateway/ResendEmailAdapter.java`
  - `backend/reservas/src/main/java/com/luistudio/reservas/service/email/gateway/GmailEmailAdapter.java`
- Adaptees:
  - `backend/reservas/src/main/java/com/luistudio/reservas/service/email/gateway/adaptee/ResendClientAdaptee.java`
  - `backend/reservas/src/main/java/com/luistudio/reservas/service/email/gateway/adaptee/GmailClientAdaptee.java`

`LogEmailGateway` se mantiene c?mo fallback local o mock, no c?mo adapter externo. `EmailGatewayResolver` solo resuelve el proveedor configurado.

## Command

Los recordatorios programados usan Command con manager y cola:

- `backend/reservas/src/main/java/com/luistudio/reservas/service/booking/command/BookingReminderCommand.java`
- `backend/reservas/src/main/java/com/luistudio/reservas/service/booking/command/BookingReminderCommandManager.java`
- `backend/reservas/src/main/java/com/luistudio/reservas/service/booking/command/SendUpcomingReservationReminderCommand.java`
- `backend/reservas/src/main/java/com/luistudio/reservas/service/booking/command/SendEndingSoonReservationReminderCommand.java`
- `backend/reservas/src/main/java/com/luistudio/reservas/service/booking/command/BookingReminderScheduler.java`

El scheduler agrega comandos al manager; el manager ejecuta los comandos pendientes. Cada tarea queda encapsulada c?mo objeto ejecutable.

## Facade

`AuthService` es la fachada del m?dulo de autenticación:

- `backend/reservas/src/main/java/com/luistudio/reservas/service/AuthService.java`
- `backend/reservas/src/main/java/com/luistudio/reservas/service/auth/LoginService.java`
- `backend/reservas/src/main/java/com/luistudio/reservas/service/auth/LoginAttemptService.java`
- `backend/reservas/src/main/java/com/luistudio/reservas/service/auth/PasswordResetService.java`
- `backend/reservas/src/main/java/com/luistudio/reservas/service/auth/TwoFactorService.java`

`AuthController` se comunica con `AuthService`, y la fachada delega internamente en los servicios especializados.
