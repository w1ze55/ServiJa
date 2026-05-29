package com.servija.servija.dto.chat;

import java.time.Instant;
import java.util.List;

public record ReadReceiptDTO(
    String conversationId,
    String readerEmail,
    String readerRole,
    List<String> messageIds,
    Instant readAt
) {}
