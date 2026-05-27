package com.servija.servija.service;

import com.servija.servija.dto.chat.ConversationDTO;
import com.servija.servija.dto.chat.MessageDTO;
import com.servija.servija.dto.chat.SendMessageRequest;
import com.servija.servija.exception.ResourceNotFoundException;
import com.servija.servija.model.ServiceRequest;
import com.servija.servija.model.chat.Conversation;
import com.servija.servija.model.chat.Message;
import com.servija.servija.repository.ConversationRepository;
import com.servija.servija.repository.MessageRepository;
import com.servija.servija.repository.ServiceRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final ConversationRepository conversationRepository;
    private final MessageRepository messageRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final WebSocketService webSocketService;

    public ConversationDTO getOrCreateConversation(Long serviceRequestId, String userEmail) {
        ServiceRequest sr = serviceRequestRepository.findById(serviceRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação não encontrada"));

        assertParticipant(sr, userEmail);

        if (!Boolean.TRUE.equals(sr.getChatEnabled())) {
            throw new IllegalStateException("Chat não está habilitado para esta solicitação");
        }

        Conversation conv = conversationRepository.findByServiceRequestId(serviceRequestId)
                .orElseGet(() -> {
                    String providerEmail = sr.getFreelancer() != null ? sr.getFreelancer().getEmail() : null;
                    Conversation newConv = Conversation.builder()
                            .serviceRequestId(serviceRequestId)
                            .contractorEmail(sr.getContractor().getEmail())
                            .providerEmail(providerEmail)
                            .locked(false)
                            .unreadCount(Map.of("contractor", 0, "provider", 0))
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build();
                    return conversationRepository.save(newConv);
                });

        return toDTO(conv);
    }

    public List<ConversationDTO> listConversations(String userEmail) {
        return conversationRepository
                .findByContractorEmailOrProviderEmailOrderByUpdatedAtDesc(userEmail, userEmail)
                .stream()
                .map(this::toDTO)
                .toList();
    }

    public Page<MessageDTO> getMessages(String conversationId, String userEmail, int page, int size) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversa não encontrada"));

        assertConversationParticipant(conv, userEmail);

        return messageRepository
                .findByConversationIdOrderBySentAtAsc(conversationId, PageRequest.of(page, size))
                .map(this::toDTO);
    }

    public MessageDTO sendMessage(String conversationId, String userEmail, SendMessageRequest request) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversa não encontrada"));

        assertConversationParticipant(conv, userEmail);

        if (conv.isLocked()) {
            throw new IllegalStateException("Chat bloqueado para esta solicitação");
        }

        String senderType = userEmail.equals(conv.getContractorEmail()) ? "contractor" : "provider";
        String recipientRole = senderType.equals("contractor") ? "provider" : "contractor";

        Message message = Message.builder()
                .conversationId(conversationId)
                .senderId(userEmail)
                .senderType(senderType)
                .type(request.type() != null ? request.type() : "text")
                .content(request.content())
                .attachmentUrl(request.attachmentUrl())
                .status("sent")
                .sentAt(Instant.now())
                .build();

        message = messageRepository.save(message);

        Map<String, Integer> unread = new HashMap<>(conv.getUnreadCount() != null ? conv.getUnreadCount() : Map.of());
        unread.merge(recipientRole, 1, Integer::sum);

        conv.setLastMessage(Conversation.LastMessage.builder()
                .content(request.content())
                .senderId(userEmail)
                .sentAt(message.getSentAt())
                .build());
        conv.setUnreadCount(unread);
        conv.setUpdatedAt(Instant.now());
        conversationRepository.save(conv);

        MessageDTO dto = toDTO(message);
        webSocketService.notifyChatMessage(conversationId, dto);
        return dto;
    }

    public void lockConversationByServiceRequest(Long serviceRequestId) {
        conversationRepository.findByServiceRequestId(serviceRequestId).ifPresent(conv -> {
            if (!conv.isLocked()) {
                conv.setLocked(true);
                conv.setUpdatedAt(Instant.now());
                conversationRepository.save(conv);
            }
        });
    }

    public void markAsRead(String conversationId, String userEmail) {
        Conversation conv = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ResourceNotFoundException("Conversa não encontrada"));

        assertConversationParticipant(conv, userEmail);

        String role = userEmail.equals(conv.getContractorEmail()) ? "contractor" : "provider";
        Map<String, Integer> unread = new HashMap<>(conv.getUnreadCount() != null ? conv.getUnreadCount() : Map.of());
        unread.put(role, 0);
        conv.setUnreadCount(unread);
        conv.setUpdatedAt(Instant.now());
        conversationRepository.save(conv);
    }

    private void assertParticipant(ServiceRequest sr, String email) {
        boolean isContractor = sr.getContractor().getEmail().equals(email);
        boolean isProvider = sr.getFreelancer() != null && sr.getFreelancer().getEmail().equals(email);
        if (!isContractor && !isProvider) {
            throw new ResourceNotFoundException("Solicitação não encontrada");
        }
    }

    private void assertConversationParticipant(Conversation conv, String email) {
        boolean ok = email.equals(conv.getContractorEmail()) || email.equals(conv.getProviderEmail());
        if (!ok) {
            throw new ResourceNotFoundException("Conversa não encontrada");
        }
    }

    private ConversationDTO toDTO(Conversation c) {
        ConversationDTO.LastMessageDTO lastMsg = c.getLastMessage() == null ? null :
                new ConversationDTO.LastMessageDTO(
                        c.getLastMessage().getContent(),
                        c.getLastMessage().getSenderId(),
                        c.getLastMessage().getSentAt());
        return new ConversationDTO(c.getId(), c.getServiceRequestId(),
                c.getContractorEmail(), c.getProviderEmail(), c.isLocked(),
                lastMsg, c.getUnreadCount(), c.getCreatedAt(), c.getUpdatedAt());
    }

    private MessageDTO toDTO(Message m) {
        return new MessageDTO(m.getId(), m.getConversationId(), m.getSenderId(),
                m.getSenderType(), m.getType(), m.getContent(),
                m.getAttachmentUrl(), m.getStatus(), m.getSentAt());
    }
}
