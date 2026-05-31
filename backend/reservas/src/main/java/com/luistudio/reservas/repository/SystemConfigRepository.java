package com.luistudio.reservas.repository;

import com.luistudio.reservas.model.SystemConfigEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SystemConfigRepository extends JpaRepository<SystemConfigEntity, Long> {
    Optional<SystemConfigEntity> findByClave(String clave);
}
