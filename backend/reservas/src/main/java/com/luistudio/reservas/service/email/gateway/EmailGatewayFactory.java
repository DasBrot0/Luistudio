package com.luistudio.reservas.service.email.gateway;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class EmailGatewayFactory {

    private final String emailProvider;
    private final String resendApiKey;
    private final LogEmailGateway logEmailGateway;
    private final ResendEmailGateway resendEmailGateway;

    public EmailGatewayFactory(
        @Value("${app.email.provider:log}") String emailProvider,
        @Value("${app.email.resend.api-key:}") String resendApiKey,
        LogEmailGateway logEmailGateway,
        ResendEmailGateway resendEmailGateway
    ) {
        this.emailProvider = emailProvider;
        this.resendApiKey = resendApiKey;
        this.logEmailGateway = logEmailGateway;
        this.resendEmailGateway = resendEmailGateway;
    }

    public EmailGateway createGateway() {
        if ("resend".equalsIgnoreCase(emailProvider) && StringUtils.hasText(resendApiKey)) {
            return resendEmailGateway;
        }

        if ("resend".equalsIgnoreCase(emailProvider) && !StringUtils.hasText(resendApiKey)) {
            log.warn("[OUTBOX] EMAIL_PROVIDER=resend pero RESEND_API_KEY no esta configurada. Usando fallback a log.");
        }

        return logEmailGateway;
    }
}
