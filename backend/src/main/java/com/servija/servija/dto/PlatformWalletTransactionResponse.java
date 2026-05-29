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
public class PlatformWalletTransactionResponse {

    private Long id;
    private String type; // FEE, WITHDRAWAL
    private BigDecimal amount;
    private String status;
    private String description;
    private String paymentMethod;
    private String serviceName;
    private String referenceLabel;
    private LocalDateTime date;
}
