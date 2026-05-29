package com.servija.servija.admin.wallet;

import com.servija.servija.model.Payment;
import com.servija.servija.model.PlatformWallet;
import com.servija.servija.repository.PaymentRepository;
import com.servija.servija.repository.PlatformWalletRepository;
import com.servija.servija.service.PlatformWalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlatformWalletServiceTest {

    @Mock
    private PlatformWalletRepository platformWalletRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @InjectMocks
    private PlatformWalletService platformWalletService;

    private PlatformWallet wallet;

    @BeforeEach
    void setUp() {
        wallet = PlatformWallet.builder()
                .id(1L)
                .availableBalance(new BigDecimal("100.00"))
                .reservedBalance(new BigDecimal("20.00"))
                .totalEarned(new BigDecimal("300.00"))
                .totalWithdrawn(new BigDecimal("180.00"))
                .build();
    }

    @Test
    @DisplayName("deve retornar carteira existente")
    void shouldReturnExistingWallet() {
        when(platformWalletRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.of(wallet));

        PlatformWallet result = platformWalletService.getOrCreateWallet();

        assertThat(result).isEqualTo(wallet);
        verify(platformWalletRepository, never()).save(any());
    }

    @Test
    @DisplayName("deve criar carteira zerada quando não existir")
    void shouldCreateWalletWhenMissing() {
        when(platformWalletRepository.findFirstByOrderByIdAsc()).thenReturn(Optional.empty());
        when(platformWalletRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        PlatformWallet result = platformWalletService.getOrCreateWallet();

        assertThat(result.getAvailableBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getReservedBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getTotalEarned()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getTotalWithdrawn()).isEqualByComparingTo(BigDecimal.ZERO);
        verify(platformWalletRepository).save(any());
    }

    @Test
    @DisplayName("deve ignorar pagamento nulo")
    void shouldIgnoreNullPayment() {
        boolean released = platformWalletService.releasePlatformFeeIfNeeded(null);

        assertThat(released).isFalse();
        verifyNoInteractions(paymentRepository, platformWalletRepository);
    }

    @Test
    @DisplayName("deve ignorar pagamento não concluído")
    void shouldIgnoreNotCompletedPayment() {
        Payment payment = Payment.builder()
                .id(1L)
                .status(Payment.PaymentStatus.PENDING)
                .platformFee(new BigDecimal("50.00"))
                .build();

        when(paymentRepository.lockById(1L)).thenReturn(Optional.of(payment));

        boolean released = platformWalletService.releasePlatformFeeIfNeeded(payment);

        assertThat(released).isFalse();
        verify(paymentRepository, never()).save(any());
        verify(platformWalletRepository, never()).save(any());
    }

    @Test
    @DisplayName("deve ignorar pagamento cuja taxa já foi liberada")
    void shouldIgnorePaymentWithAlreadyReleasedFee() {
        Payment payment = Payment.builder()
                .id(1L)
                .status(Payment.PaymentStatus.COMPLETED)
                .platformFee(new BigDecimal("50.00"))
                .platformFeeReleasedAt(LocalDateTime.of(2026, 5, 1, 10, 0))
                .build();

        when(paymentRepository.lockById(1L)).thenReturn(Optional.of(payment));

        boolean released = platformWalletService.releasePlatformFeeIfNeeded(payment);

        assertThat(released).isFalse();
        verify(paymentRepository, never()).save(any());
        verify(platformWalletRepository, never()).save(any());
    }

    @Test
    @DisplayName("deve marcar pagamento como liberado quando taxa for zero")
    void shouldMarkPaymentAsReleasedWhenFeeIsZero() {
        Payment payment = Payment.builder()
                .id(1L)
                .status(Payment.PaymentStatus.COMPLETED)
                .platformFee(BigDecimal.ZERO)
                .build();

        when(paymentRepository.lockById(1L)).thenReturn(Optional.of(payment));
        when(paymentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        boolean released = platformWalletService.releasePlatformFeeIfNeeded(payment);

        assertThat(released).isFalse();
        assertThat(payment.getPlatformFeeReleasedAt()).isNotNull();
        verify(paymentRepository).save(payment);
        verify(platformWalletRepository, never()).save(any());
    }

    @Test
    @DisplayName("deve creditar taxa da plataforma no saldo disponível")
    void shouldCreditReleasedPlatformFee() {
        Payment payment = Payment.builder()
                .id(1L)
                .status(Payment.PaymentStatus.COMPLETED)
                .platformFee(new BigDecimal("5.00"))
                .installmentFee(new BigDecimal("1.80"))
                .gatewayFee(new BigDecimal("1.99"))
                .build();

        when(paymentRepository.lockById(1L)).thenReturn(Optional.of(payment));
        when(platformWalletRepository.lockAllOrderByIdAsc()).thenReturn(List.of(wallet));
        when(platformWalletRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(paymentRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        boolean released = platformWalletService.releasePlatformFeeIfNeeded(payment);

        assertThat(released).isTrue();
        assertThat(wallet.getAvailableBalance()).isEqualByComparingTo(new BigDecimal("104.81"));
        assertThat(wallet.getTotalEarned()).isEqualByComparingTo(new BigDecimal("304.81"));
        assertThat(wallet.getReservedBalance()).isEqualByComparingTo(new BigDecimal("20.00"));
        assertThat(payment.getPlatformFeeReleasedAt()).isNotNull();
        verify(platformWalletRepository).save(wallet);
        verify(paymentRepository).save(payment);
    }
}
