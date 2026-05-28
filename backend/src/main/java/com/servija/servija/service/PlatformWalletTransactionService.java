package com.servija.servija.service;

import com.servija.servija.dto.PlatformWalletTransactionResponse;
import com.servija.servija.model.Payment;
import com.servija.servija.model.PlatformWithdrawalRequest;
import com.servija.servija.repository.PaymentRepository;
import com.servija.servija.repository.PlatformWithdrawalRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlatformWalletTransactionService {

    private final PaymentRepository paymentRepository;
    private final PlatformWithdrawalRequestRepository platformWithdrawalRequestRepository;

    @Transactional(readOnly = true)
    public List<PlatformWalletTransactionResponse> getTransactions(
            String typeFilter,
            LocalDate dateFrom,
            LocalDate dateTo
    ) {
        DateRange dateRange = DateRange.of(dateFrom, dateTo);
        List<PlatformWalletTransactionResponse> transactions = new ArrayList<>();

        if (typeFilter == null || typeFilter.equalsIgnoreCase("ALL") || typeFilter.equalsIgnoreCase("FEE")) {
            for (Payment payment : paymentRepository.findByPlatformFeeReleasedAtIsNotNullOrderByPlatformFeeReleasedAtDesc()) {
                LocalDateTime releasedAt = payment.getPlatformFeeReleasedAt();
                BigDecimal netPlatformFee = resolveNetPlatformFee(payment);
                if (!dateRange.includes(releasedAt)) {
                    continue;
                }
                if (netPlatformFee.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }

                String serviceName = payment.getServiceRequest() != null
                        ? payment.getServiceRequest().getServiceName()
                        : "Serviço";
                Long serviceRequestId = payment.getServiceRequest() != null
                        ? payment.getServiceRequest().getId()
                        : null;

                transactions.add(PlatformWalletTransactionResponse.builder()
                        .id(payment.getId())
                        .type("FEE")
                        .amount(netPlatformFee)
                        .status("COMPLETED")
                        .description("Receita liquida da plataforma liberada")
                        .paymentMethod(payment.getPaymentMethod())
                        .serviceName(serviceName)
                        .referenceLabel(serviceRequestId != null ? "Serviço #" + serviceRequestId : "Pagamento #" + payment.getId())
                        .date(releasedAt)
                        .build());
            }
        }

        if (typeFilter == null || typeFilter.equalsIgnoreCase("ALL") || typeFilter.equalsIgnoreCase("WITHDRAWAL")) {
            for (PlatformWithdrawalRequest withdrawal : platformWithdrawalRequestRepository.findAllByOrderByCreatedAtDesc()) {
                if (!dateRange.includes(withdrawal.getCreatedAt())) {
                    continue;
                }

                transactions.add(PlatformWalletTransactionResponse.builder()
                        .id(withdrawal.getId())
                        .type("WITHDRAWAL")
                        .amount(withdrawal.getAmount().negate())
                        .status(withdrawal.getStatus().name())
                        .description("Saque via " + withdrawal.getWithdrawalMethod().name())
                        .paymentMethod(withdrawal.getWithdrawalMethod().name())
                        .referenceLabel(withdrawal.getDestinationLabel())
                        .date(withdrawal.getCreatedAt())
                        .build());
            }
        }

        transactions.sort(Comparator.comparing(PlatformWalletTransactionResponse::getDate).reversed());
        return transactions;
    }

    private BigDecimal resolveNetPlatformFee(Payment payment) {
        BigDecimal platformFee = payment.getPlatformFee() != null ? payment.getPlatformFee() : BigDecimal.ZERO;
        BigDecimal gatewayFee = payment.getGatewayFee() != null ? payment.getGatewayFee() : BigDecimal.ZERO;
        return platformFee.subtract(gatewayFee);
    }

    private record DateRange(LocalDateTime startInclusive, LocalDateTime endExclusive) {
        private static DateRange of(LocalDate dateFrom, LocalDate dateTo) {
            if (dateFrom == null && dateTo == null) {
                return new DateRange(null, null);
            }

            LocalDate startDate = dateFrom;
            LocalDate endDate = dateTo;

            if (startDate == null) {
                startDate = endDate;
            }
            if (endDate == null) {
                endDate = startDate;
            }
            if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
                LocalDate temp = startDate;
                startDate = endDate;
                endDate = temp;
            }

            return new DateRange(
                    startDate != null ? startDate.atStartOfDay() : null,
                    endDate != null ? endDate.plusDays(1).atStartOfDay() : null
            );
        }

        private boolean includes(LocalDateTime value) {
            if (value == null) {
                return false;
            }
            if (startInclusive != null && value.isBefore(startInclusive)) {
                return false;
            }
            return endExclusive == null || value.isBefore(endExclusive);
        }
    }
}
