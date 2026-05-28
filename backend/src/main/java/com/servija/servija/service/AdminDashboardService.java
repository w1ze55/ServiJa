package com.servija.servija.service;

import com.servija.servija.dto.AdminDashboardCountMetricDto;
import com.servija.servija.dto.AdminDashboardMoneyMetricDto;
import com.servija.servija.dto.AdminDashboardPaymentStatusDto;
import com.servija.servija.dto.AdminDashboardPeriodDto;
import com.servija.servija.dto.AdminDashboardRateMetricDto;
import com.servija.servija.dto.AdminDashboardResponse;
import com.servija.servija.dto.AdminDashboardRevenuePointDto;
import com.servija.servija.dto.AdminDashboardSummaryDto;
import com.servija.servija.dto.AdminDashboardTopContractedServiceDto;
import com.servija.servija.model.Payment;
import com.servija.servija.model.ServiceRequestHistory;
import com.servija.servija.model.User;
import com.servija.servija.repository.PaymentRepository;
import com.servija.servija.repository.ServiceRequestHistoryRepository;
import com.servija.servija.repository.ServiceRequestRepository;
import com.servija.servija.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final List<User.UserType> BUSINESS_USER_TYPES =
            List.of(User.UserType.CONTRACTOR, User.UserType.FREELANCER);
    private static final List<ServiceRequestHistory.OperationType> CONTRACTED_OPERATION_TYPES =
            List.of(
                    ServiceRequestHistory.OperationType.BUDGET_ACCEPTED,
                    ServiceRequestHistory.OperationType.COUNTER_PROPOSAL_ACCEPTED);
    private static final int TOP_SERVICES_LIMIT = 5;

    private final UserRepository userRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final PaymentRepository paymentRepository;
    private final ServiceRequestHistoryRepository serviceRequestHistoryRepository;

    public AdminDashboardResponse getDashboard(LocalDate dateFrom, LocalDate dateTo) {
        PeriodRange currentRange = resolveCurrentRange(dateFrom, dateTo);
        PeriodRange previousRange = currentRange.previousRange();

        long currentNewUsers = userRepository.countByUserTypeInAndCreatedAtBetween(
                BUSINESS_USER_TYPES, currentRange.startDateTime(), currentRange.endExclusiveDateTime());
        long previousNewUsers = userRepository.countByUserTypeInAndCreatedAtBetween(
                BUSINESS_USER_TYPES, previousRange.startDateTime(), previousRange.endExclusiveDateTime());

        long currentCompletedServices = serviceRequestHistoryRepository
                .countDistinctServiceRequestsByOperationTypeAndCreatedAtBetween(
                        ServiceRequestHistory.OperationType.SERVICE_COMPLETED,
                        currentRange.startDateTime(),
                        currentRange.endExclusiveDateTime());
        long previousCompletedServices = serviceRequestHistoryRepository
                .countDistinctServiceRequestsByOperationTypeAndCreatedAtBetween(
                        ServiceRequestHistory.OperationType.SERVICE_COMPLETED,
                        previousRange.startDateTime(),
                        previousRange.endExclusiveDateTime());

        BigDecimal currentRevenue = defaultMoney(serviceRequestHistoryRepository
                .sumCompletedRevenueByOperationTypeAndCreatedAtBetween(
                        ServiceRequestHistory.OperationType.SERVICE_COMPLETED,
                        currentRange.startDateTime(),
                        currentRange.endExclusiveDateTime()));
        BigDecimal previousRevenue = defaultMoney(serviceRequestHistoryRepository
                .sumCompletedRevenueByOperationTypeAndCreatedAtBetween(
                        ServiceRequestHistory.OperationType.SERVICE_COMPLETED,
                        previousRange.startDateTime(),
                        previousRange.endExclusiveDateTime()));

        BigDecimal currentServiJaFee = defaultMoney(paymentRepository
                .sumNetPlatformRevenueReleasedBetween(
                        currentRange.startDateTime(),
                        currentRange.endExclusiveDateTime()));
        BigDecimal previousServiJaFee = defaultMoney(paymentRepository
                .sumNetPlatformRevenueReleasedBetween(
                        previousRange.startDateTime(),
                        previousRange.endExclusiveDateTime()));

        BigDecimal currentAverageTicket = calculateAverageTicket(currentRevenue, currentCompletedServices);
        BigDecimal previousAverageTicket = calculateAverageTicket(previousRevenue, previousCompletedServices);

        long currentRequests = serviceRequestRepository.countCreatedBetween(
                currentRange.startDateTime(), currentRange.endExclusiveDateTime());
        long previousRequests = serviceRequestRepository.countCreatedBetween(
                previousRange.startDateTime(), previousRange.endExclusiveDateTime());

        long currentActiveClients = serviceRequestRepository.countDistinctActiveContractorsBetween(
                currentRange.startDateTime(), currentRange.endExclusiveDateTime());
        long previousActiveClients = serviceRequestRepository.countDistinctActiveContractorsBetween(
                previousRange.startDateTime(), previousRange.endExclusiveDateTime());

        long currentActiveProfessionals = serviceRequestRepository.countDistinctActiveFreelancersBetween(
                currentRange.startDateTime(), currentRange.endExclusiveDateTime());
        long previousActiveProfessionals = serviceRequestRepository.countDistinctActiveFreelancersBetween(
                previousRange.startDateTime(), previousRange.endExclusiveDateTime());

        BigDecimal currentConversionRate = calculateRate(currentCompletedServices, currentRequests);
        BigDecimal previousConversionRate = calculateRate(previousCompletedServices, previousRequests);

        return AdminDashboardResponse.builder()
                .period(AdminDashboardPeriodDto.builder()
                        .startDate(currentRange.startDate())
                        .endDate(currentRange.endDate())
                        .previousStartDate(previousRange.startDate())
                        .previousEndDate(previousRange.endDate())
                        .granularity(currentRange.dayCount() <= 45 ? "DAILY" : "MONTHLY")
                        .allTime(currentRange.allTime())
                        .build())
                .summary(AdminDashboardSummaryDto.builder()
                        .totalRevenue(AdminDashboardMoneyMetricDto.builder()
                                .current(currentRevenue)
                                .previous(previousRevenue)
                                .changePercent(calculateChange(currentRevenue, previousRevenue))
                                .build())
                        .serviJaFee(AdminDashboardMoneyMetricDto.builder()
                                .current(currentServiJaFee)
                                .previous(previousServiJaFee)
                                .changePercent(calculateChange(currentServiJaFee, previousServiJaFee))
                                .build())
                        .newUsers(AdminDashboardCountMetricDto.builder()
                                .current(currentNewUsers)
                                .previous(previousNewUsers)
                                .changePercent(calculateChange(currentNewUsers, previousNewUsers))
                                .build())
                        .completedServices(AdminDashboardCountMetricDto.builder()
                                .current(currentCompletedServices)
                                .previous(previousCompletedServices)
                                .changePercent(calculateChange(currentCompletedServices, previousCompletedServices))
                                .build())
                        .averageTicket(AdminDashboardMoneyMetricDto.builder()
                                .current(currentAverageTicket)
                                .previous(previousAverageTicket)
                                .changePercent(calculateChange(currentAverageTicket, previousAverageTicket))
                                .build())
                        .activeClients(AdminDashboardCountMetricDto.builder()
                                .current(currentActiveClients)
                                .previous(previousActiveClients)
                                .changePercent(calculateChange(currentActiveClients, previousActiveClients))
                                .build())
                        .activeProfessionals(AdminDashboardCountMetricDto.builder()
                                .current(currentActiveProfessionals)
                                .previous(previousActiveProfessionals)
                                .changePercent(calculateChange(currentActiveProfessionals, previousActiveProfessionals))
                                .build())
                        .conversionRate(AdminDashboardRateMetricDto.builder()
                                .current(currentConversionRate)
                                .previous(previousConversionRate)
                                .changePercent(calculateChange(currentConversionRate, previousConversionRate))
                                .currentCompletedCount(currentCompletedServices)
                                .currentRequestCount(currentRequests)
                                .previousCompletedCount(previousCompletedServices)
                                .previousRequestCount(previousRequests)
                                .build())
                        .build())
                .paymentStatusBreakdown(buildPaymentStatusBreakdown(currentRange))
                .revenueSeries(buildRevenueSeries(currentRange))
                .topContractedServices(buildTopContractedServices(currentRange))
                .build();
    }

    private PeriodRange resolveCurrentRange(LocalDate dateFrom, LocalDate dateTo) {
        LocalDate today = LocalDate.now();
        boolean allTime = dateFrom == null && dateTo == null;

        LocalDate resolvedStart = allTime ? resolveAllTimeStartDate() : (dateFrom != null ? dateFrom : resolveAllTimeStartDate());
        LocalDate resolvedEnd = dateTo != null ? dateTo : today;

        if (resolvedStart.isAfter(resolvedEnd)) {
            LocalDate swappedStart = resolvedEnd;
            LocalDate swappedEnd = resolvedStart;
            resolvedStart = swappedStart;
            resolvedEnd = swappedEnd;
        }

        return new PeriodRange(resolvedStart, resolvedEnd, allTime);
    }

    private LocalDate resolveAllTimeStartDate() {
        List<LocalDateTime> candidates = new ArrayList<>();
        candidates.add(userRepository.findEarliestCreatedAtByUserTypeIn(BUSINESS_USER_TYPES));
        candidates.add(serviceRequestRepository.findEarliestCreatedAt());
        candidates.add(paymentRepository.findEarliestCreatedAt());

        return candidates.stream()
                .filter(value -> value != null)
                .min(Comparator.naturalOrder())
                .map(LocalDateTime::toLocalDate)
                .orElse(LocalDate.now());
    }

    private List<AdminDashboardPaymentStatusDto> buildPaymentStatusBreakdown(PeriodRange range) {
        Map<String, Long> totals = new LinkedHashMap<>();
        totals.put("COMPLETED", 0L);
        totals.put("PENDING", 0L);
        totals.put("CANCELLED", 0L);
        totals.put("REFUNDED", 0L);
        totals.put("OTHERS", 0L);

        List<PaymentRepository.PaymentStatusCountProjection> rawCounts =
                paymentRepository.countStatusesCreatedBetween(range.startDateTime(), range.endExclusiveDateTime());

        for (PaymentRepository.PaymentStatusCountProjection rawCount : rawCounts) {
            String key = mapPaymentStatus(rawCount.getStatus());
            totals.computeIfPresent(key, (ignored, current) -> current + rawCount.getTotal());
        }

        List<AdminDashboardPaymentStatusDto> items = new ArrayList<>();
        items.add(toPaymentStatusDto("COMPLETED", "Concluídos", totals.get("COMPLETED")));
        items.add(toPaymentStatusDto("PENDING", "Pendentes", totals.get("PENDING")));
        items.add(toPaymentStatusDto("CANCELLED", "Cancelados", totals.get("CANCELLED")));
        items.add(toPaymentStatusDto("REFUNDED", "Reembolsados", totals.get("REFUNDED")));

        if (totals.get("OTHERS") > 0) {
            items.add(toPaymentStatusDto("OTHERS", "Outros", totals.get("OTHERS")));
        }

        return items;
    }

    private AdminDashboardPaymentStatusDto toPaymentStatusDto(String key, String label, long count) {
        return AdminDashboardPaymentStatusDto.builder()
                .key(key)
                .label(label)
                .count(count)
                .build();
    }

    private String mapPaymentStatus(Payment.PaymentStatus status) {
        return switch (status) {
            case COMPLETED -> "COMPLETED";
            case PENDING, PROCESSING, OVERDUE -> "PENDING";
            case CANCELLED -> "CANCELLED";
            case REFUNDED -> "REFUNDED";
            case FAILED, CHARGEBACK -> "OTHERS";
        };
    }

    private List<AdminDashboardRevenuePointDto> buildRevenueSeries(PeriodRange range) {
        boolean useDailyBuckets = range.dayCount() <= 45;
        Map<LocalDate, BigDecimal> totalsByBucket = new LinkedHashMap<>();

        if (useDailyBuckets) {
            for (long dayOffset = 0; dayOffset < range.dayCount(); dayOffset++) {
                LocalDate bucketDate = range.startDate().plusDays(dayOffset);
                totalsByBucket.put(bucketDate, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
            }

            for (ServiceRequestHistoryRepository.RevenueByDayProjection projection :
                    serviceRequestHistoryRepository.findRevenueSeriesByDay(
                            ServiceRequestHistory.OperationType.SERVICE_COMPLETED.name(),
                            range.startDateTime(),
                            range.endExclusiveDateTime())) {
                LocalDate bucketDate = projection.getBucketDate();
                totalsByBucket.put(bucketDate, defaultMoney(projection.getRevenue()));
            }
        } else {
            LocalDate cursor = range.startDate().withDayOfMonth(1);
            LocalDate lastBucket = range.endDate().withDayOfMonth(1);
            while (!cursor.isAfter(lastBucket)) {
                totalsByBucket.put(cursor, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
                cursor = cursor.plusMonths(1);
            }

            for (ServiceRequestHistoryRepository.RevenueByMonthProjection projection :
                    serviceRequestHistoryRepository.findRevenueSeriesByMonth(
                            ServiceRequestHistory.OperationType.SERVICE_COMPLETED.name(),
                            range.startDateTime(),
                            range.endExclusiveDateTime())) {
                LocalDate bucketDate = LocalDate.parse(projection.getBucketMonth());
                totalsByBucket.put(bucketDate, defaultMoney(projection.getRevenue()));
            }
        }

        boolean spansMultipleYears = range.startDate().getYear() != range.endDate().getYear();
        DateTimeFormatter dayFormatter = DateTimeFormatter.ofPattern("dd/MM");
        DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern(
                spansMultipleYears ? "MMM/yy" : "MMM", PT_BR);

        return totalsByBucket.entrySet().stream()
                .map(entry -> AdminDashboardRevenuePointDto.builder()
                        .key(entry.getKey().toString())
                        .label(useDailyBuckets
                                ? formatDailyLabel(entry.getKey(), range.dayCount(), dayFormatter)
                                : entry.getKey().format(monthFormatter))
                        .amount(defaultMoney(entry.getValue()))
                        .build())
                .toList();
    }

    private String formatDailyLabel(LocalDate date, long dayCount, DateTimeFormatter formatter) {
        if (dayCount <= 7) {
            return date.getDayOfWeek().getDisplayName(TextStyle.SHORT, PT_BR);
        }
        return date.format(formatter);
    }

    private List<AdminDashboardTopContractedServiceDto> buildTopContractedServices(PeriodRange range) {
        return serviceRequestHistoryRepository
                .findTopContractedServicesByOperationTypeInAndCreatedAtBetween(
                        CONTRACTED_OPERATION_TYPES,
                        range.startDateTime(),
                        range.endExclusiveDateTime())
                .stream()
                .limit(TOP_SERVICES_LIMIT)
                .map(item -> AdminDashboardTopContractedServiceDto.builder()
                        .label(item.getLabel())
                        .contractsCount(item.getTotal())
                        .build())
                .toList();
    }

    private BigDecimal calculateAverageTicket(BigDecimal revenue, long completedServices) {
        if (completedServices <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }

        return revenue.divide(BigDecimal.valueOf(completedServices), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateRate(long numerator, long denominator) {
        if (denominator <= 0) {
            return BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }

        return BigDecimal.valueOf(numerator)
                .multiply(ONE_HUNDRED)
                .divide(BigDecimal.valueOf(denominator), 1, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateChange(long current, long previous) {
        if (previous == 0L) {
            return current > 0L
                    ? ONE_HUNDRED.setScale(1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }

        return BigDecimal.valueOf(current - previous)
                .multiply(ONE_HUNDRED)
                .divide(BigDecimal.valueOf(previous), 1, RoundingMode.HALF_UP);
    }

    private BigDecimal calculateChange(BigDecimal current, BigDecimal previous) {
        BigDecimal safeCurrent = defaultMoney(current);
        BigDecimal safePrevious = defaultMoney(previous);

        if (safePrevious.compareTo(BigDecimal.ZERO) == 0) {
            return safeCurrent.compareTo(BigDecimal.ZERO) > 0
                    ? ONE_HUNDRED.setScale(1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }

        return safeCurrent.subtract(safePrevious)
                .multiply(ONE_HUNDRED)
                .divide(safePrevious, 1, RoundingMode.HALF_UP);
    }

    private BigDecimal defaultMoney(BigDecimal value) {
        return normalizeMoney(value == null ? BigDecimal.ZERO : value);
    }

    private BigDecimal normalizeMoney(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    private record PeriodRange(LocalDate startDate, LocalDate endDate, boolean allTime) {
        private long dayCount() {
            return ChronoUnit.DAYS.between(startDate, endDate) + 1;
        }

        private LocalDateTime startDateTime() {
            return startDate.atStartOfDay();
        }

        private LocalDateTime endExclusiveDateTime() {
            return endDate.plusDays(1).atStartOfDay();
        }

        private PeriodRange previousRange() {
            long days = dayCount();
            LocalDate previousEnd = startDate.minusDays(1);
            LocalDate previousStart = previousEnd.minusDays(days - 1);
            return new PeriodRange(previousStart, previousEnd, false);
        }
    }
}
