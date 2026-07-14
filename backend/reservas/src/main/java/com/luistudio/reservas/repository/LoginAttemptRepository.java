package com.luistudio.reservas.repository;

import com.luistudio.reservas.model.LoginAttemptEntity;
import com.luistudio.reservas.model.UserEntity;
import java.time.OffsetDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.domain.Specification;

public interface LoginAttemptRepository
        extends JpaRepository<LoginAttemptEntity, Long>,
                JpaSpecificationExecutor<LoginAttemptEntity> {

    /**
     * Loads the user together with every result row without adding a fetch join to
     * the {@link Specification}. A fetch join in a paged specification is also
     * applied to Spring Data's count query on some Hibernate versions, which can
     * make otherwise valid login-attempt requests fail with a 500 response.
     */
    @Override
    @EntityGraph(attributePaths = "usuario")
    Page<LoginAttemptEntity> findAll(Specification<LoginAttemptEntity> specification, Pageable pageable);

    long countByUsuarioAndExitoFalseAndFechaIntentoAfter(UserEntity usuario, OffsetDateTime after);
}
