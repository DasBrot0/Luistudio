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
    private static final String STUDENT_MY_BOOKINGS = "STUDENT_MY_BOOKINGS";
    private static final String STUDENT_RESERVE = "STUDENT_RESERVE";
    private static final String ADMIN_ROOMS = "ADMIN_ROOMS";
    private static final String ADMIN_BOOKINGS = "ADMIN_BOOKINGS";

    private final UserService userService;
    private final NotificationPreferenceRepository notificationPreferenceRepository;

    public PreferenceService(
        UserService userService,
        NotificationPreferenceRepository notificationPreferenceRepository
    ) {
        this.userService = userService;
        this.notificationPreferenceRepository = notificationPreferenceRepository;
    }

    @Transactional
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
        if (request.loginLandingView() != null) {
            pref.setLoginLandingView(parseLandingView(request.loginLandingView(), user.getRol().getNombre()));
        }
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
            pref.getFontScale() == null ? 1.0 : pref.getFontScale(),
            resolveLandingView(pref.getLoginLandingView(), pref.getUsuario().getRol().getNombre())
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

    private String parseLandingView(String value, String roleName) {
        String normalized = value.trim().toUpperCase();
        String role = roleName == null ? "" : roleName.trim().toUpperCase();

        if ("ADMIN".equals(role)) {
            if (ADMIN_ROOMS.equals(normalized) || ADMIN_BOOKINGS.equals(normalized)) return normalized;
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Vista inicial invalida para ADMIN.");
        }

        if (STUDENT_MY_BOOKINGS.equals(normalized) || STUDENT_RESERVE.equals(normalized)) return normalized;
        throw new BusinessException(HttpStatus.BAD_REQUEST, "Vista inicial invalida para ESTUDIANTE.");
    }

    private String resolveLandingView(String value, String roleName) {
        String role = roleName == null ? "" : roleName.trim().toUpperCase();
        if ("ADMIN".equals(role)) {
            if (ADMIN_BOOKINGS.equals(value) || ADMIN_ROOMS.equals(value)) return value;
            return ADMIN_ROOMS;
        }
        if (STUDENT_RESERVE.equals(value) || STUDENT_MY_BOOKINGS.equals(value)) return value;
        return STUDENT_MY_BOOKINGS;
    }
}
