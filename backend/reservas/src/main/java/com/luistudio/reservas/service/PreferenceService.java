package com.luistudio.reservas.service;

import com.luistudio.reservas.dto.user.NotificationPreferencesResponse;
import com.luistudio.reservas.dto.user.NotificationPreferencesUpdateRequest;
import com.luistudio.reservas.exception.BusinessException;
import com.luistudio.reservas.model.NotificationPreferenceEntity;
import com.luistudio.reservas.model.UserEntity;
import com.luistudio.reservas.repository.NotificationPreferenceRepository;
import java.time.OffsetDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PreferenceService {

    private final UserService userService;
    private final NotificationPreferenceRepository notificationPreferenceRepository;

    public PreferenceService(
        UserService userService,
        NotificationPreferenceRepository notificationPreferenceRepository
    ) {
        this.userService = userService;
        this.notificationPreferenceRepository = notificationPreferenceRepository;
    }

    @Transactional(readOnly = true)
    public NotificationPreferencesResponse getPreferences(Long userId) {
        UserEntity user = userService.getById(userId);
        NotificationPreferenceEntity pref = userService.getOrCreatePreferences(user);
        return toResponse(pref);
    }

    @Transactional
    public NotificationPreferencesResponse updatePreferences(Long userId, NotificationPreferencesUpdateRequest request) {
        UserEntity user = userService.getById(userId);
        NotificationPreferenceEntity pref = userService.getOrCreatePreferences(user);
        if (request.emailEnabled() != null) pref.setEmailHabilitado(request.emailEnabled());
        if (request.reminderEnabled() != null) pref.setRecordatorioHabilitado(request.reminderEnabled());
        if (request.bookingChangesEnabled() != null) pref.setCambiosReservaHabilitado(request.bookingChangesEnabled());
        if (request.themeMode() != null) pref.setThemeMode(parseThemeMode(request.themeMode()));
        if (request.fontScale() != null) pref.setFontScale(clampFontScale(request.fontScale()));
        pref.setActualizadoEn(OffsetDateTime.now());
        notificationPreferenceRepository.save(pref);
        return toResponse(pref);
    }

    private NotificationPreferencesResponse toResponse(NotificationPreferenceEntity pref) {
        return new NotificationPreferencesResponse(
            Boolean.TRUE.equals(pref.getEmailHabilitado()),
            Boolean.TRUE.equals(pref.getRecordatorioHabilitado()),
            Boolean.TRUE.equals(pref.getCambiosReservaHabilitado()),
            pref.getThemeMode() == null ? "LIGHT" : pref.getThemeMode(),
            pref.getFontScale() == null ? 1.0 : pref.getFontScale()
        );
    }

    private String parseThemeMode(String value) {
        String normalized = value.trim().toUpperCase();
        if (!"LIGHT".equals(normalized) && !"DARK".equals(normalized)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Tema invalido. Usa LIGHT o DARK.");
        }
        return normalized;
    }

    private double clampFontScale(double value) {
        if (value < 0.85 || value > 1.30) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Escala de texto invalida. Rango permitido: 0.85 - 1.30.");
        }
        return Math.round(value * 100.0) / 100.0;
    }
}
