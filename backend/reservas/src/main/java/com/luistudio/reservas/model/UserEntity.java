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
@Table(name = "users")
public class UserEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private RoleEntity rol;

    @Column(name = "code", nullable = false, unique = true, length = 20)
    private String codigo;

    @Column(name = "first_name", nullable = false, length = 120)
    private String nombres;

    @Column(name = "last_name", nullable = false, length = 120)
    private String apellidos;

    @Column(name = "email", nullable = false, unique = true, length = 160)
    private String correo;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private UserStatus estado = UserStatus.HABILITADO;

    @Column(name = "locked_until")
    private OffsetDateTime lockedUntil;

    @Column(name = "has_2fa", nullable = false)
    private Boolean has2fa = false;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime creadoEn = OffsetDateTime.now();

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime actualizadoEn = OffsetDateTime.now();
}
