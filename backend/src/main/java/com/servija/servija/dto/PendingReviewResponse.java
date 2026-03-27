package com.servija.servija.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingReviewResponse {

    private Long serviceRequestId;
    private String serviceName;
    private String targetName;
    private String targetAvatar;
    private String targetType;
    private LocalDateTime completedAt;
}
