package com.servija.servija.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class AdminDashboardPeriodDto {
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDate previousStartDate;
    private LocalDate previousEndDate;
    private String granularity;
    private boolean allTime;
}
