package com.servija.servija.service;

import com.servija.servija.dto.AdminDashboardResponse;
import com.servija.servija.model.Payment;
import com.servija.servija.model.ServiceRequestHistory;
import com.servija.servija.model.User;
import com.servija.servija.repository.PaymentRepository;
import com.servija.servija.repository.ServiceRequestHistoryRepository;
import com.servija.servija.repository.ServiceRequestRepository;
import com.servija.servija.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminDashboardServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private ServiceRequestRepository serviceRequestRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private ServiceRequestHistoryRepository serviceRequestHistoryRepository;

    @InjectMocks
    private AdminDashboardService adminDashboardService;

    @Test
    @DisplayName("Deve montar o dashboard usando todo o periodo disponivel")
    void getDashboard_defaultsToAllTime() {
        LocalDate today = LocalDate.now();
        LocalDate expectedStart = LocalDate.of(2026, 1, 5);
        LocalDateTime currentStart = expectedStart.atStartOfDay();
        LocalDateTime currentEnd = today.plusDays(1).atStartOfDay();
        LocalDate previousStart = expectedStart.minusDays(today.toEpochDay() - expectedStart.toEpochDay() + 1L);
        LocalDate previousEnd = expectedStart.minusDays(1);
        LocalDateTime previousStartDateTime = previousStart.atStartOfDay();
        LocalDateTime previousEndDateTime = previousEnd.plusDays(1).atStartOfDay();

        when(userRepository.findEarliestCreatedAtByUserTypeIn(List.of(User.UserType.CONTRACTOR, User.UserType.FREELANCER)))
                .thenReturn(expectedStart.atTime(8, 0));
        when(serviceRequestRepository.findEarliestCreatedAt()).thenReturn(LocalDateTime.of(2026, 1, 10, 10, 0));
        when(paymentRepository.findEarliestCreatedAt()).thenReturn(LocalDateTime.of(2026, 1, 15, 11, 0));

        when(userRepository.countByUserTypeInAndCreatedAtBetween(
                List.of(User.UserType.CONTRACTOR, User.UserType.FREELANCER), currentStart, currentEnd))
                .thenReturn(10L);
        when(userRepository.countByUserTypeInAndCreatedAtBetween(
                List.of(User.UserType.CONTRACTOR, User.UserType.FREELANCER), previousStartDateTime, previousEndDateTime))
                .thenReturn(5L);

        when(serviceRequestHistoryRepository.countDistinctServiceRequestsByOperationTypeAndCreatedAtBetween(
                ServiceRequestHistory.OperationType.SERVICE_COMPLETED, currentStart, currentEnd))
                .thenReturn(5L);
        when(serviceRequestHistoryRepository.countDistinctServiceRequestsByOperationTypeAndCreatedAtBetween(
                ServiceRequestHistory.OperationType.SERVICE_COMPLETED, previousStartDateTime, previousEndDateTime))
                .thenReturn(2L);

        when(serviceRequestHistoryRepository.sumCompletedRevenueByOperationTypeAndCreatedAtBetween(
                ServiceRequestHistory.OperationType.SERVICE_COMPLETED, currentStart, currentEnd))
                .thenReturn(new BigDecimal("1000.00"));
        when(serviceRequestHistoryRepository.sumCompletedRevenueByOperationTypeAndCreatedAtBetween(
                ServiceRequestHistory.OperationType.SERVICE_COMPLETED, previousStartDateTime, previousEndDateTime))
                .thenReturn(new BigDecimal("500.00"));

        when(serviceRequestRepository.countCreatedBetween(currentStart, currentEnd)).thenReturn(8L);
        when(serviceRequestRepository.countCreatedBetween(previousStartDateTime, previousEndDateTime)).thenReturn(4L);
        when(serviceRequestRepository.countDistinctActiveContractorsBetween(currentStart, currentEnd)).thenReturn(7L);
        when(serviceRequestRepository.countDistinctActiveContractorsBetween(previousStartDateTime, previousEndDateTime)).thenReturn(3L);
        when(serviceRequestRepository.countDistinctActiveFreelancersBetween(currentStart, currentEnd)).thenReturn(6L);
        when(serviceRequestRepository.countDistinctActiveFreelancersBetween(previousStartDateTime, previousEndDateTime)).thenReturn(2L);

        when(paymentRepository.countStatusesCreatedBetween(currentStart, currentEnd)).thenReturn(List.of(
                paymentStatusProjection(Payment.PaymentStatus.COMPLETED, 4L),
                paymentStatusProjection(Payment.PaymentStatus.PROCESSING, 2L),
                paymentStatusProjection(Payment.PaymentStatus.OVERDUE, 1L),
                paymentStatusProjection(Payment.PaymentStatus.CANCELLED, 1L),
                paymentStatusProjection(Payment.PaymentStatus.REFUNDED, 2L),
                paymentStatusProjection(Payment.PaymentStatus.FAILED, 1L)
        ));

        when(serviceRequestHistoryRepository.findRevenueSeriesByMonth(
                ServiceRequestHistory.OperationType.SERVICE_COMPLETED.name(), currentStart, currentEnd))
                .thenReturn(List.of(
                        revenueByMonth("2026-01-01", "300.00"),
                        revenueByMonth("2026-03-01", "700.00")
                ));

        when(serviceRequestHistoryRepository.findTopContractedServicesByOperationTypeInAndCreatedAtBetween(
                List.of(
                        ServiceRequestHistory.OperationType.BUDGET_ACCEPTED,
                        ServiceRequestHistory.OperationType.COUNTER_PROPOSAL_ACCEPTED
                ),
                currentStart,
                currentEnd))
                .thenReturn(List.of(
                        topServiceProjection("Eletricista residencial", 9L),
                        topServiceProjection("Pintura", 7L)
                ));

        AdminDashboardResponse response = adminDashboardService.getDashboard(null, null);

        assertThat(response.getPeriod().isAllTime()).isTrue();
        assertThat(response.getPeriod().getStartDate()).isEqualTo(expectedStart);
        assertThat(response.getSummary().getTotalRevenue().getCurrent()).isEqualByComparingTo("1000.00");
        assertThat(response.getSummary().getServiJaFee().getCurrent()).isEqualByComparingTo("50.00");
        assertThat(response.getSummary().getAverageTicket().getCurrent()).isEqualByComparingTo("200.00");
        assertThat(response.getSummary().getNewUsers().getChangePercent()).isEqualByComparingTo("100.0");
        assertThat(response.getSummary().getCompletedServices().getChangePercent()).isEqualByComparingTo("150.0");
        assertThat(response.getSummary().getConversionRate().getCurrent()).isEqualByComparingTo("62.5");
        assertThat(response.getSummary().getConversionRate().getChangePercent()).isEqualByComparingTo("25.0");
        assertThat(response.getPaymentStatusBreakdown()).extracting("key", "count").containsExactly(
                org.assertj.core.groups.Tuple.tuple("COMPLETED", 4L),
                org.assertj.core.groups.Tuple.tuple("PENDING", 3L),
                org.assertj.core.groups.Tuple.tuple("CANCELLED", 1L),
                org.assertj.core.groups.Tuple.tuple("REFUNDED", 2L),
                org.assertj.core.groups.Tuple.tuple("OTHERS", 1L)
        );
        assertThat(response.getRevenueSeries()).hasSize(5);
        assertThat(response.getRevenueSeries().get(1).getAmount()).isEqualByComparingTo("0.00");
        assertThat(response.getRevenueSeries().get(2).getAmount()).isEqualByComparingTo("700.00");
        assertThat(response.getTopContractedServices()).extracting("label", "contractsCount").containsExactly(
                org.assertj.core.groups.Tuple.tuple("Eletricista residencial", 9L),
                org.assertj.core.groups.Tuple.tuple("Pintura", 7L)
        );
    }

    @Test
    @DisplayName("Deve retornar metricas zeradas e serie diaria vazia quando nao houver dados no periodo")
    void getDashboard_returnsZeroStateForCustomRangeWithoutData() {
        LocalDate start = LocalDate.of(2026, 5, 1);
        LocalDate end = LocalDate.of(2026, 5, 7);
        LocalDateTime currentStart = start.atStartOfDay();
        LocalDateTime currentEnd = end.plusDays(1).atStartOfDay();
        LocalDateTime previousStart = LocalDate.of(2026, 4, 24).atStartOfDay();
        LocalDateTime previousEnd = LocalDate.of(2026, 4, 30).plusDays(1).atStartOfDay();

        when(userRepository.countByUserTypeInAndCreatedAtBetween(
                List.of(User.UserType.CONTRACTOR, User.UserType.FREELANCER), currentStart, currentEnd))
                .thenReturn(0L);
        when(userRepository.countByUserTypeInAndCreatedAtBetween(
                List.of(User.UserType.CONTRACTOR, User.UserType.FREELANCER), previousStart, previousEnd))
                .thenReturn(0L);

        when(serviceRequestHistoryRepository.countDistinctServiceRequestsByOperationTypeAndCreatedAtBetween(
                ServiceRequestHistory.OperationType.SERVICE_COMPLETED, currentStart, currentEnd))
                .thenReturn(0L);
        when(serviceRequestHistoryRepository.countDistinctServiceRequestsByOperationTypeAndCreatedAtBetween(
                ServiceRequestHistory.OperationType.SERVICE_COMPLETED, previousStart, previousEnd))
                .thenReturn(0L);

        when(serviceRequestHistoryRepository.sumCompletedRevenueByOperationTypeAndCreatedAtBetween(
                ServiceRequestHistory.OperationType.SERVICE_COMPLETED, currentStart, currentEnd))
                .thenReturn(null);
        when(serviceRequestHistoryRepository.sumCompletedRevenueByOperationTypeAndCreatedAtBetween(
                ServiceRequestHistory.OperationType.SERVICE_COMPLETED, previousStart, previousEnd))
                .thenReturn(null);

        when(serviceRequestRepository.countCreatedBetween(currentStart, currentEnd)).thenReturn(0L);
        when(serviceRequestRepository.countCreatedBetween(previousStart, previousEnd)).thenReturn(0L);
        when(serviceRequestRepository.countDistinctActiveContractorsBetween(currentStart, currentEnd)).thenReturn(0L);
        when(serviceRequestRepository.countDistinctActiveContractorsBetween(previousStart, previousEnd)).thenReturn(0L);
        when(serviceRequestRepository.countDistinctActiveFreelancersBetween(currentStart, currentEnd)).thenReturn(0L);
        when(serviceRequestRepository.countDistinctActiveFreelancersBetween(previousStart, previousEnd)).thenReturn(0L);

        when(paymentRepository.countStatusesCreatedBetween(currentStart, currentEnd)).thenReturn(List.of());
        when(serviceRequestHistoryRepository.findRevenueSeriesByDay(
                ServiceRequestHistory.OperationType.SERVICE_COMPLETED.name(), currentStart, currentEnd))
                .thenReturn(List.of());
        when(serviceRequestHistoryRepository.findTopContractedServicesByOperationTypeInAndCreatedAtBetween(
                List.of(
                        ServiceRequestHistory.OperationType.BUDGET_ACCEPTED,
                        ServiceRequestHistory.OperationType.COUNTER_PROPOSAL_ACCEPTED
                ),
                currentStart,
                currentEnd))
                .thenReturn(List.of());

        AdminDashboardResponse response = adminDashboardService.getDashboard(start, end);

        assertThat(response.getPeriod().isAllTime()).isFalse();
        assertThat(response.getPeriod().getGranularity()).isEqualTo("DAILY");
        assertThat(response.getPeriod().getPreviousStartDate()).isEqualTo(LocalDate.of(2026, 4, 24));
        assertThat(response.getSummary().getTotalRevenue().getCurrent()).isEqualByComparingTo("0.00");
        assertThat(response.getSummary().getServiJaFee().getCurrent()).isEqualByComparingTo("0.00");
        assertThat(response.getSummary().getNewUsers().getChangePercent()).isEqualByComparingTo("0.0");
        assertThat(response.getSummary().getConversionRate().getCurrent()).isEqualByComparingTo("0.0");
        assertThat(response.getPaymentStatusBreakdown()).extracting("key", "count").containsExactly(
                org.assertj.core.groups.Tuple.tuple("COMPLETED", 0L),
                org.assertj.core.groups.Tuple.tuple("PENDING", 0L),
                org.assertj.core.groups.Tuple.tuple("CANCELLED", 0L),
                org.assertj.core.groups.Tuple.tuple("REFUNDED", 0L)
        );
        assertThat(response.getRevenueSeries()).hasSize(7);
        assertThat(response.getRevenueSeries()).allSatisfy(point ->
                assertThat(point.getAmount()).isEqualByComparingTo("0.00"));
        assertThat(response.getTopContractedServices()).isEmpty();
    }

    private PaymentRepository.PaymentStatusCountProjection paymentStatusProjection(
            Payment.PaymentStatus status,
            long total) {
        return new PaymentRepository.PaymentStatusCountProjection() {
            @Override
            public Payment.PaymentStatus getStatus() {
                return status;
            }

            @Override
            public long getTotal() {
                return total;
            }
        };
    }

    private ServiceRequestHistoryRepository.RevenueByMonthProjection revenueByMonth(
            String bucketMonth,
            String revenue) {
        return new ServiceRequestHistoryRepository.RevenueByMonthProjection() {
            @Override
            public String getBucketMonth() {
                return bucketMonth;
            }

            @Override
            public BigDecimal getRevenue() {
                return new BigDecimal(revenue);
            }
        };
    }

    private ServiceRequestHistoryRepository.TopContractedServiceProjection topServiceProjection(
            String label,
            long total) {
        return new ServiceRequestHistoryRepository.TopContractedServiceProjection() {
            @Override
            public String getLabel() {
                return label;
            }

            @Override
            public long getTotal() {
                return total;
            }
        };
    }
}
