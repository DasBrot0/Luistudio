package com.luistudio.reservas.repository;

import com.luistudio.reservas.model.UserEntity;
import com.luistudio.reservas.model.UserStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    @EntityGraph(attributePaths = "rol")
    @Query("""
        SELECT u FROM UserEntity u
        WHERE LOWER(u.correo) = LOWER(:correo)
    """)
    Optional<UserEntity> findByCorreoIgnoreCase(@Param("correo") String correo);

    Optional<UserEntity> findByCodigo(String codigo);
    Optional<UserEntity> findByCodigoIgnoreCase(String codigo);

    long countByEstado(UserStatus estado);

    @Query("""
        SELECT u FROM UserEntity u
        WHERE (:query IS NULL OR :query = ''
          OR LOWER(u.codigo) LIKE LOWER(CONCAT('%', :query, '%'))
          OR LOWER(u.correo) LIKE LOWER(CONCAT('%', :query, '%'))
          OR LOWER(u.nombres) LIKE LOWER(CONCAT('%', :query, '%'))
          OR LOWER(u.apellidos) LIKE LOWER(CONCAT('%', :query, '%'))
          OR LOWER(CONCAT(u.nombres, ' ', u.apellidos)) LIKE LOWER(CONCAT('%', :query, '%')))
          AND (:year IS NULL OR :year = '' OR u.codigo LIKE CONCAT(:year, '%'))
          AND (:status IS NULL OR u.estado = :status)
          AND (:blocked IS NULL OR (:blocked = true AND u.lockedUntil IS NOT NULL AND u.lockedUntil > CURRENT_TIMESTAMP))
    """)
    Page<UserEntity> searchUsers(
        @Param("query") String query,
        @Param("year") String year,
        @Param("status") UserStatus status,
        @Param("blocked") Boolean blocked,
        Pageable pageable
    );
}
