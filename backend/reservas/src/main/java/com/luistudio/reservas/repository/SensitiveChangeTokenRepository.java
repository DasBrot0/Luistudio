package com.luistudio.reservas.repository;

import com.luistudio.reservas.model.SensitiveChangeTokenEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SensitiveChangeTokenRepository extends JpaRepository<SensitiveChangeTokenEntity, Long> {
    Optional<SensitiveChangeTokenEntity> findByTokenAndUsedFalse(String token);
}
