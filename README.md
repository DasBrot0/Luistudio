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

---

> Documentación completa en la carpeta [`/docs`](./docs/)
