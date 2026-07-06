package com.luistudio.reservas.repository;

import com.luistudio.reservas.model.LoginSessionEntity;
import com.luistudio.reservas.model.UserEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoginSessionRepository extends JpaRepository<LoginSessionEntity, Long> {

    List<LoginSessionEntity> findByUsuarioAndRevokedAtIsNullOrderByCreatedAtDesc(UserEntity usuario);

    Optional<LoginSessionEntity> findByJti(String jti);

    @Modifying
    @Query("UPDATE LoginSessionEntity s SET s.revokedAt = CURRENT_TIMESTAMP WHERE s.usuario = :usuario AND s.revokedAt IS NULL")
    void revokeAllByUsuario(@Param("usuario") UserEntity usuario);

    @Query("""
        SELECT COUNT(s) > 0 FROM LoginSessionEntity s
        WHERE s.usuario = :usuario
          AND s.ip = :ip
          AND s.userAgent = :userAgent
          AND s.revokedAt IS NULL
    """)
    boolean existsByUsuarioAndIpAndUserAgent(
        @Param("usuario") UserEntity usuario,
        @Param("ip") String ip,
        @Param("userAgent") String userAgent
    );
}
