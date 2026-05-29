-- Luistudio - seed Release 01 (idempotente)
-- Requiere esquema base aplicado con 001_init.sql
-- Requiere que existan usuarios base en tabla users (ejecuta inserts manuales o agrega en seed). 

BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Configuracion base (si no existe)
INSERT INTO system_config (config_key, config_value, updated_at)
VALUES
  ('max_reservas_simultaneas', '2', NOW()),
  ('duracion_maxima_minutos', '120', NOW())
ON CONFLICT (config_key) DO UPDATE SET config_value = EXCLUDED.config_value, updated_at = NOW();

-- Usuarios seed (password hash bcrypt via pgcrypto)
INSERT INTO users (
  role_id, code, first_name, last_name, email, password_hash, status, locked_until, has_2fa, created_at, updated_at
)
SELECT
  r.id,
  v.code,
  v.first_name,
  v.last_name,
  v.email,
  crypt(v.plain_password, gen_salt('bf', 12)),
  'HABILITADO',
  NULL,
  CASE WHEN v.rol_nombre = 'ADMIN' THEN TRUE ELSE FALSE END,
  NOW(),
  NOW()
FROM (
  VALUES
    ('ADMIN', '20224815', 'Franco', 'Luna', '20233916@aloe.ulima.edu.pe', 'Admin123!'),
    ('ESTUDIANTE', '20230001', 'Luis', 'Cutti', 'email@universidad.edu.pe', 'Student123!')
) AS v(rol_nombre, code, first_name, last_name, email, plain_password)
JOIN roles r ON r.name = v.rol_nombre
WHERE NOT EXISTS (
  SELECT 1 FROM users u WHERE LOWER(u.email) = LOWER(v.email)
);

-- Ensure 2FA enabled for the seeded admin even if already exists
UPDATE users
SET has_2fa = TRUE,
    updated_at = NOW()
WHERE LOWER(email) = LOWER('20233916@aloe.ulima.edu.pe');

-- Buildings
INSERT INTO buildings (code, name, location)
VALUES
  ('E1', 'Building E1', 'E1'),
  ('E2', 'Building E2', 'E2'),
  ('E3', 'Building E3', 'E3')
ON CONFLICT (code) DO NOTHING;

-- Rooms
INSERT INTO rooms (building_id, code, name, capacity, location, status)
SELECT p.id, s.code, s.name, s.capacity, s.location, s.status::VARCHAR
FROM (
  VALUES
    ('E3', '123', 'Room 1', 6, 'E3', 'DISPONIBLE'),
    ('E3', '505', 'Room 505', 8, 'E3', 'DISPONIBLE'),
    ('E1', 'A1', 'Room A1', 6, 'E1', 'DISPONIBLE'),
    ('E1', 'A2', 'Room A2', 4, 'E1', 'DISPONIBLE'),
    ('E2', 'B12', 'Room B12', 10, 'E2', 'DISPONIBLE')
) AS s(building_code, code, name, capacity, location, status)
JOIN buildings p ON p.code = s.building_code
ON CONFLICT (code) DO NOTHING;

-- Preferencias para usuarios existentes
INSERT INTO notification_preferences (user_id, email_enabled, reminder_enabled, booking_changes_enabled, theme_mode, font_scale)
SELECT u.id, TRUE, TRUE, TRUE, 'LIGHT', 1.0
FROM users u
ON CONFLICT (user_id) DO NOTHING;

-- Seed bookings for base student user
-- Expected student email: correo@universidad.edu.pe
INSERT INTO bookings (user_id, room_id, booking_date, start_time, end_time, status, people_count, note)
SELECT u.id, r.id, CURRENT_DATE + 1, TIME '16:30', TIME '17:30', 'ACTIVA', 2, 'Seed R01: bookings activa'
FROM users u
JOIN rooms r ON r.code = '123'
WHERE u.email = 'correo@universidad.edu.pe'
  AND NOT EXISTS (
    SELECT 1 FROM bookings x
    WHERE x.user_id = u.id
      AND x.room_id = r.id
      AND x.booking_date = CURRENT_DATE + 1
      AND x.start_time = TIME '16:30'
  );

INSERT INTO bookings (user_id, room_id, booking_date, start_time, end_time, status, people_count, note)
SELECT u.id, r.id, CURRENT_DATE + 2, TIME '10:00', TIME '11:00', 'ACTIVA', 1, 'Seed R01: bookings activa 2'
FROM users u
JOIN rooms r ON r.code = 'A1'
WHERE u.email = 'correo@universidad.edu.pe'
  AND NOT EXISTS (
    SELECT 1 FROM bookings x
    WHERE x.user_id = u.id
      AND x.room_id = r.id
      AND x.booking_date = CURRENT_DATE + 2
      AND x.start_time = TIME '10:00'
  );

INSERT INTO bookings (user_id, room_id, booking_date, start_time, end_time, status, people_count, note)
SELECT u.id, r.id, CURRENT_DATE - 1, TIME '12:00', TIME '13:00', 'CANCELADA', 2, 'Seed R01: historica cancelada'
FROM users u
JOIN rooms r ON r.code = '505'
WHERE u.email = 'correo@universidad.edu.pe'
  AND NOT EXISTS (
    SELECT 1 FROM bookings x
    WHERE x.user_id = u.id
      AND x.room_id = r.id
      AND x.booking_date = CURRENT_DATE - 1
      AND x.start_time = TIME '12:00'
  );

-- Sample maintenances
INSERT INTO maintenances (room_id, start_at, end_at, reason, status)
SELECT r.id, NOW() + INTERVAL '3 day', NOW() + INTERVAL '3 day 2 hour', 'Preventive maintenance seed', 'PROGRAMADO'
FROM rooms r
WHERE r.code = 'B12'
  AND NOT EXISTS (
    SELECT 1 FROM maintenances m
    WHERE m.room_id = r.id
      AND m.reason = 'Preventive maintenance seed'
  );

-- Outbox inicial
INSERT INTO email_outbox (recipient, subject, body, status, attempts)
VALUES
  ('correo@universidad.edu.pe', 'Bienvenido a Luistudio', 'Tu cuenta ya esta lista para reservar salas.', 'PENDIENTE', 0),
  ('20233916@aloe.ulima.edu.pe', 'Panel administrativo habilitado', 'Ya puedes gestionar salas y reservas.', 'PENDIENTE', 0)
ON CONFLICT DO NOTHING;

-- Auditoria de ejemplo
INSERT INTO audit_log (actor_user_id, action, entity, entity_id, detail)
SELECT u.id, 'SEED_INIT', 'sistema', 'release-01', '{"message":"Seed inicial Release 01"}'
FROM users u
WHERE u.email = '20233916@aloe.ulima.edu.pe'
  AND NOT EXISTS (
    SELECT 1 FROM audit_log a
    WHERE a.action = 'SEED_INIT' AND a.entity_id = 'release-01'
  );

COMMIT;

