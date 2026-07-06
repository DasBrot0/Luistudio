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
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "bookings")
public class ReservationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private RoomEntity sala;

    @Column(name = "booking_date", nullable = false)
    private LocalDate fecha;

    @Column(name = "start_time", nullable = false)
    private LocalTime horaInicio;

    @Column(name = "end_time", nullable = false)
    private LocalTime horaFin;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReservationStatus estado = ReservationStatus.ACTIVA;

    @Column(name = "people_count", nullable = false)
    private Integer cantidadPersonas;

    @Column(name = "note", length = 255)
    private String observacion;

    @Column(name = "attendance_status", length = 20)
    private String attendanceStatus;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime creadaEn = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime actualizadaEn = OffsetDateTime.now();

    @Column(name = "updated_by")
    private Long updatedBy;
}
