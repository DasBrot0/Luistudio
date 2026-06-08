package com.luistudio.reservas.service.email;

import com.luistudio.reservas.model.EmailOutboxEntity;
import com.luistudio.reservas.model.EmailStatus;
import com.luistudio.reservas.repository.EmailOutboxRepository;
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

    public EmailDispatchService(
        EmailOutboxRepository emailOutboxRepository,
        EmailGatewayResolver emailGatewayResolver
    ) {
        this.emailOutboxRepository = emailOutboxRepository;
        this.emailGatewayResolver = emailGatewayResolver;
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
                sent++;
            } catch (Exception ex) {
                failed++;
                email.setIntentos(email.getIntentos() + 1);
                email.setErrorDetalle(ex.getMessage());
                if (email.getIntentos() >= 3) {
                    email.setEstado(EmailStatus.ERROR);
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

    private String sanitize(String message) {
        if (message == null || message.isBlank()) {
            return "n/a";
        }
        return message.replaceAll("[\\r\\n]+", " ");
    }
}
