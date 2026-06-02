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
  ('MON-CBU', 'Campus Monterrico - Centro Bienestar Universitario', 'Monterrico'),
  ('MON-CDCS', 'Campus Monterrico - Centro Deportivo Cruz del Sur', 'Monterrico'),
  ('MAY-CDM', 'Campus Mayorazgo - Centro Deportivo Mayorazgo', 'Mayorazgo')
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    location = EXCLUDED.location;

-- Rooms
-- Nota: code/name/campus/venue/location se guardan en espanol en BD/backend.
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
    ('MAY-CDM', 'MAY-CDM-BAS-COMP', 'Basket cancha completa', 10, 'Zona Norte', 'DISPONIBLE', 'Mayorazgo', 'Centro Deportivo Mayorazgo', 2, FALSE, 10),
    ('MAY-CDM', 'MAY-CDM-BAS-MED', 'Basket media cancha', 8, 'Zona Norte', 'DISPONIBLE', 'Mayorazgo', 'Centro Deportivo Mayorazgo', 2, FALSE, 8),
    ('MAY-CDM', 'MAY-CDM-FRONTON', 'Campo fronton', 4, 'Zona Oeste', 'DISPONIBLE', 'Mayorazgo', 'Centro Deportivo Mayorazgo', 2, FALSE, 4),
    ('MAY-CDM', 'MAY-CDM-PADEL', 'Campo padel', 4, 'Zona Este', 'DISPONIBLE', 'Mayorazgo', 'Centro Deportivo Mayorazgo', 2, FALSE, 4),
    ('MAY-CDM', 'MAY-CDM-TENIS', 'Campo tenis', 4, 'Zona Este', 'DISPONIBLE', 'Mayorazgo', 'Centro Deportivo Mayorazgo', 2, FALSE, 4),
    ('MAY-CDM', 'MAY-CDM-FUTBOL', 'Cancha Futbol', 14, 'Zona Sur', 'DISPONIBLE', 'Mayorazgo', 'Centro Deportivo Mayorazgo', 10, TRUE, 14),
    ('MAY-CDM', 'MAY-CDM-VOLEY', 'Cancha Voley', 12, 'Zona Sur', 'DISPONIBLE', 'Mayorazgo', 'Centro Deportivo Mayorazgo', 8, TRUE, 12),
    ('MAY-CDM', 'MAY-CDM-PISCINA', 'Piscina', 16, 'Zona Centro', 'DISPONIBLE', 'Mayorazgo', 'Centro Deportivo Mayorazgo', 6, FALSE, 16),
    ('MON-CBU', 'MON-CBU-CAM-MULTI', 'Campo multiuso', 10, 'Edificio F1 - Piso 3', 'DISPONIBLE', 'Monterrico', 'Centro Bienestar Universitario', 4, FALSE, 10),
    ('MON-CBU', 'MON-CBU-CUBICULOS', 'Cubiculos', 8, 'Edificio F2 - Piso 2', 'DISPONIBLE', 'Monterrico', 'Centro Bienestar Universitario', 1, FALSE, 8),
    ('MON-CBU', 'MON-CBU-FUT-MESA', 'Fulbito de Mesa', 4, 'Edificio F2 - Piso 2', 'DISPONIBLE', 'Monterrico', 'Centro Bienestar Universitario', 2, FALSE, 4),
    ('MON-CBU', 'MON-CBU-SAL-BAILE', 'Sala de Baile', 14, 'Edificio F1 - Piso 4', 'DISPONIBLE', 'Monterrico', 'Centro Bienestar Universitario', 4, FALSE, 14),
    ('MON-CBU', 'MON-CBU-SAL-VISION', 'Sala visionado', 12, 'Edificio F2 - Piso 2', 'DISPONIBLE', 'Monterrico', 'Centro Bienestar Universitario', 2, FALSE, 12),
    ('MON-CBU', 'MON-CBU-TEN-MESA', 'Tennis de Mesa', 4, 'Edificio F2 - Piso 2', 'DISPONIBLE', 'Monterrico', 'Centro Bienestar Universitario', 2, FALSE, 4),
    ('MON-CDCS', 'MON-CDCS-CRUZ-FUT', 'Campo Cruz del Sur - Fulbito', 12, 'Losa - Parque Cruz del Sur', 'DISPONIBLE', 'Monterrico', 'Centro Deportivo Cruz del Sur', 8, TRUE, 12)
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
INSERT INTO notification_preferences (
  user_id,
  email_enabled,
  reminder_enabled,
  booking_changes_enabled,
  theme_mode,
  font_scale,
  login_landing_view
)
SELECT
  u.id,
  TRUE,
  TRUE,
  TRUE,
  'LIGHT',
  1.0,
  CASE WHEN r.name = 'ADMIN' THEN 'ADMIN_ROOMS' ELSE 'STUDENT_MY_BOOKINGS' END
FROM users u
JOIN roles r ON r.id = u.role_id
ON CONFLICT (user_id) DO NOTHING;

-- Seed bookings for base student user (idempotente por note)
-- Las fechas se calculan evitando domingos y los horarios respetan los bloques
-- configurados por campus: Monterrico usa slots de 60 minutos.
-- 1) Limpia duplicados historicos de seed, conservando el mas reciente por note.
WITH seed_user AS (
  SELECT id
  FROM users
  WHERE LOWER(email) = LOWER('20224692@aloe.ulima.edu.pe')
)
DELETE FROM bookings b
USING seed_user u
WHERE b.user_id = u.id
  AND b.note IN (
    'Seed R01: bookings activa',
    'Seed R01: bookings activa 2',
    'Seed R01: historica cancelada'
  )
  AND b.id NOT IN (
    SELECT MAX(k.id)
    FROM bookings k
    WHERE k.user_id = u.id
      AND k.note IN (
        'Seed R01: bookings activa',
        'Seed R01: bookings activa 2',
        'Seed R01: historica cancelada'
      )
    GROUP BY k.note
  );

-- 2) Reubica/actualiza seeds existentes segun fecha relativa actual.
UPDATE bookings b
SET room_id = r.id,
    booking_date = CURRENT_DATE + CASE EXTRACT(ISODOW FROM CURRENT_DATE)::INTEGER
      WHEN 5 THEN 3
      WHEN 6 THEN 2
      WHEN 7 THEN 1
      ELSE 1
    END,
    start_time = TIME '16:00',
    end_time = TIME '17:00',
    status = 'ACTIVA',
    people_count = 2
FROM users u
JOIN rooms r ON r.code = 'MON-CBU-CUBICULOS'
WHERE b.user_id = u.id
  AND LOWER(u.email) = LOWER('20224692@aloe.ulima.edu.pe')
  AND b.note = 'Seed R01: bookings activa';

UPDATE bookings b
SET room_id = r.id,
    booking_date = CURRENT_DATE + CASE EXTRACT(ISODOW FROM CURRENT_DATE)::INTEGER
      WHEN 4 THEN 4
      WHEN 5 THEN 4
      WHEN 6 THEN 3
      WHEN 7 THEN 2
      ELSE 2
    END,
    start_time = TIME '10:00',
    end_time = TIME '11:00',
    status = 'ACTIVA',
    people_count = 1
FROM users u
JOIN rooms r ON r.code = 'MON-CBU-SAL-VISION'
WHERE b.user_id = u.id
  AND LOWER(u.email) = LOWER('20224692@aloe.ulima.edu.pe')
  AND b.note = 'Seed R01: bookings activa 2';

UPDATE bookings b
SET room_id = r.id,
    booking_date = CURRENT_DATE - CASE EXTRACT(ISODOW FROM CURRENT_DATE)::INTEGER
      WHEN 1 THEN 3
      WHEN 7 THEN 2
      ELSE 1
    END,
    start_time = TIME '12:00',
    end_time = TIME '13:00',
    status = 'CANCELADA',
    people_count = 2
FROM users u
JOIN rooms r ON r.code = 'MAY-CDM-BAS-COMP'
WHERE b.user_id = u.id
  AND LOWER(u.email) = LOWER('20224692@aloe.ulima.edu.pe')
  AND b.note = 'Seed R01: historica cancelada';

-- 3) Inserta seed faltante (si no existe por note).
INSERT INTO bookings (user_id, room_id, booking_date, start_time, end_time, status, people_count, note)
SELECT
  u.id,
  r.id,
  CURRENT_DATE + CASE EXTRACT(ISODOW FROM CURRENT_DATE)::INTEGER
    WHEN 5 THEN 3
    WHEN 6 THEN 2
    WHEN 7 THEN 1
    ELSE 1
  END,
  TIME '16:00',
  TIME '17:00',
  'ACTIVA',
  2,
  'Seed R01: bookings activa'
FROM users u
JOIN rooms r ON r.code = 'MON-CBU-CUBICULOS'
WHERE LOWER(u.email) = LOWER('20224692@aloe.ulima.edu.pe')
  AND NOT EXISTS (
    SELECT 1 FROM bookings x
    WHERE x.user_id = u.id
      AND x.note = 'Seed R01: bookings activa'
  );

INSERT INTO bookings (user_id, room_id, booking_date, start_time, end_time, status, people_count, note)
SELECT
  u.id,
  r.id,
  CURRENT_DATE + CASE EXTRACT(ISODOW FROM CURRENT_DATE)::INTEGER
    WHEN 4 THEN 4
    WHEN 5 THEN 4
    WHEN 6 THEN 3
    WHEN 7 THEN 2
    ELSE 2
  END,
  TIME '10:00',
  TIME '11:00',
  'ACTIVA',
  1,
  'Seed R01: bookings activa 2'
FROM users u
JOIN rooms r ON r.code = 'MON-CBU-SAL-VISION'
WHERE LOWER(u.email) = LOWER('20224692@aloe.ulima.edu.pe')
  AND NOT EXISTS (
    SELECT 1 FROM bookings x
    WHERE x.user_id = u.id
      AND x.note = 'Seed R01: bookings activa 2'
  );

INSERT INTO bookings (user_id, room_id, booking_date, start_time, end_time, status, people_count, note)
SELECT
  u.id,
  r.id,
  CURRENT_DATE - CASE EXTRACT(ISODOW FROM CURRENT_DATE)::INTEGER
    WHEN 1 THEN 3
    WHEN 7 THEN 2
    ELSE 1
  END,
  TIME '12:00',
  TIME '13:00',
  'CANCELADA',
  2,
  'Seed R01: historica cancelada'
FROM users u
JOIN rooms r ON r.code = 'MAY-CDM-BAS-COMP'
WHERE LOWER(u.email) = LOWER('20224692@aloe.ulima.edu.pe')
  AND NOT EXISTS (
    SELECT 1 FROM bookings x
    WHERE x.user_id = u.id
      AND x.note = 'Seed R01: historica cancelada'
  );

-- Sample maintenances
INSERT INTO maintenances (room_id, start_at, end_at, reason, status)
SELECT r.id, NOW() + INTERVAL '3 day', NOW() + INTERVAL '3 day 2 hour', 'Preventive maintenance seed', 'PROGRAMADO'
FROM rooms r
WHERE r.code = 'MAY-CDM-PISCINA'
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
