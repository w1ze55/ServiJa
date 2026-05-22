package com.servija.servija.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminDashboardPaymentStatusDto {
    private String key;
    private String label;
    private long count;
}
