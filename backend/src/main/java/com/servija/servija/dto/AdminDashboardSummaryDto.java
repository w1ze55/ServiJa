package com.servija.servija.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminDashboardSummaryDto {
    private AdminDashboardMoneyMetricDto totalRevenue;
    private AdminDashboardMoneyMetricDto serviJaFee;
    private AdminDashboardCountMetricDto newUsers;
    private AdminDashboardCountMetricDto completedServices;
    private AdminDashboardMoneyMetricDto averageTicket;
    private AdminDashboardCountMetricDto activeClients;
    private AdminDashboardCountMetricDto activeProfessionals;
    private AdminDashboardRateMetricDto conversionRate;
}
