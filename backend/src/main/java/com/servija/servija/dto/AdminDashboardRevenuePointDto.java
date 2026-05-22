package com.servija.servija.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class AdminDashboardRevenuePointDto {
    private String key;
    private String label;
    private BigDecimal amount;
}
