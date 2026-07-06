package com.luistudio.reservas.service;

import com.luistudio.reservas.exception.BusinessException;
import com.luistudio.reservas.model.SensitiveChangeTokenEntity;
import com.luistudio.reservas.model.UserEntity;
import com.luistudio.reservas.repository.SensitiveChangeTokenRepository;
import com.luistudio.reservas.repository.UserRepository;
import com.luistudio.reservas.security.SecretHashService;
import com.luistudio.reservas.service.email.EmailTemplateService;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SensitiveChangeService {

    private static final int TOKEN_EXPIRY_MINUTES = 30;

    private final SensitiveChangeTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final EmailOutboxService emailOutboxService;
    private final EmailTemplateService emailTemplateService;
    private final SecretHashService secretHashService;
    private final SessionService sessionService;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;
    private final String confirmChangeUrl;

    public SensitiveChangeService(
        SensitiveChangeTokenRepository tokenRepository,
        UserRepository userRepository,
        EmailOutboxService emailOutboxService,
        EmailTemplateService emailTemplateService,
        SecretHashService secretHashService,
        SessionService sessionService,
        AuditService auditService,
        PasswordEncoder passwordEncoder,
        @Value("${app.frontend.confirm-change-url}") String confirmChangeUrl
    ) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.emailOutboxService = emailOutboxService;
        this.emailTemplateService = emailTemplateService;
        this.secretHashService = secretHashService;
        this.sessionService = sessionService;
        this.auditService = auditService;
        this.passwordEncoder = passwordEncoder;
        this.confirmChangeUrl = confirmChangeUrl;
    }

    @Transactional
    public void requestChange(Long userId, String actionType, String payload) {
        UserEntity user = findUser(userId);
        validateActionType(actionType);

        String rawToken = UUID.randomUUID().toString();
        String hashedToken = secretHashService.hash(rawToken);

        SensitiveChangeTokenEntity entity = new SensitiveChangeTokenEntity();
        entity.setUsuario(user);
        entity.setActionType(actionType);
        entity.setToken(hashedToken);
        entity.setPayload(payload);
        entity.setExpiresAt(OffsetDateTime.now().plusMinutes(TOKEN_EXPIRY_MINUTES));
        entity.setUsed(false);
        tokenRepository.save(entity);

        String separator = confirmChangeUrl.contains("?") ? "&" : "?";
        String ctaUrl = confirmChangeUrl + separator
            + "token=" + URLEncoder.encode(rawToken, StandardCharsets.UTF_8)
            + "&action=" + URLEncoder.encode(actionType, StandardCharsets.UTF_8);

        String actionDescription = describeAction(actionType);
        String subject = "Confirma tu " + actionDescription;
        String summary = "Recibimos una solicitud para realizar un " + actionDescription
            + " en tu cuenta. Haz clic en el botón para confirmarla. "
            + "Este enlace expira en " + TOKEN_EXPIRY_MINUTES + " minutos. "
            + "Si no solicitaste este cambio, ignora este correo.";

        String body = emailTemplateService.callToAction(subject, summary, ctaUrl);
        emailOutboxService.enqueueSecurity(user, subject, body);
    }

    @Transactional
    public void confirmChange(String rawToken) {
        String hashedToken = secretHashService.hash(rawToken);
        SensitiveChangeTokenEntity entity = tokenRepository.findByTokenAndUsedFalse(hashedToken)
            .orElseThrow(() -> new BusinessException(HttpStatus.BAD_REQUEST, "Token inválido o ya usado"));

        if (entity.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Token expirado");
        }

        UserEntity user = entity.getUsuario();
        entity.setUsed(true);
        tokenRepository.save(entity);

        dispatchAction(user, entity.getActionType(), entity.getPayload());
        auditService.record(user, "SENSITIVE_CHANGE_CONFIRMED", "sensitive_change_token",
            String.valueOf(entity.getId()), "actionType=" + entity.getActionType());
    }

    private void dispatchAction(UserEntity user, String actionType, String payload) {
        switch (actionType) {
            case "CHANGE_PASSWORD" -> {
                if (payload == null || payload.isBlank()) {
                    throw new BusinessException(HttpStatus.BAD_REQUEST, "Se requiere la nueva contraseña en el payload");
                }
                user.setPasswordHash(passwordEncoder.encode(payload));
                userRepository.save(user);
            }
            case "DISABLE_2FA" -> {
                user.setHas2fa(false);
                userRepository.save(user);
            }
            case "REVOKE_ALL_SESSIONS" -> sessionService.revokeAllSessions(user.getId());
            default -> throw new BusinessException(HttpStatus.BAD_REQUEST, "Tipo de acción desconocido: " + actionType);
        }
    }

    private void validateActionType(String actionType) {
        if (!actionType.equals("CHANGE_PASSWORD") && !actionType.equals("DISABLE_2FA") && !actionType.equals("REVOKE_ALL_SESSIONS")) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "Tipo de acción no válido");
        }
    }

    private String describeAction(String actionType) {
        return switch (actionType) {
            case "CHANGE_PASSWORD" -> "cambio de contraseña";
            case "DISABLE_2FA" -> "desactivación de 2FA";
            case "REVOKE_ALL_SESSIONS" -> "cierre de todas las sesiones";
            default -> actionType;
        };
    }

    private UserEntity findUser(Long userId) {
        return userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "Usuario no encontrado"));
    }
}
