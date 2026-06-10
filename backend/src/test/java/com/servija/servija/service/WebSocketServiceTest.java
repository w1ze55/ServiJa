package com.servija.servija.service;

import com.servija.servija.dto.NotificationStreamEvent;
import com.servija.servija.dto.chat.ReadReceiptDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class WebSocketServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private WebSocketService service;

    @Test
    @DisplayName("notifyServiceRequestStatusChange publica para contratante e freelancer")
    void notifyServiceRequestStatusChangePublishesToBothParticipants() {
        service.notifyServiceRequestStatusChange(
                10L,
                "COMPLETED",
                "client@test.com",
                "freela@test.com"
        );

        ArgumentCaptor<String> destinationCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate, times(2)).convertAndSend(destinationCaptor.capture(), payloadCaptor.capture());

        assertThat(destinationCaptor.getAllValues()).containsExactly(
                "/topic/service-request/client@test.com",
                "/topic/service-request/freela@test.com"
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) payloadCaptor.getAllValues().get(0);
        assertThat(payload)
                .containsEntry("type", "SERVICE_REQUEST_STATUS_CHANGED")
                .containsEntry("serviceRequestId", 10L)
                .containsEntry("status", "COMPLETED");
        assertThat(payload).containsKey("timestamp");
    }

    @Test
    @DisplayName("notifyPaymentFailed publica no topico de pagamento com motivo e mensagem")
    void notifyPaymentFailedPublishesReasonToPaymentTopic() {
        service.notifyPaymentFailed(55L, "client@test.com", "saldo insuficiente");

        ArgumentCaptor<Object> payloadCaptor = ArgumentCaptor.forClass(Object.class);
        verify(messagingTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq("/topic/payment/client@test.com"),
                payloadCaptor.capture()
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> payload = (Map<String, Object>) payloadCaptor.getValue();
        assertThat(payload)
                .containsEntry("type", "PAYMENT_FAILED")
                .containsEntry("paymentId", 55L)
                .containsEntry("reason", "saldo insuficiente")
                .containsEntry("message", "Falha no pagamento: saldo insuficiente");
        assertThat(payload).containsKey("timestamp");
    }

    @Test
    @DisplayName("notifyChatRead publica read receipt no topico de leitura da conversa")
    void notifyChatReadPublishesToConversationReadTopic() {
        ReadReceiptDTO receipt = new ReadReceiptDTO(
                "conv-1",
                "client@test.com",
                "contractor",
                List.of("msg-1"),
                Instant.parse("2026-06-09T21:00:00Z")
        );

        service.notifyChatRead("conv-1", receipt);

        verify(messagingTemplate).convertAndSend("/topic/chat/conv-1/read", receipt);
    }

    @Test
    @DisplayName("publishNotificationEvent publica evento persistido no stream do usuario")
    void publishNotificationEventPublishesToUserNotificationTopic() {
        NotificationStreamEvent event = NotificationStreamEvent.builder()
                .eventType("CREATED")
                .notificationId(99L)
                .unreadCount(3L)
                .build();

        service.publishNotificationEvent("user@test.com", event);

        verify(messagingTemplate).convertAndSend("/topic/notifications/user@test.com", event);
    }
}
