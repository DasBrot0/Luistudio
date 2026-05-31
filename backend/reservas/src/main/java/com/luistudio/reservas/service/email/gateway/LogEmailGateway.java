package com.luistudio.reservas.service.email.gateway;

import com.luistudio.reservas.model.EmailOutboxEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LogEmailGateway implements EmailGateway {

    @Override
    public void send(EmailOutboxEntity email) {
        log.info("[OUTBOX] To: {} | Subject: {}", email.getDestinatario(), email.getAsunto());
    }
}
