package com.luistudio.reservas.repository;

import com.luistudio.reservas.model.LoginAttemptEntity;
import com.luistudio.reservas.model.UserEntity;
import java.time.OffsetDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface LoginAttemptRepository
        extends JpaRepository<LoginAttemptEntity, Long>,
                JpaSpecificationExecutor<LoginAttemptEntity> {

    long countByUsuarioAndExitoFalseAndFechaIntentoAfter(UserEntity usuario, OffsetDateTime after);
}
