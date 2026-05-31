package com.luistudio.reservas.dto.user;

public record NotificationPreferencesUpdateRequest(
    Boolean emailEnabled,
    Boolean reminderEnabled,
    Boolean bookingChangesEnabled,
    String themeMode,
    Double fontScale
) {
}
