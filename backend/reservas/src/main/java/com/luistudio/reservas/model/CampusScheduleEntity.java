package com.luistudio.reservas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "campus_schedules")
public class CampusScheduleEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "campus_id", nullable = false)
    private CampusEntity campus;

    @Column(name = "day_of_week", nullable = false)
    private Integer diaSemana;

    @Column(name = "open_time")
    private LocalTime horaApertura;

    @Column(name = "close_time")
    private LocalTime horaCierre;

    @Column(name = "is_closed", nullable = false)
    private Boolean cerrado = false;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt = OffsetDateTime.now();
}
