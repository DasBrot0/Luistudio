package com.luistudio.reservas.repository;

import com.luistudio.reservas.model.RoomEntity;
import com.luistudio.reservas.model.RoomScheduleEntity;
import java.util.List;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomScheduleRepository extends JpaRepository<RoomScheduleEntity, Long> {
    List<RoomScheduleEntity> findBySalaOrderByDiaSemanaAsc(RoomEntity sala);
    List<RoomScheduleEntity> findBySalaIdIn(Collection<Long> roomIds);
}
