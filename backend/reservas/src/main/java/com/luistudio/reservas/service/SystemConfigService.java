package com.luistudio.reservas.service;

import com.luistudio.reservas.dto.admin.AdminConfigResponse;
import com.luistudio.reservas.dto.admin.AdminConfigUpdateRequest;
import com.luistudio.reservas.model.SystemConfigEntity;
import com.luistudio.reservas.repository.SystemConfigRepository;
import java.time.OffsetDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SystemConfigService {

    public static final String KEY_MAX_ACTIVE_BOOKINGS = "max_reservas_simultaneas";
    public static final String KEY_MAX_DURATION_MINUTES = "duracion_maxima_minutos";
    private static final String KEY_CAMPUS_SLOT_MINUTES_PREFIX = "campus_slot_minutos_";

    private final SystemConfigRepository systemConfigRepository;

    public SystemConfigService(SystemConfigRepository systemConfigRepository) {
        this.systemConfigRepository = systemConfigRepository;
    }

    @Transactional
    public void ensureDefaults() {
        upsert(KEY_MAX_ACTIVE_BOOKINGS, "1");
        upsert(KEY_MAX_DURATION_MINUTES, "120");
        upsert(campusSlotKey("Monterrico"), "60");
        upsert(campusSlotKey("Mayorazgo"), "45");
    }

    @Transactional(readOnly = true)
    public AdminConfigResponse getConfig() {
        int maxBookings = Integer.parseInt(getValue(KEY_MAX_ACTIVE_BOOKINGS, "1"));
        int maxDuration = Integer.parseInt(getValue(KEY_MAX_DURATION_MINUTES, "120"));
        return new AdminConfigResponse(maxBookings, maxDuration);
    }

    @Transactional
    public AdminConfigResponse updateConfig(AdminConfigUpdateRequest request) {
        upsert(KEY_MAX_ACTIVE_BOOKINGS, String.valueOf(request.maxActiveBookings()));
        upsert(KEY_MAX_DURATION_MINUTES, String.valueOf(request.maxDurationMinutes()));
        return getConfig();
    }

    @Transactional(readOnly = true)
    public int getMaxActiveBookings() {
        return Integer.parseInt(getValue(KEY_MAX_ACTIVE_BOOKINGS, "1"));
    }

    @Transactional(readOnly = true)
    public int getMaxDurationMinutes() {
        return Integer.parseInt(getValue(KEY_MAX_DURATION_MINUTES, "120"));
    }

    @Transactional(readOnly = true)
    public int getCampusSlotMinutes(String campus) {
        return Integer.parseInt(getValue(campusSlotKey(campus), String.valueOf(getDefaultCampusSlotMinutes(campus))));
    }

    @Transactional
    public void setCampusSlotMinutes(String campus, int slotMinutes) {
        validateCampusSlotMinutes(slotMinutes);
        upsert(campusSlotKey(campus), String.valueOf(slotMinutes));
    }

    public void validateCampusSlotMinutes(int slotMinutes) {
        if (slotMinutes != 30 && slotMinutes != 45 && slotMinutes != 60 && slotMinutes != 120) {
            throw new com.luistudio.reservas.exception.BusinessException(
                org.springframework.http.HttpStatus.BAD_REQUEST,
                "La duracion por reserva debe ser 30, 45, 60 o 120 minutos"
            );
        }
    }

    private String getValue(String key, String defaultValue) {
        return systemConfigRepository.findByClave(key)
            .map(SystemConfigEntity::getValor)
            .orElse(defaultValue);
    }

    private void upsert(String key, String value) {
        SystemConfigEntity entity = systemConfigRepository.findByClave(key).orElseGet(SystemConfigEntity::new);
        entity.setClave(key);
        entity.setValor(value);
        entity.setUpdatedAt(OffsetDateTime.now());
        systemConfigRepository.save(entity);
    }

    private String campusSlotKey(String campus) {
        String normalized = campus == null ? "default" : campus.trim().toLowerCase();
        normalized = normalized.replaceAll("[^a-z0-9]+", "_");
        return KEY_CAMPUS_SLOT_MINUTES_PREFIX + normalized;
    }

    private int getDefaultCampusSlotMinutes(String campus) {
        if ("Mayorazgo".equalsIgnoreCase(campus)) {
            return 45;
        }
        return 60;
    }
}
