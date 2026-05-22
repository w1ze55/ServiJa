package com.servija.servija.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class AdminDashboardCountMetricDto {
    private long current;
    private long previous;
    private BigDecimal changePercent;
}
