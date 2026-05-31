package com.luistudio.reservas.repository;

import com.luistudio.reservas.model.CampusScheduleEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CampusScheduleRepository extends JpaRepository<CampusScheduleEntity, Long> {
    List<CampusScheduleEntity> findByCampusIgnoreCaseOrderByDiaSemanaAsc(String campus);
}
