package com.luistudio.reservas.repository;

import com.luistudio.reservas.model.ReservationEntity;
import com.luistudio.reservas.model.ReservationStatus;
import com.luistudio.reservas.model.RoomEntity;
import com.luistudio.reservas.model.UserEntity;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReservationRepository extends JpaRepository<ReservationEntity, Long>, JpaSpecificationExecutor<ReservationEntity> {

    @Override
    @EntityGraph(attributePaths = {"usuario", "sala", "sala.pabellon", "sala.pabellon.campus"})
    Page<ReservationEntity> findAll(Specification<ReservationEntity> specification, Pageable pageable);

    @EntityGraph(attributePaths = {"usuario", "usuario.rol", "sala"})
    @Query("""
        SELECT r FROM ReservationEntity r
        WHERE r.fecha BETWEEN :fromDate AND :toDate
        ORDER BY r.fecha ASC, r.horaInicio ASC
    """)
    List<ReservationEntity> findForDashboard(
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate
    );

    @EntityGraph(attributePaths = {"usuario", "sala"})
    Page<ReservationEntity> findByUsuarioOrderByFechaDescHoraInicioDesc(UserEntity usuario, Pageable pageable);

    Optional<ReservationEntity> findTopByUsuarioAndSalaAndFechaAndHoraInicioAndHoraFinOrderByIdDesc(
        UserEntity usuario,
        RoomEntity sala,
        LocalDate fecha,
        LocalTime horaInicio,
        LocalTime horaFin
    );

    @EntityGraph(attributePaths = {"usuario", "sala"})
    @Query("""
        SELECT r FROM ReservationEntity r
        WHERE r.sala = :room
          AND r.estado = com.luistudio.reservas.model.ReservationStatus.ACTIVA
          AND r.fecha BETWEEN :fromDate AND :toDate
        ORDER BY r.fecha ASC, r.horaInicio ASC
    """)
    List<ReservationEntity> findActiveByRoomAndDateRange(
        @Param("room") RoomEntity room,
        @Param("fromDate") LocalDate fromDate,
        @Param("toDate") LocalDate toDate
    );

    long countByUsuarioAndEstado(UserEntity usuario, ReservationStatus estado);

    @EntityGraph(attributePaths = {"usuario", "sala"})
    Page<ReservationEntity> findByEstadoOrderByFechaDescHoraInicioDesc(ReservationStatus estado, Pageable pageable);

    @EntityGraph(attributePaths = {"usuario", "sala"})
    Page<ReservationEntity> findByFechaOrderByHoraInicioDesc(LocalDate fecha, Pageable pageable);

    @EntityGraph(attributePaths = {"usuario", "sala"})
    Page<ReservationEntity> findAllByOrderByFechaDescHoraInicioDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"usuario", "sala"})
    Page<ReservationEntity> findByEstadoAndFechaOrderByHoraInicioDesc(
        ReservationStatus estado,
        LocalDate fecha,
        Pageable pageable
    );

    @Query("""
        SELECT COUNT(r) FROM ReservationEntity r
        WHERE r.sala = :room
          AND r.fecha = :date
          AND r.estado = com.luistudio.reservas.model.ReservationStatus.ACTIVA
          AND r.horaInicio < :endTime
          AND r.horaFin > :startTime
          AND (:excludeId IS NULL OR r.id <> :excludeId)
    """)
    long countOverlapping(
        @Param("room") RoomEntity room,
        @Param("date") LocalDate date,
        @Param("startTime") LocalTime startTime,
        @Param("endTime") LocalTime endTime,
        @Param("excludeId") Long excludeId
    );

    @Query("""
        SELECT r.numeroUnidad FROM ReservationEntity r
        WHERE r.sala = :room
          AND r.fecha = :date
          AND r.estado = com.luistudio.reservas.model.ReservationStatus.ACTIVA
          AND r.horaInicio < :endTime
          AND r.horaFin > :startTime
          AND (:excludeId IS NULL OR r.id <> :excludeId)
          AND r.numeroUnidad IS NOT NULL
    """)
    List<Integer> findOccupiedUnitNumbers(
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
        SELECT COUNT(r) > 0 FROM ReservationEntity r
        WHERE LOWER(r.sala.pabellon.campus.nombre) = LOWER(:campus)
          AND r.estado = com.luistudio.reservas.model.ReservationStatus.ACTIVA
          AND (r.fecha > :today OR (r.fecha = :today AND r.horaFin > :nowTime))
    """)
    boolean existsFutureActiveReservationsByCampus(
        @Param("campus") String campus,
        @Param("today") LocalDate today,
        @Param("nowTime") LocalTime nowTime
    );

    @EntityGraph(attributePaths = {"usuario", "sala"})
    @Query("""
        SELECT r FROM ReservationEntity r
        WHERE r.estado = com.luistudio.reservas.model.ReservationStatus.ACTIVA
          AND r.fecha = :date
          AND r.horaInicio <= :time
          AND r.horaFin > :time
    """)
    List<ReservationEntity> findActiveAt(@Param("date") LocalDate date, @Param("time") LocalTime time);

    @EntityGraph(attributePaths = {"usuario", "sala"})
    @Query("""
        SELECT r FROM ReservationEntity r
        WHERE r.estado = com.luistudio.reservas.model.ReservationStatus.ACTIVA
          AND (r.fecha > :today OR (r.fecha = :today AND r.horaFin > :nowTime))
          AND (r.fecha < :futureDate OR (r.fecha = :futureDate AND r.horaInicio <= :futureTime))
    """)
    List<ReservationEntity> findUpcomingWindow(
        @Param("today") LocalDate today,
        @Param("nowTime") LocalTime nowTime,
        @Param("futureDate") LocalDate futureDate,
        @Param("futureTime") LocalTime futureTime
    );

    @EntityGraph(attributePaths = {"usuario", "sala"})
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

    @EntityGraph(attributePaths = {"usuario", "sala"})
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT r FROM ReservationEntity r
        WHERE r.estado = com.luistudio.reservas.model.ReservationStatus.ACTIVA
          AND (r.fecha < :cutoffDate OR (r.fecha = :cutoffDate AND r.horaInicio <= :cutoffTime))
          AND r.attendanceStatus IS NULL
    """)
    List<ReservationEntity> findActiveBookingsMissedBefore(
        @Param("cutoffDate") LocalDate cutoffDate,
        @Param("cutoffTime") LocalTime cutoffTime
    );
}
