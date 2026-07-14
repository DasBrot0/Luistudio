package com.luistudio.reservas.service.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.luistudio.reservas.model.EmailOutboxEntity;
import com.luistudio.reservas.model.EmailStatus;
import com.luistudio.reservas.model.RoomAvailabilitySubscriptionEntity;
import com.luistudio.reservas.repository.EmailOutboxRepository;
import com.luistudio.reservas.repository.RoomAvailabilitySubscriptionRepository;
import com.luistudio.reservas.service.email.gateway.EmailGateway;
import com.luistudio.reservas.service.email.gateway.EmailGatewayResolver;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmailDispatchServiceTest {

    @Mock private EmailOutboxRepository emailOutboxRepository;
    @Mock private EmailGatewayResolver emailGatewayResolver;
    @Mock private RoomAvailabilitySubscriptionRepository subscriptionRepository;
    @Mock private EmailGateway emailGateway;

    private EmailDispatchService service;
    private EmailOutboxEntity email;
    private RoomAvailabilitySubscriptionEntity subscription;

    @BeforeEach
    void setUp() {
        service = new EmailDispatchService(emailOutboxRepository, emailGatewayResolver, subscriptionRepository);
        email = new EmailOutboxEntity();
        email.setId(20L);
        email.setDestinatario("student@example.com");
        email.setAsunto("Sala disponible");
        email.setCuerpo("Disponible");
        email.setPayload(new ObjectMapper().createObjectNode()
            .put("notificationType", "ROOM_AVAILABLE")
            .put("subscriptionId", 10L));
        subscription = new RoomAvailabilitySubscriptionEntity();
        subscription.setId(10L);
        subscription.setStatus("EN_COLA");

        when(emailOutboxRepository.findReadyToProcess(eq(EmailStatus.PENDIENTE), any(), any()))
            .thenReturn(List.of(email));
        when(emailGatewayResolver.resolve()).thenReturn(emailGateway);
        when(subscriptionRepository.findById(10L)).thenReturn(Optional.of(subscription));
    }

    @Test
    void marksQueuedAvailabilitySubscriptionAsNotifiedOnlyAfterSuccessfulSend() {
        service.processPendingEmails();

        assertThat(email.getEstado()).isEqualTo(EmailStatus.ENVIADO);
        assertThat(subscription.getStatus()).isEqualTo("NOTIFICADA");
        assertThat(subscription.getNotifiedAt()).isNotNull();
    }

    @Test
    void restoresSubscriptionWhenEmailExhaustsItsRetries() {
        email.setIntentos(2);
        doThrow(new IllegalStateException("provider unavailable")).when(emailGateway).send(email);

        service.processPendingEmails();

        assertThat(email.getEstado()).isEqualTo(EmailStatus.ERROR);
        assertThat(subscription.getStatus()).isEqualTo("ACTIVA");
        assertThat(subscription.getNotifiedAt()).isNull();
    }
}
