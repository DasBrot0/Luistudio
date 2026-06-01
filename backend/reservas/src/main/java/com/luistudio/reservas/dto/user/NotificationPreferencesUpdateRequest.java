package com.luistudio.reservas.dto.user;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record NotificationPreferencesUpdateRequest(
    Boolean emailEnabled,
    Boolean reminderEnabled,
    Boolean bookingChangesEnabled,
    @Size(max = 10) String themeMode,
    @Min(0) @Max(3) Double fontScale,
    @Size(max = 30) String loginLandingView
) {
}
