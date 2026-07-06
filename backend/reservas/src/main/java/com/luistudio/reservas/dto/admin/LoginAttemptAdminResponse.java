package com.luistudio.reservas.dto.admin;

import java.time.OffsetDateTime;

public record LoginAttemptAdminResponse(
    Long id,
    Long userId,
    String userEmail,
    String ip,
    String userAgent,
    OffsetDateTime attemptedAt,
    boolean success,
    OffsetDateTime lockedUntil
) {
}
