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
        List<EmailOutboxEntity> pending = emailOutboxRepository.findReadyToProcess(
            EmailStatus.PENDIENTE,
            OffsetDateTime.now(),
            PageRequest.of(0, 50)
        );
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
                    "[EMAIL_OUTBOX] Fallo envio a {} | Subject: {} | intento={} | estado={} | error={}",
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
        emailGatewayResolver.resolve().send(email);
    }
}
