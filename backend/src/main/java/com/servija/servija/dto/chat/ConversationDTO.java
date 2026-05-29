package com.servija.servija.dto.chat;

import java.time.Instant;
import java.util.Map;

public record ConversationDTO(
    String id,
    Long serviceRequestId,
    String contractorEmail,
    String contractorName,
    String contractorAvatarUrl,
    String providerEmail,
    String providerName,
    String providerAvatarUrl,
    boolean locked,
    LastMessageDTO lastMessage,
    Map<String, Integer> unreadCount,
    Instant createdAt,
    Instant updatedAt
) {
    public record LastMessageDTO(String content, String senderId, Instant sentAt) {}
}
