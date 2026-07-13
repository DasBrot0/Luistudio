package com.luistudio.reservas.repository;

import com.luistudio.reservas.model.CampusEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampusRepository extends JpaRepository<CampusEntity, Long> {
    Optional<CampusEntity> findByNombreIgnoreCase(String nombre);
}
