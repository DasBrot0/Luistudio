package com.luistudio.reservas.repository;

import com.luistudio.reservas.model.MaintenanceEntity;
import com.luistudio.reservas.model.RoomEntity;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MaintenanceRepository extends JpaRepository<MaintenanceEntity, Long> {
    List<MaintenanceEntity> findBySalaOrderByInicioDesc(RoomEntity sala);

    @Query("""
        SELECT m FROM MaintenanceEntity m
        WHERE m.sala = :room
          AND m.inicio < :toTime
          AND m.fin > :fromTime
    """)
    List<MaintenanceEntity> findOverlapping(
        @Param("room") RoomEntity room,
        @Param("fromTime") OffsetDateTime fromTime,
        @Param("toTime") OffsetDateTime toTime
    );

    @Query("""
        SELECT m FROM MaintenanceEntity m
        WHERE m.inicio <= :nowValue AND m.fin >= :nowValue
    """)
    List<MaintenanceEntity> findActiveAt(@Param("nowValue") OffsetDateTime nowValue);
}
