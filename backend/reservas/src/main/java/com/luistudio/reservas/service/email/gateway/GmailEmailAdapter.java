package com.luistudio.reservas.service.email.gateway;

import com.luistudio.reservas.model.EmailOutboxEntity;
import com.luistudio.reservas.service.email.gateway.adaptee.GmailClientAdaptee;
import org.springframework.stereotype.Component;

@Component
public class GmailEmailAdapter implements EmailGateway {

    private final GmailClientAdaptee gmailClientAdaptee;

    public GmailEmailAdapter(GmailClientAdaptee gmailClientAdaptee) {
        this.gmailClientAdaptee = gmailClientAdaptee;
    }

    @Override
    public String provider() {
        return "gmail";
    }

    @Override
    public boolean isConfigured() {
        return gmailClientAdaptee.canSend();
    }

    @Override
    public void send(EmailOutboxEntity email) {
        gmailClientAdaptee.sendWithGmail(email);
    }
}
