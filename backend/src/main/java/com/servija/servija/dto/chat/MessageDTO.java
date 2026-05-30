package com.servija.servija.dto.chat;

import java.time.Instant;

public record MessageDTO(
    String id,
    String conversationId,
    String senderId,
    String senderType,
    String type,
    String content,
    String attachmentUrl,
    String attachmentName,
    String status,
    Instant sentAt,
    Instant readAt
) {}
