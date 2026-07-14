package com.luistudio.reservas.model;

import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
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
import java.util.LinkedHashSet;
import java.util.Set;
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

    @Column(name = "location", nullable = false, length = 120)
    private String ubicacion;

    @Column(name = "min_people", nullable = false)
    private Integer minimoPersonas = 1;

    @Column(name = "min_people_required", nullable = false)
    private Boolean minimoPersonasObligatorio = false;

    @Column(name = "max_people", nullable = false)
    private Integer maximoPersonas = 1;

    @Column(name = "inventory_count", nullable = false)
    private Integer cantidadUnidades = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "noise_level", nullable = false, length = 10)
    private RoomNoiseLevel nivelRuido = RoomNoiseLevel.MEDIO;

    @Column(name = "supports_concentration", nullable = false)
    private Boolean permiteConcentracion = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", nullable = false, length = 30)
    private RoomType tipo = RoomType.GENERAL;

    @ElementCollection
    @CollectionTable(name = "room_equipment", joinColumns = @JoinColumn(name = "room_id"))
    @Column(name = "equipment", nullable = false, length = 50)
    private Set<String> equipamiento = new LinkedHashSet<>();

    @Column(name = "description", length = 500)
    private String descripcion;

    @ElementCollection
    @CollectionTable(name = "room_allowed_activities", joinColumns = @JoinColumn(name = "room_id"))
    @Column(name = "activity", nullable = false, length = 60)
    private Set<String> actividadesPermitidas = new LinkedHashSet<>();

    @ElementCollection
    @CollectionTable(name = "room_nearby_services", joinColumns = @JoinColumn(name = "room_id"))
    @Column(name = "service", nullable = false, length = 60)
    private Set<String> serviciosCercanos = new LinkedHashSet<>();

    @ElementCollection
    @CollectionTable(name = "room_accessibility_features", joinColumns = @JoinColumn(name = "room_id"))
    @Column(name = "feature", nullable = false, length = 60)
    private Set<String> caracteristicasAccesibilidad = new LinkedHashSet<>();

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private RoomState estado = RoomState.DISPONIBLE;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime creadaEn = OffsetDateTime.now();

    /** Valores derivados para conservar el contrato de lectura sin duplicar catálogo. */
    public String getCampus() {
        return pabellon == null || pabellon.getCampus() == null ? null : pabellon.getCampus().getNombre();
    }

    public String getVenue() {
        return pabellon == null ? null : pabellon.getNombre();
    }

}
