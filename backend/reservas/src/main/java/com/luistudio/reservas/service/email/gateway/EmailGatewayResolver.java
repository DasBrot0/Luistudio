package com.luistudio.reservas.service.email.gateway;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class EmailGatewayResolver {

    private final String configuredProvider;
    private final List<EmailGateway> gateways;

    public EmailGatewayResolver(
        @Value("${app.email.provider:log}") String configuredProvider,
        List<EmailGateway> gateways
    ) {
        this.configuredProvider = configuredProvider;
        this.gateways = gateways;
    }

    public EmailGateway resolve() {
        return gateways.stream()
            .filter(gateway -> gateway.provider().equalsIgnoreCase(configuredProvider))
            .filter(EmailGateway::isConfigured)
            .findFirst()
            .orElseGet(this::fallbackToLog);
    }

    private EmailGateway fallbackToLog() {
        log.warn("[OUTBOX] Provider de correo '{}' no configurado o no soportado. Usando fallback log.", configuredProvider);

        return gateways.stream()
            .filter(gateway -> gateway.provider().equalsIgnoreCase("log"))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No existe LogEmailGateway como fallback"));
    }
}
