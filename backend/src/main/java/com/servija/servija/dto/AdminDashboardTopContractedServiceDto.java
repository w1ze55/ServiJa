package com.servija.servija.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminDashboardTopContractedServiceDto {
    private String label;
    private long contractsCount;
}
