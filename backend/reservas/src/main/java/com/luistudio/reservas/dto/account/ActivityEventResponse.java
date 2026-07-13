package com.luistudio.reservas.dto.account;

import java.time.OffsetDateTime;

public record ActivityEventResponse(
    Long id,
    String action,
    String detail,
    OffsetDateTime createdAt
) {
}
