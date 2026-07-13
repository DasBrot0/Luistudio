package com.luistudio.reservas.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luistudio.reservas.model.EmailOutboxEntity;
import com.luistudio.reservas.model.EmailStatus;
import com.luistudio.reservas.model.NotificationPreferenceEntity;
import com.luistudio.reservas.model.UserEntity;
import com.luistudio.reservas.repository.EmailOutboxRepository;
import com.luistudio.reservas.repository.NotificationPreferenceRepository;
import com.luistudio.reservas.service.email.EmailTemplateService;
import java.time.OffsetDateTime;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailOutboxService {
    private static final Logger log = LoggerFactory.getLogger(EmailOutboxService.class);
    private static final String BOOKING_REMINDER = "BOOKING_REMINDER";

    private final EmailOutboxRepository emailOutboxRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final ObjectMapper objectMapper;
    private final EmailTemplateService emailTemplateService;

    public EmailOutboxService(
        EmailOutboxRepository emailOutboxRepository,
        NotificationPreferenceRepository notificationPreferenceRepository,
        ObjectMapper objectMapper,
        EmailTemplateService emailTemplateService
    ) {
        this.emailOutboxRepository = emailOutboxRepository;
        this.notificationPreferenceRepository = notificationPreferenceRepository;
        this.objectMapper = objectMapper;
        this.emailTemplateService = emailTemplateService;
    }

    @Transactional
    public boolean enqueue(UserEntity recipient, String subject, String body, String payload) {
        NotificationPreferenceEntity preference = notificationPreferenceRepository
            .findByUsuario(recipient)
            .orElse(null);

        if (preference != null && Boolean.FALSE.equals(preference.getEmailHabilitado())) {
            log.info("email_outbox_skipped reason=email_disabled");
            return false;
        }

        String notificationType = resolveNotificationType(payload);
        if (preference != null && notificationType != null && !isNotificationEmailEnabled(preference, notificationType)) {
            log.info(
                "email_outbox_skipped reason=notification_email_disabled notificationType={}",
                notificationType
            );
            return false;
        }

        EmailOutboxEntity email = new EmailOutboxEntity();
        email.setDestinatario(recipient.getCorreo());
        email.setAsunto(subject);
        email.setCuerpo(toHtmlBody(subject, body));
        email.setPayload(parsePayload(payload));
        email.setEstado(EmailStatus.PENDIENTE);
        email.setIntentos(0);
        email.setDisponibleDesde(OffsetDateTime.now());
        emailOutboxRepository.save(email);
        log.info("email_outbox_enqueued emailId={} notificationType={}", email.getId(), notificationType);
        return true;
    }

    @Transactional
    public void enqueueReminderOnce(
        UserEntity recipient,
        String subject,
        String body,
        Long reservationId,
        String reminderType
    ) {
        boolean alreadyQueued = emailOutboxRepository.existsReminderByRecipientAndTypeAndReservation(
            recipient.getCorreo(),
            reminderType,
            reservationId
        );
        if (alreadyQueued) {
            return;
        }

        String payload = toJsonPayload(Map.of(
            "reservationId", reservationId,
            "reminderType", reminderType
        ));
        enqueue(recipient, subject, body, payload);
    }

    @Transactional
    public void enqueueSecurity(UserEntity recipient, String subject, String body) {
        EmailOutboxEntity email = new EmailOutboxEntity();
        email.setDestinatario(recipient.getCorreo());
        email.setAsunto(subject);
        email.setCuerpo(toHtmlBody(subject, body));
        email.setPayload(null);
        email.setEstado(EmailStatus.PENDIENTE);
        email.setIntentos(0);
        email.setDisponibleDesde(OffsetDateTime.now());
        emailOutboxRepository.save(email);
    }

    private String toHtmlBody(String subject, String body) {
        if (emailTemplateService.isHtml(body)) {
            return body;
        }
        return emailTemplateService.alert(subject, body);
    }

    private String resolveNotificationType(String payload) {
        JsonNode parsed = parsePayload(payload);
        if (parsed == null || !parsed.isObject()) return null;
        JsonNode explicitType = parsed.get("notificationType");
        if (explicitType != null && explicitType.isTextual()) return explicitType.asText();
        JsonNode reminderType = parsed.get("reminderType");
        if (reminderType != null && reminderType.isTextual()) return BOOKING_REMINDER;
        return null;
    }

    private boolean isNotificationEmailEnabled(NotificationPreferenceEntity preference, String notificationType) {
        String raw = preference.getNotificationSettings();
        if (raw == null || raw.isBlank()) return true;
        try {
            JsonNode settings = objectMapper.readTree(raw);
            JsonNode typeSettings = settings.get(notificationType);
            if (typeSettings == null || !typeSettings.isObject()) return true;
            JsonNode email = typeSettings.get("email");
            return email == null || !email.isBoolean() || email.asBoolean();
        } catch (Exception ex) {
            return true;
        }
    }

    private String toJsonPayload(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            log.warn("email_outbox_payload_serialize_failed message={}", sanitize(ex.getMessage()));
            return null;
        }
    }

    private JsonNode parsePayload(String payload) {
        if (payload == null || payload.isBlank()) {
            return null;
        }

        try {
            return objectMapper.readTree(payload);
        } catch (Exception ex) {
            return objectMapper.getNodeFactory().textNode(payload);
        }
    }

    private String sanitize(String message) {
        if (message == null || message.isBlank()) {
            return "n/a";
        }
        return message.replaceAll("[\\r\\n]+", " ");
    }
}
