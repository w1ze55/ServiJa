package com.servija.servija.model.chat;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

@Document(collection = "messages")
@CompoundIndex(name = "conv_sent_idx", def = "{'conversationId': 1, 'sentAt': 1}")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Message {

    @Id
    private String id;

    private String conversationId;
    private String senderId;
    private String senderType; // "contractor" | "provider"
    private String type;       // "text" | "image" | "file" | "system"
    private String content;
    private String attachmentUrl;
    private String status;     // "sent" | "delivered" | "read"
    private Instant sentAt;
    private Instant readAt;
}
