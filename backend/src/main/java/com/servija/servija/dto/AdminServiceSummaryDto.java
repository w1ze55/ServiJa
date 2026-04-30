package com.servija.servija.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class AdminServiceSummaryDto {
    private Long id;
    private String title;
    private String category;
    private BigDecimal price;
    private String priceType;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
