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
public class PaymentResponse {

    private Long id;
    private Long serviceRequestId;
    private BigDecimal amount;
    private BigDecimal platformFee;
    private BigDecimal gatewayFee;
    private BigDecimal totalAmount;
    private String paymentMethod;
    private Integer installmentCount;
    private String status;
    private String transactionId;
    private String asaasPaymentId;
    private String pixQrCode;
    private String pixCopyPaste;
    private LocalDateTime createdAt;
    private LocalDateTime completedAt;
}
