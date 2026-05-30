package com.luistudio.reservas.repository;

import com.luistudio.reservas.model.PasswordResetEntity;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetRepository extends JpaRepository<PasswordResetEntity, Long> {
    Optional<PasswordResetEntity> findByTokenAndUsadoFalse(String token);

    void deleteByExpiraEnBefore(OffsetDateTime time);
}
