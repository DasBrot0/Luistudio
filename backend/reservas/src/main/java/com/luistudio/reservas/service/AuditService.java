package com.luistudio.reservas.service;

import com.luistudio.reservas.model.AuditLogEntity;
import com.luistudio.reservas.model.UserEntity;
import com.luistudio.reservas.repository.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public AuditService(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public void record(UserEntity actor, String action, String entity, String entityId, String detail) {
        AuditLogEntity log = new AuditLogEntity();
        log.setActor(actor);
        log.setAccion(action);
        log.setEntidad(entity);
        log.setEntidadId(entityId);
        log.setDetalle(detail);
        auditLogRepository.save(log);
    }
}
