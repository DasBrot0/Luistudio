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
    private final String gmailClientId;
    private final String gmailClientSecret;
    private final String gmailRefreshToken;
    private final LogEmailGateway logEmailGateway;
    private final ResendEmailGateway resendEmailGateway;
    private final GmailEmailGateway gmailEmailGateway;

    public EmailGatewayFactory(
        @Value("${app.email.provider:log}") String emailProvider,
        @Value("${app.email.resend.api-key:}") String resendApiKey,
        @Value("${app.email.gmail.client-id:}") String gmailClientId,
        @Value("${app.email.gmail.client-secret:}") String gmailClientSecret,
        @Value("${app.email.gmail.refresh-token:}") String gmailRefreshToken,
        LogEmailGateway logEmailGateway,
        ResendEmailGateway resendEmailGateway,
        GmailEmailGateway gmailEmailGateway
    ) {
        this.emailProvider = emailProvider;
        this.resendApiKey = resendApiKey;
        this.gmailClientId = gmailClientId;
        this.gmailClientSecret = gmailClientSecret;
        this.gmailRefreshToken = gmailRefreshToken;
        this.logEmailGateway = logEmailGateway;
        this.resendEmailGateway = resendEmailGateway;
        this.gmailEmailGateway = gmailEmailGateway;
    }

    public EmailGateway createGateway() {
        if ("resend".equalsIgnoreCase(emailProvider) && StringUtils.hasText(resendApiKey)) {
            return resendEmailGateway;
        }

        if ("gmail".equalsIgnoreCase(emailProvider)
            && StringUtils.hasText(gmailClientId)
            && StringUtils.hasText(gmailClientSecret)
            && StringUtils.hasText(gmailRefreshToken)) {
            return gmailEmailGateway;
        }

        if ("resend".equalsIgnoreCase(emailProvider) && !StringUtils.hasText(resendApiKey)) {
            log.warn("[OUTBOX] EMAIL_PROVIDER=resend pero RESEND_API_KEY no está configurada. Usando fallback a log.");
        }

        if ("gmail".equalsIgnoreCase(emailProvider)
            && (!StringUtils.hasText(gmailClientId)
                || !StringUtils.hasText(gmailClientSecret)
                || !StringUtils.hasText(gmailRefreshToken))) {
            log.warn(
                "[OUTBOX] EMAIL_PROVIDER=gmail pero faltan GMAIL_CLIENT_ID/GMAIL_CLIENT_SECRET/GMAIL_REFRESH_TOKEN. Usando fallback a log."
            );
        }

        return logEmailGateway;
    }
}
