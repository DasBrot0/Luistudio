package com.luistudio.reservas.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luistudio.reservas.dto.user.NotificationChannelPreference;
import com.luistudio.reservas.dto.user.NotificationPreferencesResponse;
import com.luistudio.reservas.dto.user.NotificationPreferencesUpdateRequest;
import com.luistudio.reservas.exception.BusinessException;
import com.luistudio.reservas.model.NotificationPreferenceEntity;
import com.luistudio.reservas.model.UserEntity;
import com.luistudio.reservas.repository.NotificationPreferenceRepository;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PreferenceService {
    private static final Logger log = LoggerFactory.getLogger(PreferenceService.class);
    private static final String STUDENT_MY_BOOKINGS = "STUDENT_MY_BOOKINGS";
    private static final String STUDENT_RESERVE = "STUDENT_RESERVE";
    private static final String ADMIN_ROOMS = "ADMIN_ROOMS";
    private static final String ADMIN_PROFILES = "ADMIN_PROFILES";
    private static final String ADMIN_BOOKINGS = "ADMIN_BOOKINGS";
    private static final String BOOKING_CONFIRMATION = "BOOKING_CONFIRMATION";
    private static final String BOOKING_UPDATE = "BOOKING_UPDATE";
    private static final String BOOKING_CANCELLATION = "BOOKING_CANCELLATION";
    private static final String BOOKING_REMINDER = "BOOKING_REMINDER";
    private static final String ROOM_MAINTENANCE = "ROOM_MAINTENANCE";
    private static final String PROFILE_STATUS = "PROFILE_STATUS";
    private static final TypeReference<Map<String, NotificationChannelPreference>> SETTINGS_TYPE = new TypeReference<>() {};

    private final UserService userService;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final ObjectMapper objectMapper;

    public PreferenceService(
        UserService userService,
        NotificationPreferenceRepository notificationPreferenceRepository,
        ObjectMapper objectMapper
    ) {
        this.userService = userService;
        this.notificationPreferenceRepository = notificationPreferenceRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public NotificationPreferencesResponse getPreferences(Long userId) {
        UserEntity user = userService.getById(userId);
        NotificationPreferenceEntity pref = userService.getOrCreatePreferences(user);
        log.info(
            "preferences_loaded userIdHash={} actorRole={} hasCustomSettings={}",
            hashId(userId),
            user.getRol().getNombre(),
            pref.getNotificationSettings() != null && !pref.getNotificationSettings().isBlank()
        );
        return toResponse(pref);
    }

    @Transactional
    public NotificationPreferencesResponse updatePreferences(Long userId, NotificationPreferencesUpdateRequest request) {
        UserEntity user = userService.getById(userId);
        NotificationPreferenceEntity pref = userService.getOrCreatePreferences(user);
        if (request.emailEnabled() != null) pref.setEmailHabilitado(request.emailEnabled());
        if (request.reminderEnabled() != null) pref.setRecordatorioHabilitado(request.reminderEnabled());
        if (request.bookingChangesEnabled() != null) pref.setCambiosReservaHabilitado(request.bookingChangesEnabled());
        if (request.notificationSettings() != null) {
            Map<String, NotificationChannelPreference> normalized = normalizeSettings(
                request.notificationSettings(),
                user.getRol().getNombre(),
                Boolean.TRUE.equals(pref.getEmailHabilitado()),
                Boolean.TRUE.equals(pref.getRecordatorioHabilitado()),
                Boolean.TRUE.equals(pref.getCambiosReservaHabilitado())
            );
            pref.setNotificationSettings(writeSettings(normalized));
            pref.setEmailHabilitado(hasAnyEmailEnabled(normalized));
            pref.setRecordatorioHabilitado(isEmailEnabled(normalized, BOOKING_REMINDER));
            pref.setCambiosReservaHabilitado(
                isEmailEnabled(normalized, BOOKING_UPDATE) || isEmailEnabled(normalized, BOOKING_CANCELLATION)
            );
        }
        if (request.themeMode() != null) pref.setThemeMode(parseThemeMode(request.themeMode()));
        if (request.fontScale() != null) pref.setFontScale(clampFontScale(request.fontScale()));
        if (request.loginLandingView() != null) {
            pref.setLoginLandingView(parseLandingView(request.loginLandingView(), user.getRol().getNombre()));
        }
        pref.setActualizadoEn(OffsetDateTime.now());
        notificationPreferenceRepository.save(pref);
        log.info("preferences_saved userIdHash={} actorRole={}", hashId(userId), user.getRol().getNombre());
        return toResponse(pref);
    }

    private NotificationPreferencesResponse toResponse(NotificationPreferenceEntity pref) {
        Map<String, NotificationChannelPreference> settings = normalizeSettings(
            readSettings(pref.getNotificationSettings()),
            pref.getUsuario().getRol().getNombre(),
            Boolean.TRUE.equals(pref.getEmailHabilitado()),
            Boolean.TRUE.equals(pref.getRecordatorioHabilitado()),
            Boolean.TRUE.equals(pref.getCambiosReservaHabilitado())
        );
        return new NotificationPreferencesResponse(
            Boolean.TRUE.equals(pref.getEmailHabilitado()),
            Boolean.TRUE.equals(pref.getRecordatorioHabilitado()),
            Boolean.TRUE.equals(pref.getCambiosReservaHabilitado()),
            settings,
            pref.getThemeMode() == null ? "LIGHT" : pref.getThemeMode(),
            pref.getFontScale() == null ? 1.0 : pref.getFontScale(),
            resolveLandingView(pref.getLoginLandingView(), pref.getUsuario().getRol().getNombre())
        );
    }

    private Map<String, NotificationChannelPreference> readSettings(String raw) {
        if (raw == null || raw.isBlank()) return Map.of();
        try {
            return objectMapper.readValue(raw, SETTINGS_TYPE);
        } catch (Exception ex) {
            log.warn("preferences_settings_deserialize_failed message={}", sanitize(ex.getMessage()));
            return Map.of();
        }
    }

    private String writeSettings(Map<String, NotificationChannelPreference> settings) {
        try {
            return objectMapper.writeValueAsString(settings);
        } catch (Exception ex) {
            log.error("preferences_settings_serialize_failed message={}", sanitize(ex.getMessage()), ex);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "No se pudieron guardar las preferencias de notificación.");
        }
    }

    private Map<String, NotificationChannelPreference> normalizeSettings(
        Map<String, NotificationChannelPreference> incoming,
        String roleName,
        boolean emailEnabled,
        boolean reminderEnabled,
        boolean bookingChangesEnabled
    ) {
        Map<String, NotificationChannelPreference> defaults = defaultSettings(roleName, emailEnabled, reminderEnabled, bookingChangesEnabled);
        Map<String, NotificationChannelPreference> result = new LinkedHashMap<>();
        Set<String> allowedKeys = defaults.keySet();
        int incomingKeys = incoming == null ? 0 : incoming.size();
        for (String key : allowedKeys) {
            NotificationChannelPreference base = defaults.get(key);
            NotificationChannelPreference value = incoming == null ? null : incoming.get(key);
            boolean app = value == null || value.app() == null ? Boolean.TRUE.equals(base.app()) : Boolean.TRUE.equals(value.app());
            boolean email = value == null || value.email() == null ? Boolean.TRUE.equals(base.email()) : Boolean.TRUE.equals(value.email());
            result.put(key, new NotificationChannelPreference(app, email));
        }
        log.debug(
            "preferences_settings_normalized role={} incomingKeys={} validKeys={}",
            roleName,
            incomingKeys,
            result.size()
        );
        return result;
    }

    private Map<String, NotificationChannelPreference> defaultSettings(
        String roleName,
        boolean emailEnabled,
        boolean reminderEnabled,
        boolean bookingChangesEnabled
    ) {
        String role = roleName == null ? "" : roleName.trim().toUpperCase();
        Map<String, NotificationChannelPreference> settings = new LinkedHashMap<>();
        if ("ADMIN".equals(role)) {
            settings.put(ROOM_MAINTENANCE, new NotificationChannelPreference(true, emailEnabled));
            settings.put(PROFILE_STATUS, new NotificationChannelPreference(true, false));
        } else {
            settings.put(BOOKING_UPDATE, new NotificationChannelPreference(true, emailEnabled && bookingChangesEnabled));
            settings.put(BOOKING_CANCELLATION, new NotificationChannelPreference(true, emailEnabled && bookingChangesEnabled));
            settings.put(BOOKING_CONFIRMATION, new NotificationChannelPreference(true, emailEnabled));
            settings.put(BOOKING_REMINDER, new NotificationChannelPreference(true, emailEnabled && reminderEnabled));
        }
        return settings;
    }

    private boolean hasAnyEmailEnabled(Map<String, NotificationChannelPreference> settings) {
        return settings.values().stream().anyMatch(item -> Boolean.TRUE.equals(item.email()));
    }

    private boolean isEmailEnabled(Map<String, NotificationChannelPreference> settings, String key) {
        NotificationChannelPreference preference = settings.get(key);
        return preference != null && Boolean.TRUE.equals(preference.email());
    }

    private String parseThemeMode(String value) {
        String normalized = value.trim().toUpperCase();
        if (!"LIGHT".equals(normalized) && !"DARK".equals(normalized)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Tema inválido. Usa LIGHT o DARK.");
        }
        return normalized;
    }

    private double clampFontScale(double value) {
        if (value < 0.85 || value > 1.30) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Escala de texto inválida. Rango permitido: 0.85 - 1.30.");
        }
        return Math.round(value * 100.0) / 100.0;
    }

    private String parseLandingView(String value, String roleName) {
        String normalized = value.trim().toUpperCase();
        String role = roleName == null ? "" : roleName.trim().toUpperCase();

        if ("ADMIN".equals(role)) {
            if (ADMIN_ROOMS.equals(normalized) || ADMIN_PROFILES.equals(normalized) || ADMIN_BOOKINGS.equals(normalized)) return normalized;
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Vista inicial inválida para ADMIN.");
        }

        if (STUDENT_MY_BOOKINGS.equals(normalized) || STUDENT_RESERVE.equals(normalized)) return normalized;
        throw new BusinessException(HttpStatus.BAD_REQUEST, "Vista inicial inválida para ESTUDIANTE.");
    }

    private String resolveLandingView(String value, String roleName) {
        String role = roleName == null ? "" : roleName.trim().toUpperCase();
        if ("ADMIN".equals(role)) {
            if (ADMIN_BOOKINGS.equals(value) || ADMIN_PROFILES.equals(value) || ADMIN_ROOMS.equals(value)) return value;
            return ADMIN_ROOMS;
        }
        if (STUDENT_RESERVE.equals(value) || STUDENT_MY_BOOKINGS.equals(value)) return value;
        return STUDENT_MY_BOOKINGS;
    }

    private String hashId(Long userId) {
        return userId == null ? "anonymous" : Integer.toHexString(Long.hashCode(userId));
    }

    private String sanitize(String message) {
        if (message == null || message.isBlank()) {
            return "n/a";
        }
        return message.replaceAll("[\\r\\n]+", " ");
    }
}
