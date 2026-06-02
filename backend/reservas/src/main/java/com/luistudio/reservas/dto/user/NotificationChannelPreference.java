package com.luistudio.reservas.dto.user;

public record NotificationChannelPreference(
    Boolean app,
    Boolean email
) {
}
