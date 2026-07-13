-- Luistudio - datos semilla idempotentes. Requiere 001_init.sql.
BEGIN;

CREATE EXTENSION IF NOT EXISTS pgcrypto;

INSERT INTO system_config (config_key, config_value, updated_at) VALUES
  ('max_reservas_simultaneas', '2', NOW()),
  ('duracion_maxima_minutos', '120', NOW()),
  ('campus_slot_minutos_monterrico', '60', NOW()),
  ('campus_slot_minutos_mayorazgo', '45', NOW())
ON CONFLICT (config_key) DO UPDATE SET config_value = EXCLUDED.config_value, updated_at = NOW();

INSERT INTO users (role_id, code, first_name, last_name, email, password_hash, status, has_2fa)
SELECT r.id, s.code, s.first_name, s.last_name, s.email, crypt(s.password, gen_salt('bf', 10)), 'HABILITADO', s.role_name = 'ADMIN'
FROM (VALUES
  ('ADMIN', '20233916', 'Franco', 'Luna', '20233916@aloe.ulima.edu.pe', 'Admin123!'),
  ('ADMIN', '20224815', 'Luis', 'G', '20224815@aloe.ulima.edu.pe', 'Admin123!'),
  ('ESTUDIANTE', '20224692', 'Irwin', 'C', '20224692@aloe.ulima.edu.pe', 'Student123!'),
  ('ESTUDIANTE', '20246423', 'Allison', 'C', '20246423@aloe.ulima.edu.pe', 'Student123!'),
  ('ESTUDIANTE', '20193934', 'Joaquin', 'C', '20193934@aloe.ulima.edu.pe', 'Student123!')
) AS s(role_name, code, first_name, last_name, email, password)
JOIN roles r ON r.name = s.role_name
ON CONFLICT (code) DO UPDATE SET first_name = EXCLUDED.first_name, last_name = EXCLUDED.last_name, has_2fa = EXCLUDED.has_2fa;

-- Jerarquía normalizada: campus -> pabellón/centro -> sala.
-- El Centro de Bienestar Universitario funciona dentro de F1; Cruz del Sur es su propio pabellón (CS).
INSERT INTO buildings (campus_id, code, name, latitude, longitude, map_enabled, map_order)
SELECT c.id, s.code, s.name, s.latitude, s.longitude, TRUE, s.map_order
FROM campuses c JOIN (VALUES
  ('Monterrico','A1','Pabellón A1',-12.084124,-76.972601,1), ('Monterrico','A2','Pabellón A2',-12.084376,-76.972508,2),
  ('Monterrico','B','Pabellón B',-12.084039,-76.972403,3), ('Monterrico','C','Pabellón C',-12.084083,-76.972113,4),
  ('Monterrico','D1','Pabellón D1',-12.083958,-76.971909,5), ('Monterrico','D2','Pabellón D2',-12.084176,-76.971874,6),
  ('Monterrico','D3','Pabellón D3',-12.084398,-76.971947,7), ('Monterrico','E1','Pabellón E1',-12.084570,-76.972030,8),
  ('Monterrico','E2','Pabellón E2',-12.084701,-76.972151,9), ('Monterrico','E3','Pabellón E3',-12.084816,-76.972008,10),
  ('Monterrico','F1','Pabellón F1',-12.083967,-76.971580,11), ('Monterrico','F2','Pabellón F2',-12.084011,-76.971293,12),
  ('Monterrico','G','Pabellón G',-12.083902,-76.971178,13), ('Monterrico','H','Pabellón H',-12.084117,-76.970929,14),
  ('Monterrico','I1','Pabellón I1',-12.084445,-76.971293,15), ('Monterrico','I2','Pabellón I2',-12.084264,-76.971016,16),
  ('Monterrico','J','Pabellón J',-12.084813,-76.971807,17), ('Monterrico','K','Pabellón K',-12.084966,-76.971877,18),
  ('Monterrico','L1','Pabellón L1',-12.084888,-76.971698,19), ('Monterrico','L2','Pabellón L2',-12.085025,-76.971772,20),
  ('Monterrico','L3','Pabellón L3',-12.084984,-76.971599,21), ('Monterrico','N','Pabellón N',-12.085090,-76.971089,22),
  ('Monterrico','O1','Pabellón O1',-12.084545,-76.970709,23), ('Monterrico','O2','Pabellón O2',-12.084735,-76.970754,24),
  ('Monterrico','P','Pabellón P',-12.085081,-76.970824,25), ('Monterrico','ZEA','Zona de Estudios Abierta',-12.084576,-76.971127,26),
  ('Monterrico','CS','Cruz del Sur',-12.0849500,-76.9713500,27),
  ('Mayorazgo','CDM','Centro Deportivo Mayorazgo',-12.0594038,-76.9410625,1)
) AS s(campus_name, code, name, latitude, longitude, map_order) ON c.name = s.campus_name
ON CONFLICT (code) DO UPDATE SET campus_id=EXCLUDED.campus_id, name=EXCLUDED.name, latitude=EXCLUDED.latitude, longitude=EXCLUDED.longitude, map_enabled=TRUE, map_order=EXCLUDED.map_order;

-- Coordenadas calibradas desde buildings.csv. CS y CDM reemplazan los códigos históricos MON-CDCS y MAY-CDM.
UPDATE buildings AS b
SET latitude = c.latitude,
    longitude = c.longitude
FROM (VALUES
  ('A1',-12.0842548,-76.9729651), ('A2',-12.0847265,-76.9728585), ('B',-12.0841222,-76.9726215),
  ('C',-12.0841867,-76.9721210), ('D1',-12.0840167,-76.9717754), ('D2',-12.0844383,-76.9718456),
  ('D3',-12.0844092,-76.9715898), ('E1',-12.0850523,-76.9720537), ('E2',-12.0852862,-76.9722265),
  ('E3',-12.0854052,-76.9719758), ('F1',-12.0841048,-76.9712867), ('F2',-12.0841719,-76.9707741),
  ('G',-12.0839273,-76.9705718), ('H',-12.0842350,-76.9701752), ('I1',-12.0846611,-76.9706975),
  ('I2',-12.0844132,-76.9703038), ('J',-12.0853964,-76.9716249), ('K',-12.0856455,-76.9717785),
  ('L1',-12.0856338,-76.9714881), ('L2',-12.0857763,-76.9715649), ('L3',-12.0856081,-76.9712052),
  ('N',-12.0858373,-76.9704175), ('O1',-12.0850020,-76.9698048), ('O2',-12.0854081,-76.9698080),
  ('P',-12.0858531,-76.9698349), ('ZEA',-12.0852699,-76.9703663), ('CS',-12.0864230,-76.9701387),
  ('CDM',-12.0596826,-76.9421069)
) AS c(code, latitude, longitude)
WHERE b.code = c.code;

INSERT INTO rooms (building_id, code, name, capacity, location, status, min_people, min_people_required, max_people)
SELECT b.id, s.code, s.name, s.capacity, s.location, 'DISPONIBLE', s.min_people, s.min_required, s.max_people
FROM (VALUES
  ('CDM','CDM-BAS-COMP','Basket cancha completa',10,'Zona Norte',2,FALSE,10), ('CDM','CDM-BAS-MED','Basket media cancha',8,'Zona Norte',2,FALSE,8),
  ('CDM','CDM-FRONTON','Campo frontón',4,'Zona Oeste',2,FALSE,4), ('CDM','CDM-PADEL','Campo pádel',4,'Zona Este',2,FALSE,4),
  ('CDM','CDM-TENIS','Campo tenis',4,'Zona Este',2,FALSE,4), ('CDM','CDM-FUTBOL','Cancha fútbol',14,'Zona Sur',10,TRUE,14),
  ('CDM','CDM-VOLEY','Cancha vóley',12,'Zona Sur',8,TRUE,12), ('CDM','CDM-PISCINA','Piscina',16,'Zona Centro',6,FALSE,16),
  ('F1','F1-CAM-MULTI','Campo multiuso',10,'Piso 3',4,FALSE,10), ('F1','F1-CUBICULOS','Cubículos',8,'Piso 2',1,FALSE,8),
  ('F1','F1-FUT-MESA','Fulbito de mesa',4,'Piso 2',2,FALSE,4), ('F1','F1-SAL-BAILE','Sala de baile',14,'Piso 4',4,FALSE,14),
  ('F1','F1-SAL-VISION','Sala de visionado',12,'Piso 2',2,FALSE,12), ('F1','F1-TEN-MESA','Tenis de mesa',4,'Piso 2',2,FALSE,4),
  ('CS','CS-CRUZ-FUT','Campo Cruz del Sur - Fulbito',12,'Losa - Parque Cruz del Sur',8,TRUE,12)
) AS s(building_code, code, name, capacity, location, min_people, min_required, max_people)
JOIN buildings b ON b.code = s.building_code
ON CONFLICT (code) DO UPDATE SET building_id=EXCLUDED.building_id, name=EXCLUDED.name, capacity=EXCLUDED.capacity, location=EXCLUDED.location, status=EXCLUDED.status, min_people=EXCLUDED.min_people, min_people_required=EXCLUDED.min_people_required, max_people=EXCLUDED.max_people;

-- Solo se muestran en el mapa pabellones que ya tienen al menos una sala activa.
UPDATE buildings b
SET map_enabled = EXISTS (
  SELECT 1 FROM rooms r WHERE r.building_id = b.id AND r.status <> 'INACTIVA'
);

UPDATE rooms
SET noise_level = 'BAJO', supports_concentration = TRUE, room_type = 'ESTUDIO_INDIVIDUAL'
WHERE code = 'F1-CUBICULOS';

INSERT INTO room_equipment (room_id, equipment)
SELECT r.id, e.equipment
FROM rooms r
JOIN (VALUES
  ('F1-CUBICULOS', 'computadora'),
  ('F1-CUBICULOS', 'pizarra'),
  ('F1-SAL-VISION', 'proyector')
) AS e(code, equipment) ON e.code = r.code
ON CONFLICT (room_id, equipment) DO NOTHING;

INSERT INTO notification_preferences (user_id, email_enabled, reminder_enabled, booking_changes_enabled, theme_mode, font_scale, login_landing_view)
SELECT u.id, TRUE, TRUE, TRUE, 'LIGHT', 1.0, CASE WHEN r.name = 'ADMIN' THEN 'ADMIN_DASHBOARD' ELSE 'STUDENT_MY_BOOKINGS' END
FROM users u JOIN roles r ON r.id = u.role_id ON CONFLICT (user_id) DO NOTHING;

INSERT INTO maintenances (room_id, start_at, end_at, reason, status)
SELECT r.id, NOW() + INTERVAL '3 day', NOW() + INTERVAL '3 day 2 hour', 'Preventive maintenance seed', 'PROGRAMADO'
FROM rooms r WHERE r.code = 'CDM-PISCINA' AND NOT EXISTS (SELECT 1 FROM maintenances m WHERE m.room_id = r.id AND m.reason = 'Preventive maintenance seed');

-- Datos de demostración para perfiles, dashboard y estados actuales del mapa.
-- Los códigos, notas y motivos DEMO_* permiten volver a ejecutar este seed sin duplicar filas.
INSERT INTO users (role_id, code, first_name, last_name, email, password_hash, status, has_2fa)
SELECT r.id, s.code, s.first_name, s.last_name, s.email, crypt('Student123!', gen_salt('bf', 10)), 'HABILITADO', FALSE
FROM (VALUES
  ('DEMO001', 'Ana', 'Torres', 'ana.torres.demo@gmail.com'),
  ('DEMO002', 'Bruno', 'Mendoza', 'bruno.mendoza.demo@gmail.com'),
  ('DEMO003', 'Camila', 'Rojas', 'camila.rojas.demo@gmail.com'),
  ('DEMO004', 'Diego', 'Vargas', 'diego.vargas.demo@gmail.com'),
  ('DEMO005', 'Elena', 'Castro', 'elena.castro.demo@gmail.com'),
  ('DEMO006', 'Fabio', 'Navarro', 'fabio.navarro.demo@gmail.com'),
  ('DEMO007', 'Gabriela', 'Silva', 'gabriela.silva.demo@gmail.com'),
  ('DEMO008', 'Hugo', 'Paredes', 'hugo.paredes.demo@gmail.com'),
  ('DEMO009', 'Irene', 'Campos', 'irene.campos.demo@gmail.com'),
  ('DEMO010', 'Javier', 'Reyes', 'javier.reyes.demo@gmail.com'),
  ('DEMO011', 'Karina', 'Flores', 'karina.flores.demo@gmail.com'),
  ('DEMO012', 'Marco', 'Salazar', 'marco.salazar.demo@gmail.com')
) AS s(code, first_name, last_name, email)
JOIN roles r ON r.name = 'ESTUDIANTE'
ON CONFLICT (code) DO UPDATE SET first_name = EXCLUDED.first_name, last_name = EXCLUDED.last_name, email = EXCLUDED.email, status = 'HABILITADO';

INSERT INTO notification_preferences (user_id, email_enabled, reminder_enabled, booking_changes_enabled, theme_mode, font_scale, login_landing_view)
SELECT u.id, TRUE, TRUE, TRUE, 'LIGHT', 1.0, 'STUDENT_MY_BOOKINGS'
FROM users u WHERE u.code LIKE 'DEMO%'
ON CONFLICT (user_id) DO NOTHING;

-- Las reservas DEMO se regeneran para que los cambios del catálogo y horarios no dejen datos inválidos.
DELETE FROM attendance_records ar
USING bookings b
WHERE ar.booking_id = b.id AND b.note LIKE 'DEMO_%';

DELETE FROM bookings WHERE note LIKE 'DEMO_DASHBOARD_%' OR note = 'DEMO_MAP_CURRENT';

WITH demo_users AS (
  SELECT id, ROW_NUMBER() OVER (ORDER BY code) AS rn, COUNT(*) OVER () AS total FROM users WHERE code LIKE 'DEMO%'
), demo_rooms AS (
  SELECT r.id, r.min_people, r.min_people_required, r.max_people, c.name AS campus_name,
         ROW_NUMBER() OVER (ORDER BY r.code) AS rn, COUNT(*) OVER () AS total
  FROM rooms r
  JOIN buildings b ON b.id = r.building_id
  JOIN campuses c ON c.id = b.campus_id
  WHERE r.status = 'DISPONIBLE'
), samples AS (SELECT generate_series(1, 48) AS n)
INSERT INTO bookings (user_id, room_id, booking_date, start_time, end_time, status, people_count, note, attendance_status, created_at, updated_at)
SELECT u.id, r.id,
       CURRENT_DATE - (EXTRACT(ISODOW FROM CURRENT_DATE)::INTEGER - 1 + 7 + (s.n % 4) * 7),
       TIME '10:00', CASE WHEN r.campus_name = 'Mayorazgo' THEN TIME '10:45' ELSE TIME '11:00' END,
       'COMPLETADA',
       CASE WHEN r.min_people_required THEN r.min_people ELSE LEAST(r.max_people, GREATEST(1, 1 + (s.n % 4))) END,
       'DEMO_DASHBOARD_' || LPAD(s.n::TEXT, 3, '0'),
       CASE WHEN s.n % 7 = 0 THEN 'INASISTIO' ELSE 'ASISTIO' END,
       NOW() - (7 + (s.n % 4) * 7) * INTERVAL '1 day', NOW() - (7 + (s.n % 4) * 7) * INTERVAL '1 day'
FROM samples s
JOIN demo_users u ON u.rn = ((s.n - 1) % u.total) + 1
JOIN demo_rooms r ON r.rn = ((s.n - 1) % r.total) + 1
;

INSERT INTO attendance_records (booking_id, user_id, recorded_at, tolerance_minutes)
SELECT b.id, b.user_id, b.updated_at, 15
FROM bookings b
WHERE b.note LIKE 'DEMO_DASHBOARD_%' AND b.attendance_status = 'INASISTIO'
ON CONFLICT (booking_id) DO NOTHING;

-- Mantenimiento que cubre el instante de ejecución para mostrar el estado MANTENIMIENTO.
INSERT INTO maintenances (room_id, start_at, end_at, reason, status)
SELECT r.id, NOW() - INTERVAL '1 hour', NOW() + INTERVAL '6 hours', 'DEMO_MAP_MAINTENANCE', 'EN_CURSO'
FROM rooms r WHERE r.code = 'F1-TEN-MESA'
  AND NOT EXISTS (SELECT 1 FROM maintenances m WHERE m.reason = 'DEMO_MAP_MAINTENANCE');

UPDATE maintenances SET start_at = NOW() - INTERVAL '1 hour', end_at = NOW() + INTERVAL '6 hours', status = 'EN_CURSO'
WHERE reason = 'DEMO_MAP_MAINTENANCE';

-- Mantiene consistente el estado administrativo de la sala con sus mantenimientos vigentes.
UPDATE rooms r SET status = 'EN_MANTENIMIENTO'
WHERE EXISTS (
  SELECT 1 FROM maintenances m
  WHERE m.room_id = r.id AND m.status = 'EN_CURSO' AND m.start_at <= NOW() AND m.end_at > NOW()
);

-- Normaliza estados persistidos que versiones anteriores no cerraban correctamente.
UPDATE maintenances
SET status = 'FINALIZADO'
WHERE status IN ('PROGRAMADO', 'EN_CURSO') AND end_at <= NOW();

UPDATE maintenances
SET status = 'EN_CURSO'
WHERE status = 'PROGRAMADO' AND start_at <= NOW() AND end_at > NOW();

UPDATE rooms r
SET status = 'DISPONIBLE'
WHERE r.status = 'EN_MANTENIMIENTO'
  AND EXISTS (SELECT 1 FROM maintenances m WHERE m.room_id = r.id)
  AND NOT EXISTS (
    SELECT 1 FROM maintenances m
    WHERE m.room_id = r.id AND m.status = 'EN_CURSO' AND m.start_at <= NOW() AND m.end_at > NOW()
  );

UPDATE login_sessions SET current = FALSE WHERE revoked_at IS NOT NULL AND current = TRUE;

INSERT INTO login_attempts (user_id, attempted_at, success, source_ip, user_agent)
SELECT u.id, NOW() - s.n * INTERVAL '2 hour', s.n % 4 <> 0, '127.0.0.' || (10 + s.n), 'Luistudio demo browser'
FROM generate_series(1, 18) AS s(n)
JOIN users u ON u.code = 'DEMO' || LPAD((((s.n - 1) % 12) + 1)::TEXT, 3, '0')
WHERE NOT EXISTS (SELECT 1 FROM login_attempts la WHERE la.user_agent = 'Luistudio demo browser');

COMMIT;
