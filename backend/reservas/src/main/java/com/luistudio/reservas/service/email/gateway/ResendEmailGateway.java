package com.luistudio.reservas.service.email.gateway;

import com.luistudio.reservas.model.EmailOutboxEntity;
import com.luistudio.reservas.service.email.EmailTemplateService;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class ResendEmailGateway implements EmailGateway {

    private final RestClient restClient;
    private final String emailFrom;
    private final String resendApiKey;
    private final EmailTemplateService emailTemplateService;

    public ResendEmailGateway(
        RestClient.Builder restClientBuilder,
        @Value("${app.email.from:Luistudio <onboarding@resend.dev>}") String emailFrom,
        @Value("${app.email.resend.api-key:}") String resendApiKey,
        EmailTemplateService emailTemplateService
    ) {
        this.restClient = restClientBuilder.baseUrl("https://api.resend.com").build();
        this.emailFrom = emailFrom;
        this.resendApiKey = resendApiKey;
        this.emailTemplateService = emailTemplateService;
    }

    @Override
    public void send(EmailOutboxEntity email) {
        boolean html = emailTemplateService.isHtml(email.getCuerpo());
        Map<String, Object> payload = Map.of(
            "from", emailFrom,
            "to", List.of(email.getDestinatario()),
            "subject", email.getAsunto(),
            html ? "html" : "text", email.getCuerpo()
        );

        String response = restClient.post()
            .uri("/emails")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + resendApiKey)
            .contentType(MediaType.APPLICATION_JSON)
            .body(payload)
            .retrieve()
            .body(String.class);

        log.info("[RESEND] Email enviado a {} | Subject: {} | Response: {}", email.getDestinatario(), email.getAsunto(), response);
    }
}
