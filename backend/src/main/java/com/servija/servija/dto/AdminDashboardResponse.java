package com.servija.servija.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class AdminDashboardResponse {
    private AdminDashboardPeriodDto period;
    private AdminDashboardSummaryDto summary;
    private List<AdminDashboardPaymentStatusDto> paymentStatusBreakdown;
    private List<AdminDashboardRevenuePointDto> revenueSeries;
    private List<AdminDashboardTopContractedServiceDto> topContractedServices;
}
