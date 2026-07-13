package com.luistudio.reservas.service;

import com.luistudio.reservas.dto.session.SessionListResponse;
import com.luistudio.reservas.dto.session.SessionResponse;
import com.luistudio.reservas.exception.BusinessException;
import com.luistudio.reservas.model.LoginSessionEntity;
import com.luistudio.reservas.model.UserEntity;
import com.luistudio.reservas.repository.LoginSessionRepository;
import com.luistudio.reservas.service.email.EmailTemplateService;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SessionService {

    private static final DateTimeFormatter DISPLAY_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final LoginSessionRepository loginSessionRepository;
    private final UserService userService;
    private final AuditService auditService;
    private final EmailOutboxService emailOutboxService;
    private final EmailTemplateService emailTemplateService;

    public SessionService(
        LoginSessionRepository loginSessionRepository,
        UserService userService,
        AuditService auditService,
        EmailOutboxService emailOutboxService,
        EmailTemplateService emailTemplateService
    ) {
        this.loginSessionRepository = loginSessionRepository;
        this.userService = userService;
        this.auditService = auditService;
        this.emailOutboxService = emailOutboxService;
        this.emailTemplateService = emailTemplateService;
    }

    @Transactional
    public LoginSessionEntity createSession(UserEntity user, String jti, String ip, String userAgent) {
        String deviceLabel = deriveDeviceLabel(userAgent);
        boolean hasPreviousAccess = loginSessionRepository.existsByUsuario(user);
        boolean newIp = ip != null && !ip.isBlank()
            && !loginSessionRepository.existsByUsuarioAndIp(user, ip);
        boolean newDevice = userAgent != null && !userAgent.isBlank()
            && !loginSessionRepository.existsByUsuarioAndDeviceLabel(user, deviceLabel);
        boolean isUnusual = hasPreviousAccess && (newIp || newDevice);

        LoginSessionEntity session = new LoginSessionEntity();
        session.setUsuario(user);
        session.setJti(jti);
        session.setIp(ip);
        session.setUserAgent(userAgent);
        session.setDeviceLabel(deviceLabel);
        session.setCreatedAt(OffsetDateTime.now());
        session.setLastSeenAt(OffsetDateTime.now());
        session.setCurrent(true);
        LoginSessionEntity saved = loginSessionRepository.save(session);

        if (isUnusual) {
            String when = OffsetDateTime.now().format(DISPLAY_FMT);
            emailOutboxService.enqueueSecurity(
                user,
                "Acceso inusual detectado",
                emailTemplateService.accessAlert(ip, userAgent, when)
            );
            auditService.record(user, "LOGIN_UNUSUAL_ACCESS", "login_session", saved.getJti(),
                "ip=" + ip + ";ua=" + (userAgent.length() > 80 ? userAgent.substring(0, 80) : userAgent));
        }

        auditService.record(user, "LOGIN_SUCCESS", "login_session", saved.getJti(),
            "ip=" + ip + ";device=" + saved.getDeviceLabel());

        return saved;
    }

    @Transactional(readOnly = true)
    public SessionListResponse getMySessions(Long userId, String currentJti) {
        UserEntity user = userService.getById(userId);
        List<SessionResponse> responses = loginSessionRepository
            .findByUsuarioAndRevokedAtIsNullOrderByCreatedAtDesc(user)
            .stream()
            .map(s -> toResponse(s, currentJti))
            .toList();
        return new SessionListResponse(responses);
    }

    @Transactional
    public void revokeSession(Long userId, Long sessionId) {
        UserEntity user = userService.getById(userId);
        LoginSessionEntity session = loginSessionRepository.findById(sessionId)
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Sesión no encontrada"));
        if (!session.getUsuario().getId().equals(userId)) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "No puedes revocar una sesión de otro usuario");
        }
        session.setRevokedAt(OffsetDateTime.now());
        session.setCurrent(false);
        loginSessionRepository.save(session);
        auditService.record(user, "LOGOUT_REMOTE", "login_session", String.valueOf(sessionId), "session_id=" + sessionId);
    }

    @Transactional
    public void revokeCurrentSession(UserEntity user, String jti) {
        if (jti == null) return;
        loginSessionRepository.findByJti(jti).ifPresent(s -> {
            s.setRevokedAt(OffsetDateTime.now());
            s.setCurrent(false);
            loginSessionRepository.save(s);
        });
        auditService.record(user, "LOGOUT_CURRENT", "login_session", jti, null);
    }

    @Transactional
    public void revokeAllSessions(Long userId) {
        UserEntity user = userService.getById(userId);
        loginSessionRepository.revokeAllByUsuario(user, OffsetDateTime.now());
        auditService.record(user, "LOGOUT_ALL", "login_session", null, "userId=" + userId);
    }

    private SessionResponse toResponse(LoginSessionEntity s, String currentJti) {
        boolean isCurrent = s.getJti().equals(currentJti);
        return new SessionResponse(
            s.getId(),
            s.getIp(),
            s.getUserAgent(),
            s.getDeviceLabel(),
            s.getCreatedAt(),
            s.getLastSeenAt(),
            isCurrent
        );
    }

    private String deriveDeviceLabel(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) return "Dispositivo desconocido";
        String ua = userAgent.toLowerCase();
        if (ua.contains("mobile") || ua.contains("android")) return "Móvil";
        if (ua.contains("tablet") || ua.contains("ipad")) return "Tablet";
        if (ua.contains("windows")) return "Windows";
        if (ua.contains("macintosh") || ua.contains("mac os")) return "Mac";
        if (ua.contains("linux")) return "Linux";
        return "Navegador web";
    }
}
