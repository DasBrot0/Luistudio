package com.luistudio.reservas.service;

import com.luistudio.reservas.model.EmailOutboxEntity;
import com.luistudio.reservas.model.EmailStatus;
import com.luistudio.reservas.model.NotificationPreferenceEntity;
import com.luistudio.reservas.model.UserEntity;
import com.luistudio.reservas.repository.EmailOutboxRepository;
import com.luistudio.reservas.repository.NotificationPreferenceRepository;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

@Slf4j
@Service
public class EmailOutboxService {

    private final EmailOutboxRepository emailOutboxRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final RestClient restClient;
    private final String emailProvider;
    private final String emailFrom;
    private final String resendApiKey;

    public EmailOutboxService(
        EmailOutboxRepository emailOutboxRepository,
        NotificationPreferenceRepository notificationPreferenceRepository,
        RestClient.Builder restClientBuilder,
        @Value("${app.email.provider:log}") String emailProvider,
        @Value("${app.email.from:Luistudio <onboarding@resend.dev>}") String emailFrom,
        @Value("${app.email.resend.api-key:}") String resendApiKey
    ) {
        this.emailOutboxRepository = emailOutboxRepository;
        this.notificationPreferenceRepository = notificationPreferenceRepository;
        this.restClient = restClientBuilder.baseUrl("https://api.resend.com").build();
        this.emailProvider = emailProvider;
        this.emailFrom = emailFrom;
        this.resendApiKey = resendApiKey;
    }

    @Transactional
    public void enqueue(UserEntity recipient, String subject, String body, String payload) {
        NotificationPreferenceEntity preference = notificationPreferenceRepository
            .findByUsuario(recipient)
            .orElse(null);

        if (preference != null && Boolean.FALSE.equals(preference.getEmailHabilitado())) {
            return;
        }

        EmailOutboxEntity email = new EmailOutboxEntity();
        email.setDestinatario(recipient.getCorreo());
        email.setAsunto(subject);
        email.setCuerpo(body);
        email.setPayload(payload);
        email.setEstado(EmailStatus.PENDIENTE);
        email.setIntentos(0);
        email.setDisponibleDesde(OffsetDateTime.now());
        emailOutboxRepository.save(email);
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
            }
        }
    }

    private void sendEmail(EmailOutboxEntity email) {
        if (shouldUseResend()) {
            sendWithResend(email);
            return;
        }
        if ("resend".equalsIgnoreCase(emailProvider) && !StringUtils.hasText(resendApiKey)) {
            log.warn("[OUTBOX] EMAIL_PROVIDER=resend pero RESEND_API_KEY no esta configurada. Usando fallback a log.");
        }
        log.info("[OUTBOX] To: {} | Subject: {}", email.getDestinatario(), email.getAsunto());
    }

    private boolean shouldUseResend() {
        return "resend".equalsIgnoreCase(emailProvider) && StringUtils.hasText(resendApiKey);
    }

    private void sendWithResend(EmailOutboxEntity email) {
        Map<String, Object> payload = Map.of(
            "from", emailFrom,
            "to", List.of(email.getDestinatario()),
            "subject", email.getAsunto(),
            "text", email.getCuerpo()
        );

        String response = restClient.post()
            .uri("/emails")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + resendApiKey)
            .contentType(MediaType.APPLICATION_JSON)
            .body(payload)
            .retrieve()
            .body(String.class);

        log.info("[RESEND] Email enviado a {} | Subject: {} | Response: {}", email.getDestinatario(), email.getAsunto(), response);
    }
}
