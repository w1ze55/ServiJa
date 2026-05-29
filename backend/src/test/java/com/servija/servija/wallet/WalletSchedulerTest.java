package com.servija.servija.wallet;

import com.servija.servija.model.Payment;
import com.servija.servija.model.ServiceRequest;
import com.servija.servija.model.ServiceRequestHistory.OperationType;
import com.servija.servija.model.User;
import com.servija.servija.repository.PaymentRepository;
import com.servija.servija.scheduler.WalletScheduler;
import com.servija.servija.service.FreelancerWalletService;
import com.servija.servija.service.PlatformWalletService;
import com.servija.servija.service.ServiceRequestHistoryService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletSchedulerTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private FreelancerWalletService walletService;

    @Mock
    private PlatformWalletService platformWalletService;

    @Mock
    private ServiceRequestHistoryService historyService;

    @InjectMocks
    private WalletScheduler walletScheduler;

    private User freelancer;

    @BeforeEach
    void setUp() {
        freelancer = User.builder()
                .id(1L)
                .email("freelancer@test.com")
                .name("João Silva")
                .build();
    }

    @Test
    @DisplayName("deve liberar saldo quando entrega tem mais de 7 dias sem aprovação")
    void shouldReleaseAfter7Days() {
        ServiceRequest sr = ServiceRequest.builder()
                .id(1L)
                .status(ServiceRequest.Status.DELIVERED)
                .updatedAt(LocalDateTime.now().minusDays(8)) // 8 dias atrás
                .build();

        Payment payment = Payment.builder()
                .id(1L)
                .status(Payment.PaymentStatus.COMPLETED)
                .serviceRequest(sr)
                .freelancer(freelancer)
                .amount(new BigDecimal("950.00"))
                .build();

        when(paymentRepository.findAll()).thenReturn(List.of(payment));

        walletScheduler.autoReleaseExpiredPendingBalances();

        verify(walletService).releasePendingToAvailable("freelancer@test.com", new BigDecimal("950.00"));
        verify(platformWalletService).releasePlatformFeeIfNeeded(payment);
        verify(historyService).record(
                sr,
                freelancer,
                OperationType.SERVICE_COMPLETED,
                "Servico concluido automaticamente apos 7 dias sem aprovacao do cliente");
        assertThat(sr.getStatus()).isEqualTo(ServiceRequest.Status.COMPLETED);
    }

    @Test
    @DisplayName("não deve liberar saldo se entrega tem menos de 7 dias")
    void shouldNotReleaseBeforeDeadline() {
        ServiceRequest sr = ServiceRequest.builder()
                .id(1L)
                .status(ServiceRequest.Status.DELIVERED)
                .updatedAt(LocalDateTime.now().minusDays(3)) // 3 dias atrás
                .build();

        Payment payment = Payment.builder()
                .id(1L)
                .status(Payment.PaymentStatus.COMPLETED)
                .serviceRequest(sr)
                .freelancer(freelancer)
                .amount(new BigDecimal("950.00"))
                .build();

        when(paymentRepository.findAll()).thenReturn(List.of(payment));

        walletScheduler.autoReleaseExpiredPendingBalances();

        verify(walletService, never()).releasePendingToAvailable(any(), any());
    }

    @Test
    @DisplayName("não deve liberar saldo se serviço não foi entregue")
    void shouldNotReleaseIfNotDelivered() {
        ServiceRequest sr = ServiceRequest.builder()
                .id(1L)
                .status(ServiceRequest.Status.IN_PROGRESS) // Não entregue
                .updatedAt(LocalDateTime.now().minusDays(10))
                .build();

        Payment payment = Payment.builder()
                .id(1L)
                .status(Payment.PaymentStatus.COMPLETED)
                .serviceRequest(sr)
                .freelancer(freelancer)
                .amount(new BigDecimal("950.00"))
                .build();

        when(paymentRepository.findAll()).thenReturn(List.of(payment));

        walletScheduler.autoReleaseExpiredPendingBalances();

        verify(walletService, never()).releasePendingToAvailable(any(), any());
    }

    @Test
    @DisplayName("não deve liberar saldo se serviço já foi completado")
    void shouldNotReleaseIfAlreadyCompleted() {
        ServiceRequest sr = ServiceRequest.builder()
                .id(1L)
                .status(ServiceRequest.Status.COMPLETED) // Já liberado
                .updatedAt(LocalDateTime.now().minusDays(10))
                .build();

        Payment payment = Payment.builder()
                .id(1L)
                .status(Payment.PaymentStatus.COMPLETED)
                .serviceRequest(sr)
                .freelancer(freelancer)
                .amount(new BigDecimal("950.00"))
                .build();

        when(paymentRepository.findAll()).thenReturn(List.of(payment));

        walletScheduler.autoReleaseExpiredPendingBalances();

        verify(walletService, never()).releasePendingToAvailable(any(), any());
    }

    @Test
    @DisplayName("não deve liberar se pagamento não está completo")
    void shouldNotReleaseIfPaymentNotCompleted() {
        ServiceRequest sr = ServiceRequest.builder()
                .id(1L)
                .status(ServiceRequest.Status.DELIVERED)
                .updatedAt(LocalDateTime.now().minusDays(10))
                .build();

        Payment payment = Payment.builder()
                .id(1L)
                .status(Payment.PaymentStatus.PENDING) // Não confirmado
                .serviceRequest(sr)
                .freelancer(freelancer)
                .amount(new BigDecimal("950.00"))
                .build();

        when(paymentRepository.findAll()).thenReturn(List.of(payment));

        walletScheduler.autoReleaseExpiredPendingBalances();

        verify(walletService, never()).releasePendingToAvailable(any(), any());
    }
}
