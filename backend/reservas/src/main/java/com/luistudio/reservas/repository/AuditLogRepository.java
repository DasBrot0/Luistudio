package com.luistudio.reservas.repository;

import com.luistudio.reservas.model.AuditLogEntity;
import com.luistudio.reservas.model.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.OffsetDateTime;
import java.util.Collection;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {

    Page<AuditLogEntity> findByActorAndAccionInOrderByCreadoEnDesc(
        UserEntity actor,
        Collection<String> actions,
        Pageable pageable
    );

    Page<AuditLogEntity> findByActorAndAccionInAndCreadoEnGreaterThanEqualOrderByCreadoEnDesc(
        UserEntity actor,
        Collection<String> actions,
        OffsetDateTime from,
        Pageable pageable
    );

    Page<AuditLogEntity> findByActorAndAccionInAndCreadoEnLessThanEqualOrderByCreadoEnDesc(
        UserEntity actor,
        Collection<String> actions,
        OffsetDateTime to,
        Pageable pageable
    );

    Page<AuditLogEntity> findByActorAndAccionInAndCreadoEnBetweenOrderByCreadoEnDesc(
        UserEntity actor,
        Collection<String> actions,
        OffsetDateTime from,
        OffsetDateTime to,
        Pageable pageable
    );
}
