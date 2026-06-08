package com.luistudio.reservas.repository;

import com.luistudio.reservas.model.EmailOutboxEntity;
import com.luistudio.reservas.model.EmailStatus;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EmailOutboxRepository extends JpaRepository<EmailOutboxEntity, Long> {

    @Query("""
        SELECT e FROM EmailOutboxEntity e
        WHERE e.estado = :status AND e.disponibleDesde <= :nowValue
        ORDER BY e.id ASC
    """)
    List<EmailOutboxEntity> findReadyToProcess(
        @Param("status") EmailStatus status,
        @Param("nowValue") OffsetDateTime nowValue,
        Pageable pageable
    );

    @Query(
        value = """
            SELECT EXISTS (
                SELECT 1
                FROM email_outbox e
                WHERE e.recipient = :recipient
                  AND e.status IN ('PENDIENTE', 'ENVIADO')
                  AND e.payload IS NOT NULL
                  AND e.payload->>'reminderType' = :reminderType
                  AND e.payload->>'reservationId' = CAST(:reservationId AS TEXT)
            )
        """,
        nativeQuery = true
    )
    boolean existsReminderByRecipientAndTypeAndReservation(
        @Param("recipient") String recipient,
        @Param("reminderType") String reminderType,
        @Param("reservationId") Long reservationId
    );
}
