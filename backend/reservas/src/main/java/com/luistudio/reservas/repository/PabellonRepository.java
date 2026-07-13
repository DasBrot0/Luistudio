package com.luistudio.reservas.repository;

import com.luistudio.reservas.model.PabellonEntity;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PabellonRepository extends JpaRepository<PabellonEntity, Long> {
    Optional<PabellonEntity> findByCodigo(String codigo);

    @Query("""
        SELECT p FROM PabellonEntity p
        WHERE p.mapEnabled = TRUE OR p.mapEnabled IS NULL
        ORDER BY p.campus.nombre, COALESCE(p.mapOrder, 0), p.codigo
    """)
    List<PabellonEntity> findMapEnabledOrdered();
}
