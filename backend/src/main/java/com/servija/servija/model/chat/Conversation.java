package com.servija.servija.model.chat;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Document(collection = "conversations")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Conversation {

    @Id
    private String id;

    @Indexed(unique = true)
    private Long serviceRequestId;

    private String contractorEmail;
    private String providerEmail;

    private boolean locked;

    private LastMessage lastMessage;

    private Map<String, Integer> unreadCount;

    private Instant createdAt;
    private Instant updatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LastMessage {
        private String content;
        private String senderId;
        private Instant sentAt;
    }
}
