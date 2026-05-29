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
public class PlatformWalletResponse {

    private Long id;
    private BigDecimal availableBalance;
    private BigDecimal reservedBalance;
    private BigDecimal totalEarned;
    private BigDecimal totalWithdrawn;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
