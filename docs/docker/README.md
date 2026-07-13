# Guía Docker

Esta guía permite ejecutar Luistudio en cualquier PC que tenga Docker y Docker Compose, sin instalar Java, Maven, Node.js, PostgreSQL ni pgAdmin.

## Servicios incluidos

| Servicio | Dirección por defecto | Propósito |
|---|---|---|
| Aplicación web | `http://localhost` | Frontend React/Nginx |
| API | `http://localhost:8080/api` | Backend Spring Boot |
| pgAdmin | `http://localhost:5050` | Administración web de PostgreSQL |
| PostgreSQL | `localhost:5432` | Base de datos |

## Primer arranque

1. Descarga o clona el proyecto completo.
2. Abre una terminal en la carpeta raíz del proyecto, donde está `docker-compose.yml`.
3. Opcional pero recomendado: copia `.env.example` como `.env` y cambia las contraseñas y `JWT_SECRET`.
4. Ejecuta:

```bash
docker compose up --build
```

Docker descargará las imágenes necesarias, compilará el backend y frontend, y creará la base de datos. El primer inicio puede tardar varios minutos; los siguientes son más rápidos.

Cuando aparezcan los logs de los servicios, abre `http://localhost` en el navegador.

Para ejecutarlo en segundo plano, usa:

```bash
docker compose up --build -d
```

## Acceso a la base de datos

pgAdmin ya forma parte de los contenedores. Abre `http://localhost:5050` e inicia sesión con los valores predeterminados:

- Correo: `admin@luistudio.com`
- Contraseña: `admin123`

El servidor **Luistudio PostgreSQL** aparece registrado automáticamente. Su contraseña predeterminada es `luistudio_dev_password`.

Para un pgAdmin instalado fuera de Docker, usa estos datos:

| Campo | Valor por defecto |
|---|---|
| Host | `localhost` |
| Puerto | `5432` |
| Base de datos | `luistudio_db` |
| Usuario | `luistudio` |
| Contraseña | `luistudio_dev_password` |

Desde otra PC de la misma red, reemplaza `localhost` por la IP o el nombre de la PC que ejecuta Docker. Revisa que el firewall permita los puertos requeridos.

## Cuando se modifica código

Después de modificar el frontend o backend, reconstruye el servicio correspondiente. Los datos de PostgreSQL se conservan.

```bash
# Reconstruir todo el proyecto
docker compose up --build

# Reconstruir solo el frontend
docker compose up --build frontend

# Reconstruir solo el backend
docker compose up --build backend
```

Si los servicios están en segundo plano, puedes usar los mismos comandos con `-d`:

```bash
docker compose up --build -d
```

Los cambios en los archivos SQL de `database/` no se aplican automáticamente a una base ya creada. Para conservar datos, ejecuta la migración nueva manualmente desde pgAdmin. Para reiniciar por completo la base de desarrollo, consulta la siguiente sección.

## Detener o reiniciar

```bash
# Detener contenedores y conservar la base de datos
docker compose down

# Volver a iniciarlos sin reconstruir imágenes
docker compose up

# Ver el estado de los servicios
docker compose ps

# Ver logs de un servicio, por ejemplo el backend
docker compose logs -f backend
```

## Reiniciar la base de datos

> Advertencia: este comando elimina permanentemente todos los datos de PostgreSQL y pgAdmin almacenados en Docker.

```bash
docker compose down -v
docker compose up --build
```

Al crear el volumen nuevamente se ejecutan automáticamente `001_init.sql` y `002_seed_release01.sql`. El script `003_drop_all_tables.sql` no se ejecuta automáticamente.

## Configuración y seguridad

Los puertos, credenciales y URLs se configuran en `.env`. Parte de `.env.example`:

```env
POSTGRES_PASSWORD=una-clave-segura
PGADMIN_DEFAULT_PASSWORD=otra-clave-segura
JWT_SECRET=una-clave-larga-y-aleatoria
```

No compartas ni subas el archivo `.env` con claves reales al repositorio. Para acceder desde otra PC, ajusta `CORS_ORIGINS`, `FRONTEND_RESET_PASSWORD_URL` y `FRONTEND_CONFIRM_CHANGE_URL` con la IP o dominio donde se publica la aplicación.
