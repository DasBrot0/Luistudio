package com.luistudio.reservas.dto.admin;

import java.time.OffsetDateTime;

public record AnnouncementResponse(
    Long id,
    String title,
    String announcementType,
    OffsetDateTime createdAt,
    int recipientCount
) {
}
