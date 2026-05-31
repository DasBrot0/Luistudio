package com.luistudio.reservas.dto.user;

public record NotificationPreferencesResponse(
    boolean emailEnabled,
    boolean reminderEnabled,
    boolean bookingChangesEnabled,
    String themeMode,
    double fontScale,
    String loginLandingView
) {
}
