package com.luistudio.reservas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "rooms")
public class RoomEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "building_id", nullable = false)
    private PabellonEntity pabellon;

    @Column(name = "code", nullable = false, unique = true, length = 20)
    private String codigo;

    @Column(name = "name", nullable = false, length = 120)
    private String nombre;

    @Column(name = "capacity", nullable = false)
    private Integer capacidad;

    @Column(name = "campus", nullable = false, length = 120)
    private String campus;

    @Column(name = "venue", nullable = false, length = 160)
    private String venue;

    @Column(name = "location", nullable = false, length = 120)
    private String ubicacion;

    @Column(name = "min_people", nullable = false)
    private Integer minimoPersonas = 1;

    @Column(name = "min_people_required", nullable = false)
    private Boolean minimoPersonasObligatorio = false;

    @Column(name = "max_people", nullable = false)
    private Integer maximoPersonas = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private RoomState estado = RoomState.DISPONIBLE;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime creadaEn = OffsetDateTime.now();
}
