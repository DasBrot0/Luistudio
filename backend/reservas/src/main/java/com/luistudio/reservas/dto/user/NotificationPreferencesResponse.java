package com.luistudio.reservas.dto.user;

import java.util.Map;

public record NotificationPreferencesResponse(
    boolean emailEnabled,
    boolean reminderEnabled,
    boolean bookingChangesEnabled,
    Map<String, NotificationChannelPreference> notificationSettings,
    String themeMode,
    double fontScale,
    String loginLandingView
) {
}
