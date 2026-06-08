package com.luistudio.reservas.repository;

import com.luistudio.reservas.model.PabellonEntity;
import com.luistudio.reservas.model.RoomEntity;
import com.luistudio.reservas.model.RoomState;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomRepository extends JpaRepository<RoomEntity, Long> {
    Optional<RoomEntity> findByCodigo(String codigo);

    @EntityGraph(attributePaths = "pabellon")
    List<RoomEntity> findByEstadoNot(RoomState estado);

    List<RoomEntity> findByUbicacionIgnoreCaseAndEstadoNot(String ubicacion, RoomState estado);

    List<RoomEntity> findByCampusIgnoreCaseAndEstadoNot(String campus, RoomState estado);

    List<RoomEntity> findByPabellonAndEstadoNot(PabellonEntity pabellon, RoomState estado);

    @EntityGraph(attributePaths = "pabellon")
    @Query("""
        SELECT r FROM RoomEntity r
        WHERE r.estado <> :excludedState
          AND (:campus IS NULL OR :campus = '' OR LOWER(r.campus) = LOWER(:campus))
          AND (:venue IS NULL OR :venue = '' OR LOWER(r.venue) = LOWER(:venue))
          AND (:location IS NULL OR :location = '' OR LOWER(r.ubicacion) = LOWER(:location))
          AND (
            :query IS NULL OR :query = ''
            OR LOWER(r.codigo) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(r.nombre) LIKE LOWER(CONCAT('%', :query, '%'))
            OR LOWER(r.ubicacion) LIKE LOWER(CONCAT('%', :query, '%'))
          )
    """)
    Page<RoomEntity> searchActiveRooms(
        @Param("campus") String campus,
        @Param("venue") String venue,
        @Param("location") String location,
        @Param("query") String query,
        @Param("excludedState") RoomState excludedState,
        Pageable pageable
    );
}
