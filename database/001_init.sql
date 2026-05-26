-- Luistudio - esquema inicial PostgreSQL
-- Ejecutar en una base existente (ej. luistudio_db)

BEGIN;

CREATE TABLE IF NOT EXISTS rol (
  id BIGSERIAL PRIMARY KEY,
  nombre VARCHAR(50) NOT NULL UNIQUE,
  descripcion VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS permiso (
  id BIGSERIAL PRIMARY KEY,
  codigo VARCHAR(80) NOT NULL UNIQUE,
  descripcion VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS rol_permiso (
  rol_id BIGINT NOT NULL,
  permiso_id BIGINT NOT NULL,
  PRIMARY KEY (rol_id, permiso_id),
  CONSTRAINT fk_rol_permiso_rol FOREIGN KEY (rol_id) REFERENCES rol(id),
  CONSTRAINT fk_rol_permiso_permiso FOREIGN KEY (permiso_id) REFERENCES permiso(id)
);

CREATE TABLE IF NOT EXISTS usuario (
  id BIGSERIAL PRIMARY KEY,
  rol_id BIGINT NOT NULL,
  codigo VARCHAR(20) NOT NULL UNIQUE,
  nombres VARCHAR(120) NOT NULL,
  apellidos VARCHAR(120) NOT NULL,
  correo VARCHAR(160) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  estado VARCHAR(20) NOT NULL DEFAULT 'HABILITADO',
  locked_until TIMESTAMPTZ,
  has_2fa BOOLEAN NOT NULL DEFAULT FALSE,
  creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT fk_usuario_rol FOREIGN KEY (rol_id) REFERENCES rol(id),
  CONSTRAINT chk_usuario_estado CHECK (estado IN ('HABILITADO', 'DESHABILITADO'))
);

CREATE TABLE IF NOT EXISTS system_config (
  id BIGSERIAL PRIMARY KEY,
  clave VARCHAR(100) NOT NULL UNIQUE,
  valor VARCHAR(255) NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS intento_login (
  id BIGSERIAL PRIMARY KEY,
  usuario_id BIGINT NOT NULL,
  fecha_intento TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  exito BOOLEAN NOT NULL DEFAULT FALSE,
  ip_origen VARCHAR(64),
  CONSTRAINT fk_intento_login_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id)
);

CREATE TABLE IF NOT EXISTS doble_factor (
  id BIGSERIAL PRIMARY KEY,
  usuario_id BIGINT NOT NULL UNIQUE,
  habilitado BOOLEAN NOT NULL DEFAULT FALSE,
  metodo VARCHAR(30),
  secreto VARCHAR(255),
  actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT fk_doble_factor_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id)
);

CREATE TABLE IF NOT EXISTS recuperacion_contra (
  id BIGSERIAL PRIMARY KEY,
  usuario_id BIGINT NOT NULL,
  token VARCHAR(255) NOT NULL UNIQUE,
  expira_en TIMESTAMPTZ NOT NULL,
  usado BOOLEAN NOT NULL DEFAULT FALSE,
  creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT fk_recuperacion_contra_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id)
);

CREATE TABLE IF NOT EXISTS pabellon (
  id BIGSERIAL PRIMARY KEY,
  codigo VARCHAR(20) NOT NULL UNIQUE,
  nombre VARCHAR(120) NOT NULL,
  ubicacion VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS sala (
  id BIGSERIAL PRIMARY KEY,
  pabellon_id BIGINT NOT NULL,
  codigo VARCHAR(20) NOT NULL UNIQUE,
  nombre VARCHAR(120) NOT NULL,
  capacidad INTEGER NOT NULL,
  ubicacion VARCHAR(120) NOT NULL,
  estado VARCHAR(30) NOT NULL DEFAULT 'DISPONIBLE',
  creada_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT fk_sala_pabellon FOREIGN KEY (pabellon_id) REFERENCES pabellon(id),
  CONSTRAINT chk_sala_capacidad CHECK (capacidad > 0),
  CONSTRAINT chk_sala_estado CHECK (estado IN ('DISPONIBLE', 'EN_MANTENIMIENTO', 'INACTIVA'))
);

CREATE TABLE IF NOT EXISTS mantenimiento (
  id BIGSERIAL PRIMARY KEY,
  sala_id BIGINT NOT NULL,
  inicio TIMESTAMPTZ NOT NULL,
  fin TIMESTAMPTZ NOT NULL,
  motivo VARCHAR(255) NOT NULL,
  estado VARCHAR(20) NOT NULL DEFAULT 'PROGRAMADO',
  creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT fk_mantenimiento_sala FOREIGN KEY (sala_id) REFERENCES sala(id),
  CONSTRAINT chk_mantenimiento_rango CHECK (fin > inicio),
  CONSTRAINT chk_mantenimiento_estado CHECK (estado IN ('PROGRAMADO', 'EN_CURSO', 'FINALIZADO', 'CANCELADO'))
);

CREATE TABLE IF NOT EXISTS reserva (
  id BIGSERIAL PRIMARY KEY,
  usuario_id BIGINT NOT NULL,
  sala_id BIGINT NOT NULL,
  fecha DATE NOT NULL,
  hora_inicio TIME NOT NULL,
  hora_fin TIME NOT NULL,
  estado VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
  cantidad_personas INTEGER NOT NULL,
  observacion VARCHAR(255),
  creada_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  actualizada_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT fk_reserva_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id),
  CONSTRAINT fk_reserva_sala FOREIGN KEY (sala_id) REFERENCES sala(id),
  CONSTRAINT chk_reserva_horas CHECK (hora_fin > hora_inicio),
  CONSTRAINT chk_reserva_estado CHECK (estado IN ('ACTIVA', 'CANCELADA', 'COMPLETADA')),
  CONSTRAINT chk_reserva_personas CHECK (cantidad_personas > 0)
);

CREATE TABLE IF NOT EXISTS preferencia_notificacion (
  id BIGSERIAL PRIMARY KEY,
  usuario_id BIGINT NOT NULL UNIQUE,
  email_habilitado BOOLEAN NOT NULL DEFAULT TRUE,
  recordatorio_habilitado BOOLEAN NOT NULL DEFAULT TRUE,
  cambios_reserva_habilitado BOOLEAN NOT NULL DEFAULT TRUE,
  actualizado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT fk_preferencia_notificacion_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id)
);

CREATE TABLE IF NOT EXISTS registro_notificacion (
  id BIGSERIAL PRIMARY KEY,
  usuario_id BIGINT NOT NULL,
  tipo VARCHAR(40) NOT NULL,
  asunto VARCHAR(160) NOT NULL,
  contenido TEXT NOT NULL,
  enviado_en TIMESTAMPTZ,
  estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
  CONSTRAINT fk_registro_notificacion_usuario FOREIGN KEY (usuario_id) REFERENCES usuario(id),
  CONSTRAINT chk_registro_notificacion_estado CHECK (estado IN ('PENDIENTE', 'ENVIADO', 'ERROR'))
);

CREATE TABLE IF NOT EXISTS email_outbox (
  id BIGSERIAL PRIMARY KEY,
  destinatario VARCHAR(160) NOT NULL,
  asunto VARCHAR(160) NOT NULL,
  cuerpo TEXT NOT NULL,
  payload JSONB,
  estado VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
  intentos INTEGER NOT NULL DEFAULT 0,
  disponible_desde TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  enviado_en TIMESTAMPTZ,
  error_detalle TEXT,
  CONSTRAINT chk_email_outbox_estado CHECK (estado IN ('PENDIENTE', 'ENVIADO', 'ERROR'))
);

CREATE TABLE IF NOT EXISTS audit_log (
  id BIGSERIAL PRIMARY KEY,
  actor_usuario_id BIGINT,
  accion VARCHAR(80) NOT NULL,
  entidad VARCHAR(80) NOT NULL,
  entidad_id VARCHAR(80),
  detalle JSONB,
  creado_en TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT fk_audit_log_actor FOREIGN KEY (actor_usuario_id) REFERENCES usuario(id)
);

CREATE TABLE IF NOT EXISTS two_factor_codes (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL,
  code VARCHAR(10) NOT NULL,
  expira_at TIMESTAMPTZ NOT NULL,
  usado BOOLEAN NOT NULL DEFAULT FALSE,
  CONSTRAINT fk_two_factor_code_user FOREIGN KEY (user_id) REFERENCES usuario(id)
);

CREATE INDEX IF NOT EXISTS idx_sala_ubicacion ON sala (ubicacion);
CREATE INDEX IF NOT EXISTS idx_reserva_usuario_fecha ON reserva (usuario_id, fecha);
CREATE INDEX IF NOT EXISTS idx_reserva_sala_fecha_horas ON reserva (sala_id, fecha, hora_inicio, hora_fin);
CREATE INDEX IF NOT EXISTS idx_intento_login_usuario_fecha ON intento_login (usuario_id, fecha_intento);
CREATE INDEX IF NOT EXISTS idx_audit_log_entidad_entidad_id ON audit_log (entidad, entidad_id);

INSERT INTO rol (nombre, descripcion)
VALUES
  ('ADMIN', 'Administrador del sistema'),
  ('ESTUDIANTE', 'Usuario estudiante')
ON CONFLICT (nombre) DO NOTHING;

INSERT INTO permiso (codigo, descripcion)
VALUES
  ('RESERVA_CREAR', 'Crear reservas'),
  ('RESERVA_EDITAR', 'Editar reservas'),
  ('RESERVA_CANCELAR', 'Cancelar reservas'),
  ('SALA_GESTIONAR', 'Gestionar salas'),
  ('USUARIO_GESTIONAR', 'Gestionar usuarios')
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id
FROM rol r
CROSS JOIN permiso p
WHERE r.nombre = 'ADMIN'
ON CONFLICT DO NOTHING;

INSERT INTO system_config (clave, valor)
VALUES
  ('max_reservas_simultaneas', '1'),
  ('duracion_maxima_minutos', '120')
ON CONFLICT (clave) DO NOTHING;

INSERT INTO rol_permiso (rol_id, permiso_id)
SELECT r.id, p.id
FROM rol r
JOIN permiso p ON p.codigo IN ('RESERVA_CREAR', 'RESERVA_EDITAR', 'RESERVA_CANCELAR')
WHERE r.nombre = 'ESTUDIANTE'
ON CONFLICT DO NOTHING;

COMMIT;
