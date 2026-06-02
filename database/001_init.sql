-- Luistudio - esquema inicial PostgreSQL
-- Ejecutar en una base existente (ej. luistudio_db)

BEGIN;

CREATE TABLE IF NOT EXISTS roles (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(50) NOT NULL UNIQUE,
  description VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS permissions (
  id BIGSERIAL PRIMARY KEY,
  code VARCHAR(80) NOT NULL UNIQUE,
  description VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS role_permissions (
  role_id BIGINT NOT NULL,
  permission_id BIGINT NOT NULL,
  PRIMARY KEY (role_id, permission_id),
  CONSTRAINT fk_rol_permiso_rol FOREIGN KEY (role_id) REFERENCES roles(id),
  CONSTRAINT fk_rol_permiso_permiso FOREIGN KEY (permission_id) REFERENCES permissions(id)
);

CREATE TABLE IF NOT EXISTS users (
  id BIGSERIAL PRIMARY KEY,
  role_id BIGINT NOT NULL,
  code VARCHAR(20) NOT NULL UNIQUE,
  first_name VARCHAR(120) NOT NULL,
  last_name VARCHAR(120) NOT NULL,
  email VARCHAR(160) NOT NULL UNIQUE,
  password_hash VARCHAR(255) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'HABILITADO',
  locked_until TIMESTAMPTZ,
  has_2fa BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT fk_usuario_rol FOREIGN KEY (role_id) REFERENCES roles(id),
  CONSTRAINT chk_usuario_status CHECK (status IN ('HABILITADO', 'DESHABILITADO'))
);

CREATE TABLE IF NOT EXISTS system_config (
  id BIGSERIAL PRIMARY KEY,
  config_key VARCHAR(100) NOT NULL UNIQUE,
  config_value VARCHAR(255) NOT NULL,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS login_attempts (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL,
  attempted_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  success BOOLEAN NOT NULL DEFAULT FALSE,
  source_ip VARCHAR(64),
  CONSTRAINT fk_intento_login_usuario FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS two_factor_settings (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL UNIQUE,
  enabled BOOLEAN NOT NULL DEFAULT FALSE,
  method VARCHAR(30),
  secret VARCHAR(255),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT fk_doble_factor_usuario FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS password_resets (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL,
  token VARCHAR(255) NOT NULL UNIQUE,
  expires_at TIMESTAMPTZ NOT NULL,
  used BOOLEAN NOT NULL DEFAULT FALSE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT fk_recuperacion_contra_usuario FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS buildings (
  id BIGSERIAL PRIMARY KEY,
  code VARCHAR(20) NOT NULL UNIQUE,
  name VARCHAR(120) NOT NULL,
  location VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS rooms (
  id BIGSERIAL PRIMARY KEY,
  building_id BIGINT NOT NULL,
  code VARCHAR(20) NOT NULL UNIQUE,
  name VARCHAR(120) NOT NULL,
  capacity INTEGER NOT NULL,
  campus VARCHAR(120) NOT NULL,
  venue VARCHAR(160) NOT NULL,
  location VARCHAR(120) NOT NULL,
  min_people INTEGER NOT NULL DEFAULT 1,
  min_people_required BOOLEAN NOT NULL DEFAULT FALSE,
  max_people INTEGER NOT NULL DEFAULT 1,
  status VARCHAR(30) NOT NULL DEFAULT 'DISPONIBLE',
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT fk_sala_pabellon FOREIGN KEY (building_id) REFERENCES buildings(id),
  CONSTRAINT chk_sala_capacidad CHECK (capacity > 0),
  CONSTRAINT chk_sala_personas CHECK (
    min_people > 0
    AND max_people > 0
    AND max_people >= min_people
    AND max_people <= capacity
  ),
  CONSTRAINT chk_sala_estado CHECK (status IN ('DISPONIBLE', 'EN_MANTENIMIENTO', 'INACTIVA'))
);

CREATE TABLE IF NOT EXISTS campus_schedules (
  id BIGSERIAL PRIMARY KEY,
  campus VARCHAR(120) NOT NULL,
  day_of_week INTEGER NOT NULL,
  open_time TIME,
  close_time TIME,
  is_closed BOOLEAN NOT NULL DEFAULT FALSE,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT uq_campus_schedule UNIQUE (campus, day_of_week),
  CONSTRAINT chk_campus_day CHECK (day_of_week BETWEEN 1 AND 7),
  CONSTRAINT chk_campus_schedule_window CHECK (
    (is_closed = TRUE AND open_time IS NULL AND close_time IS NULL)
    OR
    (is_closed = FALSE AND open_time IS NOT NULL AND close_time IS NOT NULL AND close_time > open_time)
  )
);

CREATE TABLE IF NOT EXISTS room_schedules (
  id BIGSERIAL PRIMARY KEY,
  room_id BIGINT NOT NULL,
  day_of_week INTEGER NOT NULL,
  open_time TIME,
  close_time TIME,
  is_closed BOOLEAN NOT NULL DEFAULT FALSE,
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT fk_room_schedule_room FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE,
  CONSTRAINT uq_room_schedule UNIQUE (room_id, day_of_week),
  CONSTRAINT chk_room_day CHECK (day_of_week BETWEEN 1 AND 7),
  CONSTRAINT chk_room_schedule_window CHECK (
    (is_closed = TRUE AND open_time IS NULL AND close_time IS NULL)
    OR
    (is_closed = FALSE AND open_time IS NOT NULL AND close_time IS NOT NULL AND close_time > open_time)
  )
);

CREATE TABLE IF NOT EXISTS maintenances (
  id BIGSERIAL PRIMARY KEY,
  room_id BIGINT NOT NULL,
  start_at TIMESTAMPTZ NOT NULL,
  end_at TIMESTAMPTZ NOT NULL,
  reason VARCHAR(255) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PROGRAMADO',
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT fk_mantenimiento_sala FOREIGN KEY (room_id) REFERENCES rooms(id),
  CONSTRAINT chk_mantenimiento_rango CHECK (end_at > start_at),
  CONSTRAINT chk_mantenimiento_estado CHECK (status IN ('PROGRAMADO', 'EN_CURSO', 'FINALIZADO', 'CANCELADO'))
);

CREATE TABLE IF NOT EXISTS bookings (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL,
  room_id BIGINT NOT NULL,
  booking_date DATE NOT NULL,
  start_time TIME NOT NULL,
  end_time TIME NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVA',
  people_count INTEGER NOT NULL,
  note VARCHAR(255),
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_by BIGINT,
  CONSTRAINT fk_reserva_usuario FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT fk_reserva_sala FOREIGN KEY (room_id) REFERENCES rooms(id),
  CONSTRAINT chk_reserva_horas CHECK (end_time > start_time),
  CONSTRAINT chk_reserva_estado CHECK (status IN ('ACTIVA', 'CANCELADA', 'COMPLETADA')),
  CONSTRAINT chk_reserva_personas CHECK (people_count > 0)
);

CREATE TABLE IF NOT EXISTS notification_preferences (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL UNIQUE,
  email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  reminder_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  booking_changes_enabled BOOLEAN NOT NULL DEFAULT TRUE,
  notification_settings TEXT,
  theme_mode VARCHAR(10) NOT NULL DEFAULT 'LIGHT',
  font_scale DOUBLE PRECISION NOT NULL DEFAULT 1.0,
  login_landing_view VARCHAR(30) NOT NULL DEFAULT 'STUDENT_MY_BOOKINGS',
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT chk_preferencia_landing_view CHECK (
    login_landing_view IN ('STUDENT_MY_BOOKINGS', 'STUDENT_RESERVE', 'ADMIN_ROOMS', 'ADMIN_PROFILES', 'ADMIN_BOOKINGS')
  ),
  CONSTRAINT fk_preferencia_notificacion_usuario FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS notification_logs (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL,
  type VARCHAR(40) NOT NULL,
  subject VARCHAR(160) NOT NULL,
  content TEXT NOT NULL,
  sent_at TIMESTAMPTZ,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
  CONSTRAINT fk_registro_notificacion_usuario FOREIGN KEY (user_id) REFERENCES users(id),
  CONSTRAINT chk_registro_notificacion_estado CHECK (status IN ('PENDIENTE', 'ENVIADO', 'ERROR'))
);

CREATE TABLE IF NOT EXISTS email_outbox (
  id BIGSERIAL PRIMARY KEY,
  recipient VARCHAR(160) NOT NULL,
  subject VARCHAR(160) NOT NULL,
  body TEXT NOT NULL,
  payload JSONB,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDIENTE',
  attempts INTEGER NOT NULL DEFAULT 0,
  available_from TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  sent_at TIMESTAMPTZ,
  error_detail TEXT,
  CONSTRAINT chk_email_outbox_estado CHECK (status IN ('PENDIENTE', 'ENVIADO', 'ERROR'))
);

CREATE TABLE IF NOT EXISTS audit_log (
  id BIGSERIAL PRIMARY KEY,
  actor_user_id BIGINT,
  action VARCHAR(80) NOT NULL,
  entity VARCHAR(80) NOT NULL,
  entity_id VARCHAR(80),
  detail TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  CONSTRAINT fk_audit_log_actor FOREIGN KEY (actor_user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS two_factor_codes (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL,
  code VARCHAR(128) NOT NULL,
  expires_at TIMESTAMPTZ NOT NULL,
  used BOOLEAN NOT NULL DEFAULT FALSE,
  CONSTRAINT fk_two_factor_code_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_sala_ubicacion ON rooms (location);
CREATE INDEX IF NOT EXISTS idx_sala_campus ON rooms (campus);
CREATE INDEX IF NOT EXISTS idx_sala_venue ON rooms (venue);
CREATE INDEX IF NOT EXISTS idx_campus_schedule_campus_day ON campus_schedules (campus, day_of_week);
CREATE INDEX IF NOT EXISTS idx_room_schedule_room_day ON room_schedules (room_id, day_of_week);
CREATE INDEX IF NOT EXISTS idx_bookings_user_date ON bookings (user_id, booking_date);
CREATE INDEX IF NOT EXISTS idx_bookings_room_date_hours ON bookings (room_id, booking_date, start_time, end_time);
CREATE INDEX IF NOT EXISTS idx_intento_login_usuario_fecha ON login_attempts (user_id, attempted_at);
CREATE INDEX IF NOT EXISTS idx_audit_log_entidad_entidad_id ON audit_log (entity, entity_id);

INSERT INTO roles (name, description)
VALUES
  ('ADMIN', 'Administrador del sistema'),
  ('ESTUDIANTE', 'users estudiante')
ON CONFLICT (name) DO NOTHING;

INSERT INTO permissions (code, description)
VALUES
  ('RESERVA_CREAR', 'Crear reservas'),
  ('RESERVA_EDITAR', 'Editar reservas'),
  ('RESERVA_CANCELAR', 'Cancelar reservas'),
  ('SALA_GESTIONAR', 'Gestionar salas'),
  ('USUARIO_GESTIONAR', 'Gestionar usuarios')
ON CONFLICT (code) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
CROSS JOIN permissions p
WHERE r.name = 'ADMIN'
ON CONFLICT DO NOTHING;

INSERT INTO system_config (config_key, config_value)
VALUES
  ('max_reservas_simultaneas', '1'),
  ('duracion_maxima_minutos', '120'),
  ('campus_slot_minutos_monterrico', '60'),
  ('campus_slot_minutos_mayorazgo', '45')
ON CONFLICT (config_key) DO NOTHING;

INSERT INTO campus_schedules (campus, day_of_week, open_time, close_time, is_closed)
VALUES
  ('Monterrico', 1, TIME '06:00', TIME '22:00', FALSE),
  ('Monterrico', 2, TIME '06:00', TIME '22:00', FALSE),
  ('Monterrico', 3, TIME '06:00', TIME '22:00', FALSE),
  ('Monterrico', 4, TIME '06:00', TIME '22:00', FALSE),
  ('Monterrico', 5, TIME '06:00', TIME '22:00', FALSE),
  ('Monterrico', 6, TIME '06:00', TIME '12:00', FALSE),
  ('Monterrico', 7, NULL, NULL, TRUE),
  ('Mayorazgo', 1, TIME '05:30', TIME '22:00', FALSE),
  ('Mayorazgo', 2, TIME '05:30', TIME '22:00', FALSE),
  ('Mayorazgo', 3, TIME '05:30', TIME '22:00', FALSE),
  ('Mayorazgo', 4, TIME '05:30', TIME '22:00', FALSE),
  ('Mayorazgo', 5, TIME '05:30', TIME '22:00', FALSE),
  ('Mayorazgo', 6, TIME '05:30', TIME '22:00', FALSE),
  ('Mayorazgo', 7, NULL, NULL, TRUE)
ON CONFLICT (campus, day_of_week) DO NOTHING;

INSERT INTO role_permissions (role_id, permission_id)
SELECT r.id, p.id
FROM roles r
JOIN permissions p ON p.code IN ('RESERVA_CREAR', 'RESERVA_EDITAR', 'RESERVA_CANCELAR')
WHERE r.name = 'ESTUDIANTE'
ON CONFLICT DO NOTHING;

COMMIT;

