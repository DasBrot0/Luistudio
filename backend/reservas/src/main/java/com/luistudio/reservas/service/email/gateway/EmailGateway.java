package com.luistudio.reservas.service.email.gateway;

import com.luistudio.reservas.model.EmailOutboxEntity;

public interface EmailGateway {
    void send(EmailOutboxEntity email);
}
