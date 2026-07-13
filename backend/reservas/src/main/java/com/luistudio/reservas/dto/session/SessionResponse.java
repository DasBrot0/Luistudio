package com.luistudio.reservas.dto.session;

import java.time.OffsetDateTime;

public record SessionResponse(
    Long id,
    String ip,
    String userAgent,
    String deviceLabel,
    OffsetDateTime createdAt,
    OffsetDateTime lastSeenAt,
    boolean current
) {
}
