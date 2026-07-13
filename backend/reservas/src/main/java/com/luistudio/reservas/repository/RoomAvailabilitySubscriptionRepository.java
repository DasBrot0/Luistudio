package com.luistudio.reservas.repository;

import com.luistudio.reservas.model.RoomAvailabilitySubscriptionEntity;
import com.luistudio.reservas.model.RoomEntity;
import com.luistudio.reservas.model.UserEntity;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RoomAvailabilitySubscriptionRepository extends JpaRepository<RoomAvailabilitySubscriptionEntity, Long> {

    @Query("""
        SELECT s FROM RoomAvailabilitySubscriptionEntity s
        WHERE s.usuario = :user
          AND s.sala = :room
          AND s.status IN ('ACTIVA', 'EN_COLA')
    """)
    Optional<RoomAvailabilitySubscriptionEntity> findActiveByUserAndRoom(
        @Param("user") UserEntity user,
        @Param("room") RoomEntity room
    );

    @Query("""
        SELECT s FROM RoomAvailabilitySubscriptionEntity s
        WHERE s.sala = :room
          AND s.targetDate = :targetDate
          AND s.startTime = :startTime
          AND s.endTime = :endTime
          AND s.status = 'ACTIVA'
    """)
    List<RoomAvailabilitySubscriptionEntity> findActiveSubscriptionsForRoom(
        @Param("room") RoomEntity room,
        @Param("targetDate") LocalDate targetDate,
        @Param("startTime") LocalTime startTime,
        @Param("endTime") LocalTime endTime
    );

    List<RoomAvailabilitySubscriptionEntity> findByUsuarioAndStatus(UserEntity usuario, String status);

    List<RoomAvailabilitySubscriptionEntity> findByStatus(String status);
}
