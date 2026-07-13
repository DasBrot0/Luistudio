package com.luistudio.reservas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "buildings")
public class PabellonEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 20)
    private String codigo;

    @Column(name = "name", nullable = false, length = 120)
    private String nombre;

    @ManyToOne
    @JoinColumn(name = "campus_id", nullable = false)
    private CampusEntity campus;

    @Column(name = "latitude", precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 10, scale = 7)
    private BigDecimal longitude;

    // La restricción NOT NULL pertenece a la migración: Hibernate ddl-auto no puede
    // imponerla de forma segura sobre instalaciones que ya contienen edificios.
    @Column(name = "map_enabled")
    private Boolean mapEnabled = true;

    @Column(name = "map_order")
    private Integer mapOrder = 0;
}
