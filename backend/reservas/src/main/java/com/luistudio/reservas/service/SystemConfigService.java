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

    private final SystemConfigRepository systemConfigRepository;

    public SystemConfigService(SystemConfigRepository systemConfigRepository) {
        this.systemConfigRepository = systemConfigRepository;
    }

    @Transactional
    public void ensureDefaults() {
        upsert(KEY_MAX_ACTIVE_BOOKINGS, "1");
        upsert(KEY_MAX_DURATION_MINUTES, "120");
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
}
