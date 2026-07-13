package com.luistudio.reservas.service.email;

import com.luistudio.reservas.model.EmailOutboxEntity;
import com.luistudio.reservas.model.EmailStatus;
import com.luistudio.reservas.repository.EmailOutboxRepository;
import com.luistudio.reservas.repository.RoomAvailabilitySubscriptionRepository;
import com.luistudio.reservas.service.email.gateway.EmailGatewayResolver;
import java.time.OffsetDateTime;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class EmailDispatchService {

    private final EmailOutboxRepository emailOutboxRepository;
    private final EmailGatewayResolver emailGatewayResolver;
    private final RoomAvailabilitySubscriptionRepository subscriptionRepository;

    public EmailDispatchService(
        EmailOutboxRepository emailOutboxRepository,
        EmailGatewayResolver emailGatewayResolver,
        RoomAvailabilitySubscriptionRepository subscriptionRepository
    ) {
        this.emailOutboxRepository = emailOutboxRepository;
        this.emailGatewayResolver = emailGatewayResolver;
        this.subscriptionRepository = subscriptionRepository;
    }

    @Transactional
    public void processPendingEmails() {
        long startedAt = System.currentTimeMillis();
        List<EmailOutboxEntity> pending = emailOutboxRepository.findReadyToProcess(
            EmailStatus.PENDIENTE,
            OffsetDateTime.now(),
            PageRequest.of(0, 50)
        );
        int sent = 0;
        int failed = 0;
        for (EmailOutboxEntity email : pending) {
            try {
                sendEmail(email);
                email.setEstado(EmailStatus.ENVIADO);
                email.setEnviadoEn(OffsetDateTime.now());
                emailOutboxRepository.save(email);
                markAvailabilitySubscriptionNotified(email);
                sent++;
            } catch (Exception ex) {
                failed++;
                email.setIntentos(email.getIntentos() + 1);
                email.setErrorDetalle(ex.getMessage());
                if (email.getIntentos() >= 3) {
                    email.setEstado(EmailStatus.ERROR);
                    restoreAvailabilitySubscription(email);
                } else {
                    email.setDisponibleDesde(OffsetDateTime.now().plusMinutes(2));
                }
                emailOutboxRepository.save(email);
                log.warn(
                    "email_dispatch_failed emailId={} attempt={} status={} error={}",
                    email.getId(),
                    email.getIntentos(),
                    email.getEstado(),
                    sanitize(ex.getMessage())
                );
            }
        }
        log.info(
            "email_dispatch_cycle processed={} sent={} failed={} durationMs={}",
            pending.size(),
            sent,
            failed,
            System.currentTimeMillis() - startedAt
        );
    }

    private void sendEmail(EmailOutboxEntity email) {
        emailGatewayResolver.resolve().send(email);
    }

    private void markAvailabilitySubscriptionNotified(EmailOutboxEntity email) {
        if (email.getPayload() == null || !email.getPayload().isObject()) return;
        var type = email.getPayload().get("notificationType");
        var subscriptionId = email.getPayload().get("subscriptionId");
        if (type == null || !"ROOM_AVAILABLE".equals(type.asText()) || subscriptionId == null || !subscriptionId.canConvertToLong()) return;
        subscriptionRepository.findById(subscriptionId.asLong()).ifPresent(subscription -> {
            if (!"ACTIVA".equals(subscription.getStatus())) return;
            subscription.setStatus("NOTIFICADA");
            subscription.setNotifiedAt(OffsetDateTime.now());
            subscriptionRepository.save(subscription);
        });
    }

    private void restoreAvailabilitySubscription(EmailOutboxEntity email) {
        Long subscriptionId = availabilitySubscriptionId(email);
        if (subscriptionId == null) return;
        subscriptionRepository.findById(subscriptionId).ifPresent(subscription -> {
            if (!"EN_COLA".equals(subscription.getStatus())) return;
            subscription.setStatus("ACTIVA");
            subscriptionRepository.save(subscription);
        });
    }

    private Long availabilitySubscriptionId(EmailOutboxEntity email) {
        if (email.getPayload() == null || !email.getPayload().isObject()) return null;
        var type = email.getPayload().get("notificationType");
        var subscriptionId = email.getPayload().get("subscriptionId");
        if (type == null || !"ROOM_AVAILABLE".equals(type.asText()) || subscriptionId == null || !subscriptionId.canConvertToLong()) return null;
        return subscriptionId.asLong();
    }

    private String sanitize(String message) {
        if (message == null || message.isBlank()) {
            return "n/a";
        }
        return message.replaceAll("[\\r\\n]+", " ");
    }
}
