package com.luistudio.reservas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "audit_log")
public class AuditLogEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "actor_user_id")
    private UserEntity actor;

    @Column(name = "action", nullable = false, length = 80)
    private String accion;

    @Column(name = "entity", nullable = false, length = 80)
    private String entidad;

    @Column(name = "entity_id", length = 80)
    private String entidadId;

    @Column(name = "detail", columnDefinition = "TEXT")
    private String detalle;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime creadoEn = OffsetDateTime.now();
}
