package com.servija.servija.wallet;

import com.servija.servija.exception.ResourceNotFoundException;
import com.servija.servija.model.FreelancerBankAccount;
import com.servija.servija.model.User;
import com.servija.servija.model.WithdrawalRequest;
import com.servija.servija.repository.UserRepository;
import com.servija.servija.repository.WithdrawalRequestRepository;
import com.servija.servija.service.AsaasService;
import com.servija.servija.service.AuditLogger;
import com.servija.servija.service.BankAccountService;
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
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Mock
    private BankAccountService bankAccountService;

    @Mock
    private AuditLogger auditLogger;

    @InjectMocks
    private WithdrawalService withdrawalService;

    private User freelancer;
    private FreelancerBankAccount tedAccount;

    @BeforeEach
    void setUp() {
        freelancer = User.builder()
                .id(1L)
                .email("freelancer@test.com")
                .name("Joao Silva")
                .build();

        tedAccount = FreelancerBankAccount.builder()
                .id(10L)
                .user(freelancer)
                .accountType(FreelancerBankAccount.AccountType.BANK_ACCOUNT)
                .bankName("Banco Teste")
                .bankCode("001")
                .agency("1234")
                .accountNumber("12345")
                .accountDigit("6")
                .accountBankType(FreelancerBankAccount.BankAccountType.CONTA_CORRENTE)
                .holderName("Joao Silva")
                .holderCpfCnpj("12345678900")
                .build();
    }

    @Nested
    @DisplayName("requestWithdrawal")
    class RequestWithdrawal {

        @Test
        @DisplayName("deve criar saque via PIX com sucesso")
        void shouldCreatePixWithdrawal() {
            AtomicReference<WithdrawalRequest> persisted = new AtomicReference<>();

            when(userRepository.findByEmail("freelancer@test.com")).thenReturn(Optional.of(freelancer));
            when(walletService.hasSufficientBalance(freelancer, new BigDecimal("500.00"))).thenReturn(true);
            when(withdrawalRepository.save(any())).thenAnswer(invocation -> {
                WithdrawalRequest withdrawal = invocation.getArgument(0);
                if (withdrawal.getId() == null) {
                    withdrawal.setId(1L);
                }
                persisted.set(withdrawal);
                return withdrawal;
            });
            when(withdrawalRepository.findById(1L)).thenAnswer(invocation -> Optional.ofNullable(persisted.get()));
            when(asaasService.createPixTransfer(any(), any(), any(), any()))
                    .thenReturn(Map.of("id", "tr_123", "status", "PENDING"));

            WithdrawalRequest result = withdrawalService.requestWithdrawal(
                    "freelancer@test.com",
                    new BigDecimal("500.00"),
                    "PIX",
                    "12345678900",
                    "CPF",
                    null
            );

            assertThat(result).isNotNull();
            assertThat(result.getAmount()).isEqualByComparingTo(new BigDecimal("500.00"));
            assertThat(result.getWithdrawalMethod()).isEqualTo(WithdrawalRequest.WithdrawalMethod.PIX);
            verify(asaasService).createPixTransfer(
                    eq(new BigDecimal("500.00")),
                    eq("12345678900"),
                    eq("CPF"),
                    any()
            );
            verify(asaasService, never()).createTedTransfer(any(), any(), any(), any(), any(), any(), any(), any(), any());
            verify(walletService).deductAvailableBalance(freelancer, new BigDecimal("500.00"));
        }

        @Test
        @DisplayName("deve criar saque via TED com sucesso")
        void shouldCreateTedWithdrawal() {
            AtomicReference<WithdrawalRequest> persisted = new AtomicReference<>();

            when(userRepository.findByEmail("freelancer@test.com")).thenReturn(Optional.of(freelancer));
            when(walletService.hasSufficientBalance(freelancer, new BigDecimal("500.00"))).thenReturn(true);
            when(bankAccountService.getOwnedAccount("freelancer@test.com", 10L)).thenReturn(tedAccount);
            when(withdrawalRepository.save(any())).thenAnswer(invocation -> {
                WithdrawalRequest withdrawal = invocation.getArgument(0);
                if (withdrawal.getId() == null) {
                    withdrawal.setId(1L);
                }
                persisted.set(withdrawal);
                return withdrawal;
            });
            when(withdrawalRepository.findById(1L)).thenAnswer(invocation -> Optional.ofNullable(persisted.get()));
            when(asaasService.createTedTransfer(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(Map.of("id", "tr_456", "status", "PENDING"));

            WithdrawalRequest result = withdrawalService.requestWithdrawal(
                    "freelancer@test.com",
                    new BigDecimal("500.00"),
                    "TED",
                    null,
                    null,
                    10L
            );

            assertThat(result).isNotNull();
            assertThat(result.getWithdrawalMethod()).isEqualTo(WithdrawalRequest.WithdrawalMethod.TED);
            assertThat(result.getBankAccount()).isEqualTo(tedAccount);
            assertThat(result.getPixKey()).isNull();
            verify(bankAccountService).getOwnedAccount("freelancer@test.com", 10L);
            verify(asaasService).createTedTransfer(
                    eq(new BigDecimal("500.00")),
                    eq("001"),
                    eq("1234"),
                    eq("12345"),
                    eq("6"),
                    eq("CONTA_CORRENTE"),
                    eq("Joao Silva"),
                    eq("12345678900"),
                    any()
            );
            verify(asaasService, never()).createPixTransfer(any(), any(), any(), any());
        }

        @Test
        @DisplayName("deve rejeitar saque TED sem conta bancaria")
        void shouldRejectTedWithoutBankAccount() {
            when(userRepository.findByEmail("freelancer@test.com")).thenReturn(Optional.of(freelancer));
            when(walletService.hasSufficientBalance(freelancer, new BigDecimal("500.00"))).thenReturn(true);

            assertThatThrownBy(() -> withdrawalService.requestWithdrawal(
                    "freelancer@test.com",
                    new BigDecimal("500.00"),
                    "TED",
                    null,
                    null,
                    null
            )).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("conta bancaria");
        }

        @Test
        @DisplayName("deve rejeitar saque TED usando chave PIX")
        void shouldRejectTedWithPixAccount() {
            FreelancerBankAccount pixAccount = FreelancerBankAccount.builder()
                    .id(11L)
                    .user(freelancer)
                    .accountType(FreelancerBankAccount.AccountType.PIX_KEY)
                    .pixKey("12345678900")
                    .pixKeyType("CPF")
                    .holderName("Joao Silva")
                    .holderCpfCnpj("12345678900")
                    .build();

            when(userRepository.findByEmail("freelancer@test.com")).thenReturn(Optional.of(freelancer));
            when(walletService.hasSufficientBalance(freelancer, new BigDecimal("500.00"))).thenReturn(true);
            when(bankAccountService.getOwnedAccount("freelancer@test.com", 11L)).thenReturn(pixAccount);

            assertThatThrownBy(() -> withdrawalService.requestWithdrawal(
                    "freelancer@test.com",
                    new BigDecimal("500.00"),
                    "TED",
                    null,
                    null,
                    11L
            )).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("TED");
        }

        @Test
        @DisplayName("deve rejeitar saque com saldo insuficiente")
        void shouldRejectInsufficientBalance() {
            when(userRepository.findByEmail("freelancer@test.com")).thenReturn(Optional.of(freelancer));
            when(walletService.hasSufficientBalance(freelancer, new BigDecimal("2000.00"))).thenReturn(false);

            assertThatThrownBy(() -> withdrawalService.requestWithdrawal(
                    "freelancer@test.com",
                    new BigDecimal("2000.00"),
                    "PIX",
                    "12345678900",
                    "CPF",
                    null
            )).isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("deve rejeitar usuario nao encontrado")
        void shouldRejectUserNotFound() {
            when(userRepository.findByEmail("nope@test.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> withdrawalService.requestWithdrawal(
                    "nope@test.com",
                    new BigDecimal("100.00"),
                    "PIX",
                    "key",
                    "CPF",
                    null
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
            when(userRepository.findByEmail("freelancer@test.com")).thenReturn(Optional.of(freelancer));

            withdrawalService.cancelWithdrawal(1L, "freelancer@test.com");

            assertThat(withdrawal.getStatus()).isEqualTo(WithdrawalRequest.WithdrawalStatus.CANCELLED);
            verify(withdrawalRepository).save(withdrawal);
            verify(walletService).addPendingBalance(freelancer, new BigDecimal("500.00"));
        }

        @Test
        @DisplayName("deve rejeitar cancelamento de saque nao pendente")
        void shouldRejectCancelNonPending() {
            WithdrawalRequest withdrawal = WithdrawalRequest.builder()
                    .id(1L)
                    .freelancer(freelancer)
                    .amount(new BigDecimal("500.00"))
                    .status(WithdrawalRequest.WithdrawalStatus.COMPLETED)
                    .build();

            when(withdrawalRepository.findById(1L)).thenReturn(Optional.of(withdrawal));
            when(userRepository.findByEmail("freelancer@test.com")).thenReturn(Optional.of(freelancer));

            assertThatThrownBy(() -> withdrawalService.cancelWithdrawal(1L, "freelancer@test.com"))
                    .isInstanceOf(IllegalStateException.class);
            verify(walletService, never()).addPendingBalance(any(), any());
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
