package com.luistudio.reservas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "email_outbox")
public class EmailOutboxEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipient", nullable = false, length = 160)
    private String destinatario;

    @Column(name = "subject", nullable = false, length = 160)
    private String asunto;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String cuerpo;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private EmailStatus estado = EmailStatus.PENDIENTE;

    @Column(name = "attempts", nullable = false)
    private Integer intentos = 0;

    @Column(name = "available_from", nullable = false)
    private OffsetDateTime disponibleDesde = OffsetDateTime.now();

    @Column(name = "sent_at")
    private OffsetDateTime enviadoEn;

    @Column(name = "error_detail", columnDefinition = "TEXT")
    private String errorDetalle;
}
