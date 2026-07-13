package com.luistudio.reservas.repository;

import com.luistudio.reservas.model.LoginSessionEntity;
import com.luistudio.reservas.model.UserEntity;
import java.util.List;
import java.util.Optional;
import java.time.OffsetDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoginSessionRepository extends JpaRepository<LoginSessionEntity, Long> {

    List<LoginSessionEntity> findByUsuarioAndRevokedAtIsNullOrderByCreatedAtDesc(UserEntity usuario);

    Optional<LoginSessionEntity> findByJti(String jti);

    @Modifying
    @Query("UPDATE LoginSessionEntity s SET s.revokedAt = :revokedAt, s.current = false WHERE s.usuario = :usuario AND s.revokedAt IS NULL")
    void revokeAllByUsuario(@Param("usuario") UserEntity usuario, @Param("revokedAt") OffsetDateTime revokedAt);

    boolean existsByUsuario(UserEntity usuario);

    boolean existsByUsuarioAndIp(UserEntity usuario, String ip);

    boolean existsByUsuarioAndDeviceLabel(UserEntity usuario, String deviceLabel);
}
