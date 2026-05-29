package com.servija.servija.dto;

import com.servija.servija.model.SupportRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SupportRequestResponse {
    private Long id;
    private Long userId;
    private String userName;
    private String userEmail;
    private SupportRequest.Category category;
    private String description;
    private List<SupportAttachmentDto> attachments;
    private SupportRequest.Status status;
    private String adminResponse;
    private Long adminUserId;
    private String adminName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime answeredAt;
}
