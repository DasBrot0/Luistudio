package com.luistudio.reservas.repository;

import com.luistudio.reservas.model.AttendanceRecordEntity;
import com.luistudio.reservas.model.ReservationEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecordEntity, Long> {
    Optional<AttendanceRecordEntity> findByReserva(ReservationEntity reserva);
}
