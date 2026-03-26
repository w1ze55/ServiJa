package com.servija.servija.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationStreamEvent {

    private String eventType;
    private NotificationResponse notification;
    private Long notificationId;
    private long unreadCount;
}
