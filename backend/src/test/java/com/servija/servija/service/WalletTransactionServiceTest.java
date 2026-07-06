package com.servija.servija.service;

import com.servija.servija.dto.WalletTransactionResponse;
import com.servija.servija.exception.ResourceNotFoundException;
import com.servija.servija.model.Payment;
import com.servija.servija.model.ServiceRequest;
import com.servija.servija.model.User;
import com.servija.servija.model.WithdrawalRequest;
import com.servija.servija.repository.PaymentRepository;
import com.servija.servija.repository.UserRepository;
import com.servija.servija.repository.WithdrawalRequestRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletTransactionServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private WithdrawalRequestRepository withdrawalRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WalletTransactionService service;

    @Test
    @DisplayName("getTransactions unifica recebimentos e saques em ordem decrescente")
    void getTransactionsMergesAndSortsTransactions() {
        User user = user();
        Payment payment = Payment.builder()
                .id(1L)
                .freelancer(user)
                .contractor(User.builder().name("Cliente").build())
                .amount(new BigDecimal("120.00"))
                .status(Payment.PaymentStatus.COMPLETED)
                .paymentMethod("PIX")
                .createdAt(LocalDateTime.of(2026, 1, 10, 10, 0))
                .completedAt(LocalDateTime.of(2026, 1, 11, 11, 0))
                .serviceRequest(ServiceRequest.builder().serviceName("Logo").build())
                .build();
        WithdrawalRequest withdrawal = WithdrawalRequest.builder()
                .id(2L)
                .amount(new BigDecimal("40.00"))
                .status(WithdrawalRequest.WithdrawalStatus.PENDING)
                .withdrawalMethod(WithdrawalRequest.WithdrawalMethod.PIX)
                .createdAt(LocalDateTime.of(2026, 1, 12, 12, 0))
                .build();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(paymentRepository.findByFreelancer(user)).thenReturn(List.of(payment));
        when(withdrawalRepository.findByFreelancerIdOrderByCreatedAtDesc(user.getId())).thenReturn(List.of(withdrawal));

        List<WalletTransactionResponse> result = service.getTransactions(user.getEmail(), "ALL");

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getType()).isEqualTo("WITHDRAWAL");
        assertThat(result.get(0).getAmount()).isEqualByComparingTo("-40.00");
        assertThat(result.get(1).getType()).isEqualTo("EARNING");
        assertThat(result.get(1).getDescription()).isEqualTo("Logo");
        assertThat(result.get(1).getClientName()).isEqualTo("Cliente");
    }

    @Test
    @DisplayName("getTransactions aplica filtro por tipo")
    void getTransactionsAppliesTypeFilter() {
        User user = user();
        WithdrawalRequest withdrawal = WithdrawalRequest.builder()
                .id(2L)
                .amount(new BigDecimal("40.00"))
                .status(WithdrawalRequest.WithdrawalStatus.COMPLETED)
                .withdrawalMethod(WithdrawalRequest.WithdrawalMethod.TED)
                .createdAt(LocalDateTime.of(2026, 1, 12, 12, 0))
                .build();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(withdrawalRepository.findByFreelancerIdOrderByCreatedAtDesc(user.getId())).thenReturn(List.of(withdrawal));

        List<WalletTransactionResponse> result = service.getTransactions(user.getEmail(), "WITHDRAWAL");

        assertThat(result).singleElement()
                .satisfies(tx -> {
                    assertThat(tx.getType()).isEqualTo("WITHDRAWAL");
                    assertThat(tx.getPaymentMethod()).isEqualTo("TED");
                });
    }

    @Test
    @DisplayName("getTransactions falha quando o usuário não existe")
    void getTransactionsFailsWhenUserDoesNotExist() {
        when(userRepository.findByEmail("missing@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getTransactions("missing@test.com", "ALL"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("não encontrado");
    }

    private User user() {
        return User.builder()
                .id(1L)
                .email("freela@test.com")
                .name("Freela")
                .cpf("123")
                .phone("999")
                .password("pwd")
                .userType(User.UserType.FREELANCER)
                .activeRole(User.UserType.FREELANCER)
                .build();
    }
}
