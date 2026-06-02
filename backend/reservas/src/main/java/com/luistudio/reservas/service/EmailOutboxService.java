package com.luistudio.reservas.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.luistudio.reservas.model.EmailOutboxEntity;
import com.luistudio.reservas.model.EmailStatus;
import com.luistudio.reservas.model.NotificationPreferenceEntity;
import com.luistudio.reservas.model.UserEntity;
import com.luistudio.reservas.repository.EmailOutboxRepository;
import com.luistudio.reservas.repository.NotificationPreferenceRepository;
import com.luistudio.reservas.service.email.gateway.EmailGateway;
import com.luistudio.reservas.service.email.gateway.EmailGatewayFactory;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailOutboxService {
    private static final Logger log = LoggerFactory.getLogger(EmailOutboxService.class);
    private static final Pattern URL_PATTERN = Pattern.compile("(https?://\\S+)");
    private static final Pattern CODE_PATTERN = Pattern.compile("\\b\\d{6}\\b");

    private final EmailOutboxRepository emailOutboxRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final EmailGateway emailGateway;
    private final ObjectMapper objectMapper;

    public EmailOutboxService(
        EmailOutboxRepository emailOutboxRepository,
        NotificationPreferenceRepository notificationPreferenceRepository,
        EmailGatewayFactory emailGatewayFactory,
        ObjectMapper objectMapper
    ) {
        this.emailOutboxRepository = emailOutboxRepository;
        this.notificationPreferenceRepository = notificationPreferenceRepository;
        this.emailGateway = emailGatewayFactory.createGateway();
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void enqueue(UserEntity recipient, String subject, String body, String payload) {
        NotificationPreferenceEntity preference = notificationPreferenceRepository
            .findByUsuario(recipient)
            .orElse(null);

        if (preference != null && Boolean.FALSE.equals(preference.getEmailHabilitado())) {
            log.info(
                "[EMAIL_OUTBOX] Omitido para {} porque emailEnabled=false",
                recipient.getCorreo()
            );
            return;
        }

        EmailOutboxEntity email = new EmailOutboxEntity();
        email.setDestinatario(recipient.getCorreo());
        email.setAsunto(subject);
        email.setCuerpo(toBrandedHtml(subject, body));
        email.setPayload(parsePayload(payload));
        email.setEstado(EmailStatus.PENDIENTE);
        email.setIntentos(0);
        email.setDisponibleDesde(OffsetDateTime.now());
        emailOutboxRepository.save(email);
        log.info(
            "[EMAIL_OUTBOX] Encolado para {} | Subject: {}",
            email.getDestinatario(),
            email.getAsunto()
        );
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
    @Scheduled(fixedDelay = 60000)
    public void processPendingEmails() {
        List<EmailOutboxEntity> pending = emailOutboxRepository.findReadyToProcess(EmailStatus.PENDIENTE, OffsetDateTime.now());
        for (EmailOutboxEntity email : pending) {
            try {
                sendEmail(email);
                email.setEstado(EmailStatus.ENVIADO);
                email.setEnviadoEn(OffsetDateTime.now());
                emailOutboxRepository.save(email);
            } catch (Exception ex) {
                email.setIntentos(email.getIntentos() + 1);
                email.setErrorDetalle(ex.getMessage());
                if (email.getIntentos() >= 3) {
                    email.setEstado(EmailStatus.ERROR);
                } else {
                    email.setDisponibleDesde(OffsetDateTime.now().plusMinutes(2));
                }
                emailOutboxRepository.save(email);
                log.warn(
                    "[EMAIL_OUTBOX] Fallo envio a {} | Subject: {} | intento={} | estado={} | error={} ",
                    email.getDestinatario(),
                    email.getAsunto(),
                    email.getIntentos(),
                    email.getEstado(),
                    ex.getMessage()
                );
            }
        }
    }

    private void sendEmail(EmailOutboxEntity email) {
        emailGateway.send(email);
    }

    private String toJsonPayload(Map<String, Object> payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception ex) {
            log.warn("[EMAIL_OUTBOX] No se pudo serializar payload: {}", ex.getMessage());
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

    private String toBrandedHtml(String subject, String body) {
        if (isHtml(body)) return body;

        String safeSubject = escapeHtml(subject == null ? "Notificaci\u00f3n Luistudio" : subject);
        String rawBody = body == null ? "" : body;

        String summary = "";
        List<String> details = new ArrayList<>();
        List<String> bullets = new ArrayList<>();
        String ctaUrl = null;
        String code = null;

        for (String line : rawBody.split("\\r?\\n")) {
            String trimmed = line == null ? "" : line.trim();
            if (trimmed.isEmpty()) continue;

            if (summary.isEmpty()) summary = trimmed;

            if (trimmed.startsWith("- ")) {
                bullets.add(trimmed.substring(2).trim());
            } else if (trimmed.contains(":")) {
                details.add(trimmed);
            }

            if (ctaUrl == null) {
                Matcher urlMatcher = URL_PATTERN.matcher(trimmed);
                if (urlMatcher.find()) ctaUrl = urlMatcher.group(1);
            }

            if (code == null) {
                Matcher codeMatcher = CODE_PATTERN.matcher(trimmed);
                if (codeMatcher.find()) code = codeMatcher.group();
            }
        }

        if (summary.isEmpty()) summary = "Tienes una nueva notificaci\u00f3n en Luistudio.";

        if (code != null && !code.isBlank()) {
            summary = summary.replace(code, "").replace("  ", " ").trim();
            if (summary.endsWith(":")) {
                summary = summary.substring(0, summary.length() - 1).trim();
            }
            if (summary.isBlank()) {
                summary = "Te enviamos un c\u00f3digo de verificaci\u00f3n para continuar.";
            }
        }

        StringBuilder detailsHtml = new StringBuilder();
        if (!details.isEmpty()) {
            detailsHtml.append("<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"border-collapse:separate;border-spacing:0 8px;margin-top:10px;\">");
            for (String item : details) {
                int idx = item.indexOf(':');
                String left = idx > 0 ? item.substring(0, idx).trim() : item;
                String right = idx > 0 ? item.substring(idx + 1).trim() : "";

                if (code != null && right.equals(code)) {
                    continue;
                }

                detailsHtml.append("<tr>")
                    .append("<td style=\"width:170px;padding:8px 10px;background:#eff6ff;border:1px solid #dbeafe;border-radius:8px;color:#1e3a8a;font-weight:700;font-size:13px;vertical-align:top;\">")
                    .append(escapeHtml(left))
                    .append("</td>")
                    .append("<td style=\"padding:8px 10px;background:#f8fafc;border:1px solid #e2e8f0;border-radius:8px;color:#334155;font-size:13px;\">")
                    .append(linkify(escapeHtml(right)))
                    .append("</td>")
                    .append("</tr>");
            }
            detailsHtml.append("</table>");
        }

        StringBuilder bulletsHtml = new StringBuilder();
        if (!bullets.isEmpty()) {
            bulletsHtml.append("<ul style=\"margin:10px 0 0 18px;padding:0;color:#334155;\">");
            for (String bullet : bullets) {
                bulletsHtml.append("<li style=\"margin:4px 0;\">").append(linkify(escapeHtml(bullet))).append("</li>");
            }
            bulletsHtml.append("</ul>");
        }

        String codeHtml = "";
        if (code != null) {
            codeHtml = "<div style=\"margin-top:14px;padding:12px;border:1px dashed #93c5fd;border-radius:10px;background:#eff6ff;text-align:center;\">"
                + "<div style=\"font-size:12px;color:#1e3a8a;margin-bottom:4px;\">C\u00f3digo de verificaci\u00f3n</div>"
                + "<div style=\"font-size:28px;letter-spacing:4px;color:#1d4ed8;font-weight:800;\">"
                + escapeHtml(code)
                + "</div></div>";
        }

        String actionHtml = "";
        if (ctaUrl != null) {
            String safeUrl = escapeHtml(ctaUrl);
            actionHtml = "<div style=\"margin-top:16px;\">"
                + "<a href=\"" + safeUrl + "\" style=\"display:inline-block;background:#2563eb;color:#ffffff;text-decoration:none;font-weight:700;padding:11px 16px;border-radius:10px;\">Abrir enlace</a>"
                + "</div>";
        }

        String fallbackHtml = "";
        if (details.isEmpty() && bullets.isEmpty() && code == null && ctaUrl == null) {
            fallbackHtml = "<div style=\"margin-top:8px;\">" + linkify(escapeHtml(rawBody).replace("\n", "<br/>")) + "</div>";
        }

        return "<!DOCTYPE html>"
            + "<html lang=\"es\"><head><meta charset=\"UTF-8\"><meta name=\"viewport\" content=\"width=device-width,initial-scale=1\"></head>"
            + "<body style=\"margin:0;padding:0;background:#f1f5f9;font-family:Segoe UI,Arial,sans-serif;color:#0f172a;\">"
            + "<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"padding:24px 12px;\">"
            + "<tr><td align=\"center\">"
            + "<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" style=\"max-width:620px;background:#ffffff;border-radius:16px;overflow:hidden;border:1px solid #dbe6f3;\">"
            + "<tr><td style=\"background:linear-gradient(120deg,#1e3a8a,#2563eb);padding:22px 24px;color:#ffffff;\">"
            + "<h1 style=\"margin:0;font-size:24px;font-weight:800;\">Luistudio</h1>"
            + "<p style=\"margin:6px 0 0;font-size:15px;opacity:.95;\">Actualizaci\u00f3n de tu cuenta y reservas</p>"
            + "</td></tr>"
            + "<tr><td style=\"padding:22px 24px;\">"
            + "<h2 style=\"margin:0 0 12px;color:#1e3a8a;font-size:20px;\">" + safeSubject + "</h2>"
            + "<div style=\"background:#f8fafc;border:1px solid #e2e8f0;border-radius:12px;padding:14px;color:#334155;line-height:1.5;\">"
            + "<p style=\"margin:0;\">" + escapeHtml(summary) + "</p>"
            + detailsHtml
            + bulletsHtml
            + codeHtml
            + actionHtml
            + fallbackHtml
            + "</div>"
            + "<p style=\"margin:16px 0 0;color:#64748b;font-size:12px;\">Este correo fue generado autom\u00e1ticamente por Luistudio.</p>"
            + "</td></tr></table></td></tr></table></body></html>";
    }

    private boolean isHtml(String body) {
        if (body == null) return false;
        String normalized = body.trim().toLowerCase();
        return normalized.startsWith("<!doctype html") || normalized.startsWith("<html");
    }

    private String escapeHtml(String value) {
        return value
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#39;");
    }

    private String linkify(String text) {
        if (text == null || text.isBlank()) return "";
        Matcher matcher = URL_PATTERN.matcher(text);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String url = matcher.group(1);
            String replacement = "<a href=\"" + url + "\" style=\"color:#2563eb;text-decoration:underline;\">" + url + "</a>";
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }
}

