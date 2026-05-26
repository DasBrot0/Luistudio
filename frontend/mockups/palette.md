## Paleta principal

| Token             | Color          | Hex           | Uso                                         |
| ----------------- | -------------- | ------------- | ------------------------------------------- |
| `primary`       | Azul índigo   | `#2D2F8F`   | Fondo sidebar, botones primarios            |
| `primary-dark`  | Índigo oscuro | `#21236B`   | Hover de botones primarios, header          |
| `primary-light` | Índigo suave  | `#3D3FA8`   | Estados activos en sidebar                  |
| `accent`        | Blanco         | `#FFFFFF`   | Texto sobre fondo primario, íconos activos |
| `accent-muted`  | Blanco 60%     | `#FFFFFF99` | Íconos inactivos en sidebar                |

---

## Superficie y fondos

| Token          | Hex           | Uso                                              |
| -------------- | ------------- | ------------------------------------------------ |
| `bg-base`    | `#F4F5FA`   | Fondo general de la app                          |
| `bg-card`    | `#FFFFFF`   | Tarjetas, modals, tablas                         |
| `bg-sidebar` | `#2D2F8F`   | Sidebar colapsado y expandido                    |
| `bg-active`  | `#FFFFFF1A` | Item activo en sidebar (blanco 10% transparente) |

---

## Estados y semántica

| Token       | Hex         | Uso                                    |
| ----------- | ----------- | -------------------------------------- |
| `success` | `#22C55E` | Reserva confirmada, usuario habilitado |
| `warning` | `#F59E0B` | Mantenimiento, advertencias            |
| `danger`  | `#EF4444` | Cancelar, eliminar, cuenta bloqueada   |
| `info`    | `#3B82F6` | Notificaciones informativas            |
| `neutral` | `#6B7280` | Texto secundario, etiquetas inactivas  |

---

## Texto

| Token               | Hex         | Uso                               |
| ------------------- | ----------- | --------------------------------- |
| `text-primary`    | `#111827` | Títulos y body principal         |
| `text-secondary`  | `#6B7280` | Labels, subtítulos, placeholders |
| `text-on-primary` | `#FFFFFF` | Texto sobre botones/fondo índigo |
| `text-link`       | `#2D2F8F` | Links y acciones de texto         |

---

## Botones

| Variante  | Fondo           | Texto       | Hover       |
| --------- | --------------- | ----------- | ----------- |
| Primary   | `#2D2F8F`     | `#FFFFFF` | `#21236B` |
| Secondary | `#FFFFFF`     | `#2D2F8F` | `#F4F5FA` |
| Danger    | `#EF4444`     | `#FFFFFF` | `#DC2626` |
| Ghost     | `transparent` | `#2D2F8F` | `#F4F5FA` |
| Disabled  | `#E5E7EB`     | `#9CA3AF` | —          |

---

En Tailwind puedes registrar todo esto en `tailwind.config.js` bajo `theme.extend.colors` con los nombres de token, así usas clases como `bg-primary`, `text-on-primary`, `bg-danger` en lugar de hardcodear hexadecimales por toda la app.
