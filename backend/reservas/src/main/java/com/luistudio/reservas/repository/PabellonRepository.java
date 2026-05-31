package com.luistudio.reservas.repository;

import com.luistudio.reservas.model.PabellonEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PabellonRepository extends JpaRepository<PabellonEntity, Long> {
    Optional<PabellonEntity> findByCodigo(String codigo);
}
