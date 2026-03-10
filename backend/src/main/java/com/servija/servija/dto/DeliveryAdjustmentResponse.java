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
public class DeliveryAdjustmentResponse {
    private Long id;
    private Long serviceRequestId;
    private String requestedByName;
    private String requestedByEmail;
    private String reason;
    private String status;
    private String freelancerResponse;
    private LocalDateTime respondedAt;
    private LocalDateTime createdAt;
}
