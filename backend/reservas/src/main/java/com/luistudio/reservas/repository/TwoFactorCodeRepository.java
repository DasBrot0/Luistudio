package com.luistudio.reservas.repository;

import com.luistudio.reservas.model.TwoFactorCodeEntity;
import com.luistudio.reservas.model.UserEntity;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TwoFactorCodeRepository extends JpaRepository<TwoFactorCodeEntity, Long> {
    Optional<TwoFactorCodeEntity> findTopByUsuarioAndUsadoFalseOrderByIdDesc(UserEntity usuario);

    void deleteByExpiraAtBefore(OffsetDateTime time);
}
