package com.luistudio.reservas.service.email.gateway;

import com.luistudio.reservas.model.EmailOutboxEntity;
import com.luistudio.reservas.service.email.EmailTemplateService;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class GmailEmailGateway implements EmailGateway {

    private final RestClient oauthClient;
    private final RestClient gmailClient;
    private final String clientId;
    private final String clientSecret;
    private final String refreshToken;
    private final String emailFrom;
    private final EmailTemplateService emailTemplateService;

    public GmailEmailGateway(
        RestClient.Builder restClientBuilder,
        @Value("${app.email.gmail.client-id:}") String clientId,
        @Value("${app.email.gmail.client-secret:}") String clientSecret,
        @Value("${app.email.gmail.refresh-token:}") String refreshToken,
        @Value("${app.email.from:}") String emailFrom,
        EmailTemplateService emailTemplateService
    ) {
        this.oauthClient = restClientBuilder.baseUrl("https://oauth2.googleapis.com").build();
        this.gmailClient = restClientBuilder.baseUrl("https://gmail.googleapis.com").build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.refreshToken = refreshToken;
        this.emailFrom = emailFrom;
        this.emailTemplateService = emailTemplateService;
    }

    @Override
    public void send(EmailOutboxEntity email) {
        String accessToken = fetchAccessToken();
        String rawMessage = buildRawMessage(email);
        String encodedRaw = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(rawMessage.getBytes(StandardCharsets.UTF_8));

        gmailClient.post()
            .uri("/gmail/v1/users/me/messages/send")
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .contentType(MediaType.APPLICATION_JSON)
            .body(Map.of("raw", encodedRaw))
            .retrieve()
            .body(String.class);
    }

    private String fetchAccessToken() {
        Map<String, Object> tokenResponse = oauthClient.post()
            .uri("/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(
                "client_id=" + encode(clientId)
                    + "&client_secret=" + encode(clientSecret)
                    + "&refresh_token=" + encode(refreshToken)
                    + "&grant_type=refresh_token"
            )
            .retrieve()
            .body(Map.class);

        if (tokenResponse == null || tokenResponse.get("access_token") == null) {
            throw new IllegalStateException("No se pudo obtener access_token de Gmail API");
        }

        return String.valueOf(tokenResponse.get("access_token"));
    }

    private String buildRawMessage(EmailOutboxEntity email) {
        boolean html = emailTemplateService.isHtml(email.getCuerpo());
        String contentType = html
            ? "text/html; charset=UTF-8"
            : "text/plain; charset=UTF-8";
        return "From: " + emailFrom + "\r\n"
            + "To: " + email.getDestinatario() + "\r\n"
            + "Subject: " + sanitizeHeader(email.getAsunto()) + "\r\n"
            + "MIME-Version: 1.0\r\n"
            + "Content-Type: " + contentType + "\r\n"
            + "\r\n"
            + (email.getCuerpo() == null ? "" : email.getCuerpo());
    }

    private String encode(String value) {
        return java.net.URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private String sanitizeHeader(String value) {
        if (value == null) return "";
        return value.replace("\r", " ").replace("\n", " ");
    }
}
