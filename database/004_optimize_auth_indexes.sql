-- Luistudio - indices para acelerar autenticacion y seguridad
-- Aplicar en bases existentes despues de 001_init.sql.

BEGIN;

CREATE INDEX IF NOT EXISTS idx_users_email_lower ON users (LOWER(email));
CREATE INDEX IF NOT EXISTS idx_login_attempts_user_success_attempted
  ON login_attempts (user_id, success, attempted_at);
CREATE INDEX IF NOT EXISTS idx_two_factor_codes_user_used_id
  ON two_factor_codes (user_id, used, id DESC);
CREATE INDEX IF NOT EXISTS idx_password_resets_token_used
  ON password_resets (token, used);

COMMIT;
