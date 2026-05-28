package com.servija.servija.admin.wallet;

import com.servija.servija.dto.PlatformWalletTransactionResponse;
import com.servija.servija.model.Payment;
import com.servija.servija.model.PlatformWithdrawalRequest;
import com.servija.servija.repository.PaymentRepository;
import com.servija.servija.repository.PlatformWithdrawalRequestRepository;
import com.servija.servija.service.PlatformWalletTransactionService;
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
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlatformWalletTransactionServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PlatformWithdrawalRequestRepository platformWithdrawalRequestRepository;

    @InjectMocks
    private PlatformWalletTransactionService platformWalletTransactionService;

    @Test
    @DisplayName("deve unir taxas e saques, ordenar por data e aplicar fallbacks")
    void shouldMergeSortAndApplyFallbacks() {
        Payment payment = Payment.builder()
                .id(1L)
                .platformFee(new BigDecimal("25.00"))
                .paymentMethod("PIX")
                .platformFeeReleasedAt(LocalDateTime.of(2026, 5, 10, 10, 0))
                .build();

        PlatformWithdrawalRequest withdrawal = PlatformWithdrawalRequest.builder()
                .id(2L)
                .amount(new BigDecimal("200.00"))
                .status(PlatformWithdrawalRequest.WithdrawalStatus.COMPLETED)
                .withdrawalMethod(PlatformWithdrawalRequest.WithdrawalMethod.TED)
                .destinationLabel("TED - Banco do Brasil - Ag * - **7890")
                .createdAt(LocalDateTime.of(2026, 5, 12, 9, 0))
                .build();

        when(paymentRepository.findByPlatformFeeReleasedAtIsNotNullOrderByPlatformFeeReleasedAtDesc())
                .thenReturn(List.of(payment));
        when(platformWithdrawalRequestRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(withdrawal));

        List<PlatformWalletTransactionResponse> result =
                platformWalletTransactionService.getTransactions("ALL", null, null);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getType()).isEqualTo("WITHDRAWAL");
        assertThat(result.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("-200.00"));
        assertThat(result.get(1).getType()).isEqualTo("FEE");
        assertThat(result.get(1).getReferenceLabel()).isEqualTo("Pagamento #1");
        assertThat(result.get(1).getServiceName()).contains("Serv");
    }

    @Test
    @DisplayName("deve filtrar taxas por período mesmo com datas invertidas")
    void shouldFilterFeesWithReversedDates() {
        Payment inside = Payment.builder()
                .id(1L)
                .platformFee(new BigDecimal("15.00"))
                .paymentMethod("CARD")
                .platformFeeReleasedAt(LocalDateTime.of(2026, 5, 10, 10, 0))
                .build();

        Payment outside = Payment.builder()
                .id(2L)
                .platformFee(new BigDecimal("20.00"))
                .paymentMethod("PIX")
                .platformFeeReleasedAt(LocalDateTime.of(2026, 5, 20, 10, 0))
                .build();

        when(paymentRepository.findByPlatformFeeReleasedAtIsNotNullOrderByPlatformFeeReleasedAtDesc())
                .thenReturn(List.of(outside, inside));

        List<PlatformWalletTransactionResponse> result =
                platformWalletTransactionService.getTransactions(
                        "FEE",
                        LocalDate.of(2026, 5, 15),
                        LocalDate.of(2026, 5, 9)
                );

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getType()).isEqualTo("FEE");
    }

    @Test
    @DisplayName("deve retornar apenas saques quando filtro for WITHDRAWAL")
    void shouldReturnOnlyWithdrawals() {
        PlatformWithdrawalRequest first = PlatformWithdrawalRequest.builder()
                .id(3L)
                .amount(new BigDecimal("80.00"))
                .status(PlatformWithdrawalRequest.WithdrawalStatus.COMPLETED)
                .withdrawalMethod(PlatformWithdrawalRequest.WithdrawalMethod.PIX)
                .destinationLabel("PIX - CPF - **900")
                .createdAt(LocalDateTime.of(2026, 5, 11, 8, 0))
                .build();

        PlatformWithdrawalRequest second = PlatformWithdrawalRequest.builder()
                .id(4L)
                .amount(new BigDecimal("45.00"))
                .status(PlatformWithdrawalRequest.WithdrawalStatus.FAILED)
                .withdrawalMethod(PlatformWithdrawalRequest.WithdrawalMethod.TED)
                .destinationLabel("TED - Banco do Brasil - Ag * - **7890")
                .createdAt(LocalDateTime.of(2026, 5, 9, 8, 0))
                .build();

        when(platformWithdrawalRequestRepository.findAllByOrderByCreatedAtDesc())
                .thenReturn(List.of(first, second));

        List<PlatformWalletTransactionResponse> result =
                platformWalletTransactionService.getTransactions("WITHDRAWAL", null, null);

        assertThat(result).hasSize(2);
        assertThat(result).allSatisfy(tx -> assertThat(tx.getType()).isEqualTo("WITHDRAWAL"));
        assertThat(result.get(0).getAmount()).isEqualByComparingTo(new BigDecimal("-80.00"));
        verifyNoInteractions(paymentRepository);
    }
}
