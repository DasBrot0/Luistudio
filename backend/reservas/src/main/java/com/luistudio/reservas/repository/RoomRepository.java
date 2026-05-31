package com.luistudio.reservas.repository;

import com.luistudio.reservas.model.PabellonEntity;
import com.luistudio.reservas.model.RoomEntity;
import com.luistudio.reservas.model.RoomState;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<RoomEntity, Long> {
    Optional<RoomEntity> findByCodigo(String codigo);

    List<RoomEntity> findByEstadoNot(RoomState estado);

    List<RoomEntity> findByUbicacionIgnoreCaseAndEstadoNot(String ubicacion, RoomState estado);

    List<RoomEntity> findByCampusIgnoreCaseAndEstadoNot(String campus, RoomState estado);

    List<RoomEntity> findByPabellonAndEstadoNot(PabellonEntity pabellon, RoomState estado);
}
