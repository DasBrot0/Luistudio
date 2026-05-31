-- Luistudio - seed Release 01 (idempotente)
-- Requiere esquema base aplicado con 001_init.sql
-- Incluye usuarios seed (no requiere inserts manuales adicionales).

BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Configuracion base (si no existe)
INSERT INTO system_config (config_key, config_value, updated_at)
VALUES
  ('max_reservas_simultaneas', '2', NOW()),
  ('duracion_maxima_minutos', '120', NOW()),
  ('campus_slot_minutos_monterrico', '60', NOW()),
  ('campus_slot_minutos_mayorazgo', '45', NOW())
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
    ('ADMIN', '20233916', 'Franco', 'Luna', '20233916@aloe.ulima.edu.pe', 'Admin123!'),
    ('ADMIN', '20224815', 'Luis', 'G', '20224815@aloe.ulima.edu.pe', 'Admin123!'),
    ('ESTUDIANTE', '20224692', 'Irwin', 'C', '20224692@aloe.ulima.edu.pe', 'Student123!'),
    ('ESTUDIANTE', '20246423', 'Allison', 'C', '20246423@aloe.ulima.edu.pe', 'Student123!'),
    ('ESTUDIANTE', '20193934', 'Joaquin', 'C', '20193934@aloe.ulima.edu.pe', 'Student123!')
) AS v(rol_nombre, code, first_name, last_name, email, plain_password)
JOIN roles r ON r.name = v.rol_nombre
WHERE NOT EXISTS (
  SELECT 1 FROM users u WHERE LOWER(u.email) = LOWER(v.email)
);

-- Ensure 2FA enabled for the seeded admin even if already exists
UPDATE users
SET has_2fa = TRUE,
    updated_at = NOW()
WHERE LOWER(email) IN (
  LOWER('20233916@aloe.ulima.edu.pe'),
  LOWER('20224815@aloe.ulima.edu.pe')
);

-- Buildings (campus + center)
INSERT INTO buildings (code, name, location)
VALUES
  ('MON-CBU', 'Monterrico Campus - University Wellness Center', 'Monterrico'),
  ('MON-CDCS', 'Monterrico Campus - Cruz del Sur Sports Center', 'Monterrico'),
  ('MAY-CDM', 'Mayorazgo Campus - Mayorazgo Sports Center', 'Mayorazgo')
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    location = EXCLUDED.location;

-- Rooms
-- Nota: name/campus/venue se guardan en ingles en BD/backend.
INSERT INTO rooms (
  building_id,
  code,
  name,
  capacity,
  location,
  status,
  campus,
  venue,
  min_people,
  min_people_required,
  max_people
)
SELECT
  p.id,
  s.code,
  s.name,
  s.capacity,
  s.location,
  s.status::VARCHAR,
  s.campus,
  s.venue,
  s.min_people,
  s.min_people_required,
  s.max_people
FROM (
  VALUES
    ('MAY-CDM', 'MAY-CDM-BASKET-FULL', 'Basketball Full Court', 10, 'Mayorazgo Sports Center', 'DISPONIBLE', 'Mayorazgo', 'Mayorazgo Sports Center', 2, FALSE, 10),
    ('MAY-CDM', 'MAY-CDM-BASKET-HALF', 'Basketball Half Court', 8, 'Mayorazgo Sports Center', 'DISPONIBLE', 'Mayorazgo', 'Mayorazgo Sports Center', 2, FALSE, 8),
    ('MAY-CDM', 'MAY-CDM-FRONTON', 'Fronton Court', 4, 'Mayorazgo Sports Center', 'DISPONIBLE', 'Mayorazgo', 'Mayorazgo Sports Center', 2, FALSE, 4),
    ('MAY-CDM', 'MAY-CDM-PADEL', 'Padel Court', 4, 'Mayorazgo Sports Center', 'DISPONIBLE', 'Mayorazgo', 'Mayorazgo Sports Center', 2, FALSE, 4),
    ('MAY-CDM', 'MAY-CDM-TENNIS', 'Tennis Court', 4, 'Mayorazgo Sports Center', 'DISPONIBLE', 'Mayorazgo', 'Mayorazgo Sports Center', 2, FALSE, 4),
    ('MAY-CDM', 'MAY-CDM-SOCCER', 'Soccer Field', 14, 'Mayorazgo Sports Center', 'DISPONIBLE', 'Mayorazgo', 'Mayorazgo Sports Center', 10, TRUE, 14),
    ('MAY-CDM', 'MAY-CDM-VOLLEY', 'Volleyball Court', 12, 'Mayorazgo Sports Center', 'DISPONIBLE', 'Mayorazgo', 'Mayorazgo Sports Center', 8, TRUE, 12),
    ('MAY-CDM', 'MAY-CDM-POOL', 'Swimming Pool', 16, 'Mayorazgo Sports Center', 'DISPONIBLE', 'Mayorazgo', 'Mayorazgo Sports Center', 6, FALSE, 16),
    ('MON-CBU', 'MON-CBU-MULTIUSE', 'Multiuse Field', 10, 'University Wellness Center', 'DISPONIBLE', 'Monterrico', 'University Wellness Center', 4, FALSE, 10),
    ('MON-CBU', 'MON-CBU-CUBICLES', 'Study Cubicles', 8, 'University Wellness Center', 'DISPONIBLE', 'Monterrico', 'University Wellness Center', 1, FALSE, 8),
    ('MON-CBU', 'MON-CBU-TBL-FTBL', 'Table Football', 4, 'University Wellness Center', 'DISPONIBLE', 'Monterrico', 'University Wellness Center', 2, FALSE, 4),
    ('MON-CBU', 'MON-CBU-DANCE', 'Dance Room', 14, 'University Wellness Center', 'DISPONIBLE', 'Monterrico', 'University Wellness Center', 4, FALSE, 14),
    ('MON-CBU', 'MON-CBU-SCREENING', 'Screening Room', 12, 'University Wellness Center', 'DISPONIBLE', 'Monterrico', 'University Wellness Center', 2, FALSE, 12),
    ('MON-CBU', 'MON-CBU-TABLE-TENNIS', 'Table Tennis', 4, 'University Wellness Center', 'DISPONIBLE', 'Monterrico', 'University Wellness Center', 2, FALSE, 4),
    ('MON-CDCS', 'MON-CDCS-CRUZ-FIVE', 'Cruz del Sur Field - Five-a-side Soccer', 12, 'Cruz del Sur Sports Center', 'DISPONIBLE', 'Monterrico', 'Cruz del Sur Sports Center', 8, TRUE, 12)
) AS s(
  building_code,
  code,
  name,
  capacity,
  location,
  status,
  campus,
  venue,
  min_people,
  min_people_required,
  max_people
)
JOIN buildings p ON p.code = s.building_code
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    capacity = EXCLUDED.capacity,
    location = EXCLUDED.location,
    status = EXCLUDED.status,
    campus = EXCLUDED.campus,
    venue = EXCLUDED.venue,
    min_people = EXCLUDED.min_people,
    min_people_required = EXCLUDED.min_people_required,
    max_people = EXCLUDED.max_people;

-- Preferencias para usuarios existentes
INSERT INTO notification_preferences (user_id, email_enabled, reminder_enabled, booking_changes_enabled, theme_mode, font_scale)
SELECT u.id, TRUE, TRUE, TRUE, 'LIGHT', 1.0
FROM users u
ON CONFLICT (user_id) DO NOTHING;

-- Seed bookings for base student user
INSERT INTO bookings (user_id, room_id, booking_date, start_time, end_time, status, people_count, note)
SELECT u.id, r.id, CURRENT_DATE + 1, TIME '16:30', TIME '17:30', 'ACTIVA', 2, 'Seed R01: bookings activa'
FROM users u
JOIN rooms r ON r.code = 'MON-CBU-CUBICLES'
WHERE LOWER(u.email) = LOWER('20224692@aloe.ulima.edu.pe')
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
JOIN rooms r ON r.code = 'MON-CBU-SCREENING'
WHERE LOWER(u.email) = LOWER('20224692@aloe.ulima.edu.pe')
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
JOIN rooms r ON r.code = 'MAY-CDM-BASKET-FULL'
WHERE LOWER(u.email) = LOWER('20224692@aloe.ulima.edu.pe')
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
WHERE r.code = 'MAY-CDM-POOL'
  AND NOT EXISTS (
    SELECT 1 FROM maintenances m
    WHERE m.room_id = r.id
      AND m.reason = 'Preventive maintenance seed'
  );

-- Outbox inicial
INSERT INTO email_outbox (recipient, subject, body, status, attempts)
VALUES
  ('20224692@aloe.ulima.edu.pe', 'Bienvenido a Luistudio', 'Tu cuenta ya esta lista para reservar salas.', 'PENDIENTE', 0),
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
