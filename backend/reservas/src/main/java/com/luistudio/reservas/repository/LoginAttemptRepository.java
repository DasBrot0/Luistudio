package com.luistudio.reservas.repository;

import com.luistudio.reservas.model.LoginAttemptEntity;
import com.luistudio.reservas.model.UserEntity;
import java.time.OffsetDateTime;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginAttemptRepository extends JpaRepository<LoginAttemptEntity, Long> {
    long countByUsuarioAndExitoFalseAndFechaIntentoAfter(UserEntity usuario, OffsetDateTime after);
}
