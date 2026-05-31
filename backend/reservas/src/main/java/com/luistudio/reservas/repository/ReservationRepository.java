package com.luistudio.reservas.repository;

import com.luistudio.reservas.model.ReservationEntity;
import com.luistudio.reservas.model.ReservationStatus;
import com.luistudio.reservas.model.RoomEntity;
import com.luistudio.reservas.model.UserEntity;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationRepository extends JpaRepository<ReservationEntity, Long> {

    List<ReservationEntity> findByUsuarioOrderByFechaDescHoraInicioDesc(UserEntity usuario);

    long countByUsuarioAndEstado(UserEntity usuario, ReservationStatus estado);

    Page<ReservationEntity> findByEstadoOrderByFechaDescHoraInicioDesc(ReservationStatus estado, Pageable pageable);

    Page<ReservationEntity> findByFechaOrderByHoraInicioDesc(LocalDate fecha, Pageable pageable);

    @Query("""
        SELECT r FROM ReservationEntity r
        WHERE (:estado IS NULL OR r.estado = :estado)
          AND (:fecha IS NULL OR r.fecha = :fecha)
        ORDER BY r.fecha DESC, r.horaInicio DESC
    """)
    Page<ReservationEntity> findForAdmin(
        @Param("estado") ReservationStatus estado,
        @Param("fecha") LocalDate fecha,
        Pageable pageable
    );

    @Query("""
        SELECT COUNT(r) > 0 FROM ReservationEntity r
        WHERE r.sala = :room
          AND r.fecha = :date
          AND r.estado = com.luistudio.reservas.model.ReservationStatus.ACTIVA
          AND r.horaInicio < :endTime
          AND r.horaFin > :startTime
          AND (:excludeId IS NULL OR r.id <> :excludeId)
    """)
    boolean existsOverlapping(
        @Param("room") RoomEntity room,
        @Param("date") LocalDate date,
        @Param("startTime") LocalTime startTime,
        @Param("endTime") LocalTime endTime,
        @Param("excludeId") Long excludeId
    );

    @Query("""
        SELECT COUNT(r) > 0 FROM ReservationEntity r
        WHERE r.sala = :room
          AND r.estado = com.luistudio.reservas.model.ReservationStatus.ACTIVA
          AND (r.fecha > :today OR (r.fecha = :today AND r.horaInicio >= :nowTime))
    """)
    boolean existsFutureActiveReservations(
        @Param("room") RoomEntity room,
        @Param("today") LocalDate today,
        @Param("nowTime") LocalTime nowTime
    );

    @Query("""
        SELECT r FROM ReservationEntity r
        WHERE r.estado = com.luistudio.reservas.model.ReservationStatus.ACTIVA
          AND r.fecha = :date
          AND r.horaInicio <= :time
          AND r.horaFin > :time
    """)
    List<ReservationEntity> findActiveAt(@Param("date") LocalDate date, @Param("time") LocalTime time);

    @Query("""
        SELECT r FROM ReservationEntity r
        WHERE r.estado = com.luistudio.reservas.model.ReservationStatus.ACTIVA
          AND (r.fecha > :today OR (r.fecha = :today AND r.horaInicio >= :nowTime))
          AND (r.fecha < :futureDate OR (r.fecha = :futureDate AND r.horaInicio <= :futureTime))
    """)
    List<ReservationEntity> findUpcomingWindow(
        @Param("today") LocalDate today,
        @Param("nowTime") LocalTime nowTime,
        @Param("futureDate") LocalDate futureDate,
        @Param("futureTime") LocalTime futureTime
    );

    @Query("""
        SELECT r FROM ReservationEntity r
        WHERE r.estado = com.luistudio.reservas.model.ReservationStatus.ACTIVA
          AND r.fecha = :date
          AND r.horaFin <= :time
          AND r.horaFin >= :minTime
    """)
    List<ReservationEntity> findEndingSoon(
        @Param("date") LocalDate date,
        @Param("time") LocalTime time,
        @Param("minTime") LocalTime minTime
    );

    @Query("""
        SELECT COUNT(r) FROM ReservationEntity r
        WHERE r.usuario = :usuario
          AND r.estado = com.luistudio.reservas.model.ReservationStatus.ACTIVA
          AND (r.fecha > :today OR (r.fecha = :today AND r.horaFin >= :nowTime))
    """)
    long countCurrentActiveForUser(
        @Param("usuario") UserEntity usuario,
        @Param("today") LocalDate today,
        @Param("nowTime") LocalTime nowTime
    );
}
