package com.luistudio.reservas.service.email.gateway;

import com.luistudio.reservas.model.EmailOutboxEntity;
import com.luistudio.reservas.service.email.gateway.adaptee.ResendClientAdaptee;
import org.springframework.stereotype.Component;

@Component
public class ResendEmailAdapter implements EmailGateway {

    private final ResendClientAdaptee resendClientAdaptee;

    public ResendEmailAdapter(ResendClientAdaptee resendClientAdaptee) {
        this.resendClientAdaptee = resendClientAdaptee;
    }

    @Override
    public String provider() {
        return "resend";
    }

    @Override
    public boolean isConfigured() {
        return resendClientAdaptee.canSend();
    }

    @Override
    public void send(EmailOutboxEntity email) {
        resendClientAdaptee.sendWithResend(email);
    }
}
