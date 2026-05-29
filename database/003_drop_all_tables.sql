-- Luistudio - borrar todas las tablas
-- Uso: ejecutar en la base objetivo cuando se quiera reiniciar el esquema.
-- Advertencia: elimina datos de forma irreversible.

BEGIN;

DROP TABLE IF EXISTS role_permissions CASCADE;
DROP TABLE IF EXISTS two_factor_codes CASCADE;
DROP TABLE IF EXISTS two_factor_settings CASCADE;
DROP TABLE IF EXISTS password_resets CASCADE;
DROP TABLE IF EXISTS login_attempts CASCADE;
DROP TABLE IF EXISTS bookings CASCADE;
DROP TABLE IF EXISTS maintenances CASCADE;
DROP TABLE IF EXISTS rooms CASCADE;
DROP TABLE IF EXISTS buildings CASCADE;
DROP TABLE IF EXISTS notification_preferences CASCADE;
DROP TABLE IF EXISTS notification_logs CASCADE;
DROP TABLE IF EXISTS email_outbox CASCADE;
DROP TABLE IF EXISTS audit_log CASCADE;
DROP TABLE IF EXISTS users CASCADE;
DROP TABLE IF EXISTS system_config CASCADE;
DROP TABLE IF EXISTS permissions CASCADE;
DROP TABLE IF EXISTS roles CASCADE;

COMMIT;
