package com.luistudio.reservas.repository;

import com.luistudio.reservas.model.AuditLogEntity;
import com.luistudio.reservas.model.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.OffsetDateTime;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {

    @Query("""
        SELECT a FROM AuditLogEntity a
        WHERE a.actor = :actor
          AND a.accion IN ('LOGIN_SUCCESS', 'LOGOUT_CURRENT', 'LOGOUT_REMOTE', 'LOGOUT_ALL',
                           'SENSITIVE_CHANGE_CONFIRMED', 'LOGIN_UNUSUAL_ACCESS')
          AND (:from IS NULL OR a.creadoEn >= :from)
          AND (:to IS NULL OR a.creadoEn <= :to)
        ORDER BY a.creadoEn DESC
    """)
    Page<AuditLogEntity> findActivityByActor(
        @Param("actor") UserEntity actor,
        @Param("from") OffsetDateTime from,
        @Param("to") OffsetDateTime to,
        Pageable pageable
    );
}
