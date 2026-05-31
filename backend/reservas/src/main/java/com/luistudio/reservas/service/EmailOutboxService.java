package com.luistudio.reservas.service;

import com.luistudio.reservas.model.EmailOutboxEntity;
import com.luistudio.reservas.model.EmailStatus;
import com.luistudio.reservas.model.NotificationPreferenceEntity;
import com.luistudio.reservas.model.UserEntity;
import com.luistudio.reservas.repository.EmailOutboxRepository;
import com.luistudio.reservas.repository.NotificationPreferenceRepository;
import com.luistudio.reservas.service.email.gateway.EmailGateway;
import com.luistudio.reservas.service.email.gateway.EmailGatewayFactory;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EmailOutboxService {

    private final EmailOutboxRepository emailOutboxRepository;
    private final NotificationPreferenceRepository notificationPreferenceRepository;
    private final EmailGateway emailGateway;

    public EmailOutboxService(
        EmailOutboxRepository emailOutboxRepository,
        NotificationPreferenceRepository notificationPreferenceRepository,
        EmailGatewayFactory emailGatewayFactory
    ) {
        this.emailOutboxRepository = emailOutboxRepository;
        this.notificationPreferenceRepository = notificationPreferenceRepository;
        this.emailGateway = emailGatewayFactory.createGateway();
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
        emailGateway.send(email);
    }
}
