ALTER TABLE notification_preferences
ADD COLUMN IF NOT EXISTS notification_settings TEXT;

ALTER TABLE notification_preferences
DROP CONSTRAINT IF EXISTS chk_preferencia_landing_view;

ALTER TABLE notification_preferences
ADD CONSTRAINT chk_preferencia_landing_view CHECK (
  login_landing_view IN ('STUDENT_MY_BOOKINGS', 'STUDENT_RESERVE', 'ADMIN_ROOMS', 'ADMIN_PROFILES', 'ADMIN_BOOKINGS')
);
