package com.luistudio.reservas.service.email.gateway;

import com.luistudio.reservas.model.EmailOutboxEntity;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class LogEmailGateway implements EmailGateway {

    @Override
    public String provider() {
        return "log";
    }

    @Override
    public boolean isConfigured() {
        return true;
    }

    @Override
    public void send(EmailOutboxEntity email) {
        log.info("email_gateway_log_send emailId={}", email.getId());
    }
}
