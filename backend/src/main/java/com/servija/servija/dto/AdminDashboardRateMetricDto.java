package com.servija.servija.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class AdminDashboardRateMetricDto {
    private BigDecimal current;
    private BigDecimal previous;
    private BigDecimal changePercent;
    private long currentCompletedCount;
    private long currentRequestCount;
    private long previousCompletedCount;
    private long previousRequestCount;
}
