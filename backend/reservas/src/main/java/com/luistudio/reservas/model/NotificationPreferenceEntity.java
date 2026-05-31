package com.luistudio.reservas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "notification_preferences")
public class NotificationPreferenceEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private UserEntity usuario;

    @Column(name = "email_enabled", nullable = false)
    private Boolean emailHabilitado = true;

    @Column(name = "reminder_enabled", nullable = false)
    private Boolean recordatorioHabilitado = true;

    @Column(name = "booking_changes_enabled", nullable = false)
    private Boolean cambiosReservaHabilitado = true;

    @Column(name = "theme_mode", nullable = false, length = 10)
    private String themeMode = "LIGHT";

    @Column(name = "font_scale", nullable = false)
    private Double fontScale = 1.0;

    @Column(name = "login_landing_view", nullable = false, length = 30)
    private String loginLandingView = "STUDENT_MY_BOOKINGS";

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime actualizadoEn = OffsetDateTime.now();
}
