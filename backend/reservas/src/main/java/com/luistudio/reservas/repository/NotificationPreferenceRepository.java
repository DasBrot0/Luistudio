package com.luistudio.reservas.repository;

import com.luistudio.reservas.model.NotificationPreferenceEntity;
import com.luistudio.reservas.model.UserEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreferenceEntity, Long> {
    Optional<NotificationPreferenceEntity> findByUsuario(UserEntity usuario);
}
