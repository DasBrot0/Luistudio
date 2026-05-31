package com.luistudio.reservas.repository;

import com.luistudio.reservas.model.UserEntity;
import com.luistudio.reservas.model.UserStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByCorreoIgnoreCase(String correo);

    Optional<UserEntity> findByCodigo(String codigo);

    long countByEstado(UserStatus estado);

    @Query("""
        SELECT u FROM UserEntity u
        WHERE (:query IS NULL OR :query = ''
          OR LOWER(u.codigo) LIKE LOWER(CONCAT('%', :query, '%'))
          OR LOWER(u.correo) LIKE LOWER(CONCAT('%', :query, '%')))
        ORDER BY
          CASE WHEN LOWER(u.codigo) = LOWER(:query) OR LOWER(u.correo) = LOWER(:query) THEN 0 ELSE 1 END,
          u.id ASC
    """)
    Page<UserEntity> searchUsers(@Param("query") String query, Pageable pageable);
}
