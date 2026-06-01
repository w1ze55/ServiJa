package com.servija.servija.service;

import com.servija.servija.dto.chat.ReadReceiptDTO;
import com.servija.servija.dto.chat.SendMessageRequest;
import com.servija.servija.exception.ResourceNotFoundException;
import com.servija.servija.model.ServiceRequest;
import com.servija.servija.model.User;
import com.servija.servija.model.chat.Conversation;
import com.servija.servija.model.chat.Message;
import com.servija.servija.repository.ConversationRepository;
import com.servija.servija.repository.MessageRepository;
import com.servija.servija.repository.ServiceRequestRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    private ConversationRepository conversationRepository;
    @Mock
    private MessageRepository messageRepository;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @Mock
    private WebSocketService webSocketService;

    @InjectMocks
    private ChatService service;

    @Test
    @DisplayName("getOrCreateConversation cria conversa quando o chat esta habilitado")
    void getOrCreateConversationCreatesConversation() {
        ServiceRequest request = serviceRequest(true);
        when(serviceRequestRepository.findById(10L)).thenReturn(Optional.of(request));
        when(conversationRepository.findByServiceRequestId(10L)).thenReturn(Optional.empty());
        when(conversationRepository.save(any(Conversation.class))).thenAnswer(invocation -> {
            Conversation conversation = invocation.getArgument(0);
            conversation.setId("conv-1");
            return conversation;
        });

        var dto = service.getOrCreateConversation(10L, "client@test.com");

        assertThat(dto.id()).isEqualTo("conv-1");
        assertThat(dto.contractorEmail()).isEqualTo("client@test.com");
        assertThat(dto.providerEmail()).isEqualTo("freela@test.com");
    }

    @Test
    @DisplayName("sendMessage rejeita conversa bloqueada")
    void sendMessageRejectsLockedConversation() {
        Conversation conversation = conversation(true);
        when(conversationRepository.findById("conv-1")).thenReturn(Optional.of(conversation));

        assertThatThrownBy(() -> service.sendMessage("conv-1", "client@test.com", new SendMessageRequest("oi", "text", null, null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("bloqueado");
    }

    @Test
    @DisplayName("markAsRead zera unread e publica recibo quando ha mensagens nao lidas")
    void markAsReadMarksMessagesAndNotifies() {
        Conversation conversation = conversation(false);
        when(conversationRepository.findById("conv-1")).thenReturn(Optional.of(conversation));
        Message message = Message.builder()
                .id("msg-1")
                .conversationId("conv-1")
                .senderId("freela@test.com")
                .status("sent")
                .sentAt(Instant.now())
                .build();
        when(messageRepository.findByConversationIdAndSenderIdNotAndReadAtIsNull("conv-1", "client@test.com"))
                .thenReturn(List.of(message));

        service.markAsRead("conv-1", "client@test.com");

        assertThat(conversation.getUnreadCount().get("contractor")).isZero();
        verify(messageRepository).saveAll(List.of(message));
        ArgumentCaptor<ReadReceiptDTO> captor = ArgumentCaptor.forClass(ReadReceiptDTO.class);
        verify(webSocketService).notifyChatRead(any(), captor.capture());
        assertThat(captor.getValue().messageIds()).containsExactly("msg-1");
    }

    @Test
    @DisplayName("getMessages falha para usuario que nao participa da conversa")
    void getMessagesFailsForNonParticipant() {
        when(conversationRepository.findById("conv-1")).thenReturn(Optional.of(conversation(false)));

        assertThatThrownBy(() -> service.getMessages("conv-1", "outsider@test.com", 0, 10))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Conversa");
    }

    private ServiceRequest serviceRequest(boolean chatEnabled) {
        return ServiceRequest.builder()
                .id(10L)
                .contractor(User.builder().email("client@test.com").name("Cliente").build())
                .freelancer(User.builder().email("freela@test.com").name("Freela").build())
                .chatEnabled(chatEnabled)
                .build();
    }

    private Conversation conversation(boolean locked) {
        return Conversation.builder()
                .id("conv-1")
                .serviceRequestId(10L)
                .contractorEmail("client@test.com")
                .providerEmail("freela@test.com")
                .locked(locked)
                .unreadCount(new HashMap<>(Map.of("contractor", 2, "provider", 0)))
                .build();
    }
}
