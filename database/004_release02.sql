-- Luistudio – Release 02 schema migration
-- Execute on top of 001_init.sql (Release 1 schema)

BEGIN;

-- 1. Agregar user_agent a login_attempts
ALTER TABLE login_attempts
    ADD COLUMN IF NOT EXISTS user_agent VARCHAR(512);

-- 2. Tabla login_sessions
CREATE TABLE IF NOT EXISTS login_sessions (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT NOT NULL,
    jti           VARCHAR(36) NOT NULL UNIQUE,
    ip            VARCHAR(64),
    user_agent    VARCHAR(512),
    device_label  VARCHAR(120),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_seen_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    revoked_at    TIMESTAMPTZ,
    current       BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_login_session_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_login_sessions_user_revoked ON login_sessions (user_id, revoked_at);
CREATE INDEX IF NOT EXISTS idx_login_sessions_jti         ON login_sessions (jti);

-- 3. Tabla sensitive_change_tokens
CREATE TABLE IF NOT EXISTS sensitive_change_tokens (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    action_type VARCHAR(40) NOT NULL,
    token       VARCHAR(255) NOT NULL UNIQUE,
    payload     TEXT,
    expires_at  TIMESTAMPTZ NOT NULL,
    used        BOOLEAN NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_sensitive_token_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_sensitive_tokens_token_used ON sensitive_change_tokens (token, used);

-- 4. Columna attendance_status en bookings
ALTER TABLE bookings
    ADD COLUMN IF NOT EXISTS attendance_status VARCHAR(20)
        CONSTRAINT chk_booking_attendance CHECK (attendance_status IN ('ASISTIO', 'INASISTIO'));

-- 5. Tabla attendance_records
CREATE TABLE IF NOT EXISTS attendance_records (
    id               BIGSERIAL PRIMARY KEY,
    booking_id       BIGINT NOT NULL UNIQUE,
    user_id          BIGINT NOT NULL,
    recorded_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    tolerance_minutes INTEGER NOT NULL DEFAULT 15,
    CONSTRAINT fk_attendance_booking FOREIGN KEY (booking_id) REFERENCES bookings(id),
    CONSTRAINT fk_attendance_user    FOREIGN KEY (user_id)    REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_attendance_records_booking ON attendance_records (booking_id);
CREATE INDEX IF NOT EXISTS idx_attendance_records_user    ON attendance_records (user_id);

-- 6. Tabla room_availability_subscriptions
CREATE TABLE IF NOT EXISTS room_availability_subscriptions (
    id          BIGSERIAL PRIMARY KEY,
    user_id     BIGINT NOT NULL,
    room_id     BIGINT NOT NULL,
    target_date DATE NOT NULL,
    start_time  TIME NOT NULL,
    end_time    TIME NOT NULL,
    status      VARCHAR(20) NOT NULL DEFAULT 'ACTIVA'
        CONSTRAINT chk_avail_sub_status CHECK (status IN ('ACTIVA', 'NOTIFICADA', 'CANCELADA')),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    notified_at TIMESTAMPTZ,
    CONSTRAINT fk_avail_sub_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_avail_sub_room FOREIGN KEY (room_id) REFERENCES rooms(id)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_avail_sub_active
    ON room_availability_subscriptions (user_id, room_id, target_date, start_time, end_time)
    WHERE status = 'ACTIVA';

CREATE INDEX IF NOT EXISTS idx_avail_sub_user               ON room_availability_subscriptions (user_id);
CREATE INDEX IF NOT EXISTS idx_avail_sub_room_date_status   ON room_availability_subscriptions (room_id, target_date, status);

-- 7. Tabla institutional_announcements
CREATE TABLE IF NOT EXISTS institutional_announcements (
    id               BIGSERIAL PRIMARY KEY,
    author_user_id   BIGINT NOT NULL,
    title            VARCHAR(160) NOT NULL,
    content          TEXT NOT NULL,
    announcement_type VARCHAR(40) NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'PUBLICADO',
    created_at       TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_announcement_author FOREIGN KEY (author_user_id) REFERENCES users(id)
);

CREATE INDEX IF NOT EXISTS idx_announcements_created_at ON institutional_announcements (created_at DESC);

COMMIT;
