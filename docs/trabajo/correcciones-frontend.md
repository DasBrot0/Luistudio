## Sprint 1 — Autenticación y reservas base

**🔴 Formulario de reserva sin feedback de disponibilidad visible**
El botón "Ver disponibilidad" no tiene un mockup que muestre el resultado. Falta una grilla o timeline que muestre horas ocupadas y libres antes de confirmar.

**🔴 No hay navegación global**
Ninguna pantalla tiene sidebar ni topbar persistente. El usuario no tiene forma de navegar entre secciones sin memorizar URLs. Debería definirse desde el Sprint 1 porque afecta todas las pantallas posteriores.

**🟠 El modal de restablecer contraseña necesita overlay**
Aparece flotando sobre el login sin fondo oscuro detrás. Debería tener un overlay semitransparente o ser una pantalla separada.

**🟡 El calendario no diferencia días disponibles de ocupados**
Los días del calendar picker se ven todos iguales. Con colorear en rojo/gris los días sin disponibilidad y en verde los disponibles se ahorran muchos clics innecesarios.

**🟡 "Limpiar" y "Confirmar" tienen el mismo peso visual y están muy juntos**
Un clic accidental en "Limpiar" borra todo el formulario. "Limpiar" debería ser un link de texto y "Confirmar" el único botón primario destacado.

**🟡 El campo "Personas" no está conectado a la capacidad de la sala**
El usuario escribe un número pero no ve si la sala escogida tiene esa capacidad. Debería mostrarse la capacidad junto al selector de sala o filtrar automáticamente las salas disponibles según ese valor.

---

## Sprint 2 — Salas y gestión básica

**🔴 Botones "Eliminar" sin modal de confirmación**
Eliminar sala es irreversible pero no hay mockup de confirmación. Solo "Inhabilitar usuario" lo tiene; Eliminar debería tenerlo también.

**🟡 Falta estado vacío en "Mis reservas"**
No hay diseño para cuando el estudiante no tiene reservas todavía. Debería haber un mensaje claro y un botón CTA ("Hacer mi primera reserva").

---

## Sprint 3 — Usuarios, seguridad y notificaciones admin

**🔴 Las notificaciones in-app del estudiante muestran datos del registro de seguridad**
En varias pantallas el bloque "Usuario: LuisTravieso / IP: 181.65.10.2 / Evento: Cuenta bloqueada" aparece dentro de notificaciones destinadas al estudiante. Ese bloque es del admin y no debería estar ahí.

**🟠 "Mis reservas" no diferencia estados visualmente**
Confirmado y Cancelado usan solo texto. Con un color o ícono por estado (verde/rojo/gris) el usuario identificaría el estado de cada reserva de un vistazo sin leer cada fila.

**🟠 Gestión de usuarios sin paginación**
La pantalla de perfiles tiene paginador pero la de Gestión de Usuarios no. Con muchos usuarios eso es un problema operativo para el admin.

**🟠 Auditoría demasiado básica**
Solo lista dos entradas sin filtros de fecha, tipo de acción ni búsqueda por usuario. Debería tener al menos los mismos filtros que el Registro de Seguridad.

---

## Sprint 4 — Mantenimiento, 2FA y configuración

**🟠 Pantalla de Configuración de Seguridad sin acciones claras**
Muestra dos toggles en "Activado" pero no hay botón para guardar ni para cambiarlos. Si es solo informativa debería llamarse "Políticas de seguridad"; si es editable, necesita botón de guardar y toggle funcional.

**🟡 Centro de notificaciones ausente**
Las notificaciones aparecen como popups individuales pero no hay historial. Con la cantidad de eventos que genera el sistema (cancelaciones, mantenimiento, modificaciones), hace falta un ícono de campana en la barra superior con bandeja de notificaciones.

---

## Sprint 5 — Perfil de usuario

**🟡 Mi Perfil tiene avatar decorativo sin acción**
Hay un ícono de foto de perfil pero ningún botón para cambiarla. Si no es editable debería quitarse el ícono para no generar expectativa; si sí lo es, falta el botón de cámara/editar encima del avatar.
