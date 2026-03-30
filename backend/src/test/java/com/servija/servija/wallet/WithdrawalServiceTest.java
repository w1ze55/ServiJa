package com.servija.servija.wallet;

import com.servija.servija.exception.ResourceNotFoundException;
import com.servija.servija.model.User;
import com.servija.servija.model.WithdrawalRequest;
import com.servija.servija.repository.UserRepository;
import com.servija.servija.repository.WithdrawalRequestRepository;
import com.servija.servija.service.AsaasService;
import com.servija.servija.service.FreelancerWalletService;
import com.servija.servija.service.WithdrawalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WithdrawalServiceTest {

    @Mock
    private WithdrawalRequestRepository withdrawalRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FreelancerWalletService walletService;

    @Mock
    private AsaasService asaasService;

    @InjectMocks
    private WithdrawalService withdrawalService;

    private User freelancer;

    @BeforeEach
    void setUp() {
        freelancer = User.builder()
                .id(1L)
                .email("freelancer@test.com")
                .name("João Silva")
                .build();
    }

    @Nested
    @DisplayName("requestWithdrawal")
    class RequestWithdrawal {

        @Test
        @DisplayName("deve criar saque via PIX com sucesso")
        void shouldCreatePixWithdrawal() {
            when(userRepository.findByEmail("freelancer@test.com")).thenReturn(Optional.of(freelancer));
            when(walletService.hasSufficientBalance(freelancer, new BigDecimal("500.00"))).thenReturn(true);
            when(asaasService.createPixTransfer(any(), any(), any(), any()))
                    .thenReturn(Map.of("id", "tr_123", "status", "PENDING"));
            when(withdrawalRepository.save(any())).thenAnswer(inv -> {
                WithdrawalRequest w = inv.getArgument(0);
                w.setId(1L);
                return w;
            });

            WithdrawalRequest result = withdrawalService.requestWithdrawal(
                    "freelancer@test.com",
                    new BigDecimal("500.00"),
                    "12345678900",
                    "CPF"
            );

            assertThat(result).isNotNull();
            assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
            verify(walletService).deductAvailableBalance(freelancer, new BigDecimal("500.00"));
        }

        @Test
        @DisplayName("deve rejeitar saque com saldo insuficiente")
        void shouldRejectInsufficientBalance() {
            when(userRepository.findByEmail("freelancer@test.com")).thenReturn(Optional.of(freelancer));
            when(walletService.hasSufficientBalance(freelancer, new BigDecimal("2000.00"))).thenReturn(false);

            assertThatThrownBy(() -> withdrawalService.requestWithdrawal(
                    "freelancer@test.com",
                    new BigDecimal("2000.00"),
                    "12345678900",
                    "CPF"
            )).isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("deve rejeitar usuário não encontrado")
        void shouldRejectUserNotFound() {
            when(userRepository.findByEmail("nope@test.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> withdrawalService.requestWithdrawal(
                    "nope@test.com",
                    new BigDecimal("100.00"),
                    "key",
                    "CPF"
            )).isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("cancelWithdrawal")
    class CancelWithdrawal {

        @Test
        @DisplayName("deve cancelar saque pendente")
        void shouldCancelPendingWithdrawal() {
            WithdrawalRequest withdrawal = WithdrawalRequest.builder()
                    .id(1L)
                    .freelancer(freelancer)
                    .amount(new BigDecimal("500.00"))
                    .status(WithdrawalRequest.WithdrawalStatus.PENDING)
                    .build();

            when(withdrawalRepository.findById(1L)).thenReturn(Optional.of(withdrawal));

            withdrawalService.cancelWithdrawal(1L, "freelancer@test.com");

            assertThat(withdrawal.getStatus()).isEqualTo(WithdrawalRequest.WithdrawalStatus.CANCELLED);
            verify(withdrawalRepository).save(withdrawal);
        }

        @Test
        @DisplayName("deve rejeitar cancelamento de saque não-pendente")
        void shouldRejectCancelNonPending() {
            WithdrawalRequest withdrawal = WithdrawalRequest.builder()
                    .id(1L)
                    .freelancer(freelancer)
                    .amount(new BigDecimal("500.00"))
                    .status(WithdrawalRequest.WithdrawalStatus.COMPLETED)
                    .build();

            when(withdrawalRepository.findById(1L)).thenReturn(Optional.of(withdrawal));

            assertThatThrownBy(() -> withdrawalService.cancelWithdrawal(1L, "freelancer@test.com"))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("getMyWithdrawals")
    class GetMyWithdrawals {

        @Test
        @DisplayName("deve listar saques do freelancer")
        void shouldListWithdrawals() {
            when(userRepository.findByEmail("freelancer@test.com")).thenReturn(Optional.of(freelancer));
            when(withdrawalRepository.findByFreelancerIdOrderByCreatedAtDesc(1L))
                    .thenReturn(List.of(
                            WithdrawalRequest.builder().id(1L).amount(new BigDecimal("500.00")).build(),
                            WithdrawalRequest.builder().id(2L).amount(new BigDecimal("300.00")).build()
                    ));

            List<WithdrawalRequest> result = withdrawalService.getMyWithdrawals("freelancer@test.com");

            assertThat(result).hasSize(2);
        }
    }
}
