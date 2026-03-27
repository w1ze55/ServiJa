package com.servija.servija.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawalResponse {

    private Long id;
    private BigDecimal amount;
    private String status;
    private String pixKey;
    private String pixKeyType;
    private String transactionId;
    private String rejectionReason;
    private LocalDateTime processedAt;
    private LocalDateTime createdAt;
}
