package com.luistudio.reservas.service.email.gateway;

import com.luistudio.reservas.model.EmailOutboxEntity;

public interface EmailGateway {
    String provider();

    boolean isConfigured();

    void send(EmailOutboxEntity email);
}
