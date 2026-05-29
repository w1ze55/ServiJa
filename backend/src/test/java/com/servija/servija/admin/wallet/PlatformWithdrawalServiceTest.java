package com.servija.servija.admin.wallet;

import com.servija.servija.model.PlatformBankAccount;
import com.servija.servija.model.PlatformWallet;
import com.servija.servija.model.PlatformWithdrawalRequest;
import com.servija.servija.repository.PlatformWithdrawalRequestRepository;
import com.servija.servija.service.AsaasService;
import com.servija.servija.service.AuditLogger;
import com.servija.servija.service.PlatformBankAccountService;
import com.servija.servija.service.PlatformWalletService;
import com.servija.servija.service.PlatformWithdrawalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlatformWithdrawalServiceTest {

    @Mock
    private PlatformWithdrawalRequestRepository platformWithdrawalRequestRepository;

    @Mock
    private PlatformWalletService platformWalletService;

    @Mock
    private PlatformBankAccountService platformBankAccountService;

    @Mock
    private AsaasService asaasService;

    @Mock
    private AuditLogger auditLogger;

    @InjectMocks
    private PlatformWithdrawalService platformWithdrawalService;

    private PlatformWallet wallet;
    private PlatformBankAccount pixAccount;
    private PlatformBankAccount tedAccount;

    @BeforeEach
    void setUp() {
        wallet = PlatformWallet.builder()
                .id(1L)
                .availableBalance(new BigDecimal("1000.00"))
                .reservedBalance(BigDecimal.ZERO)
                .totalEarned(new BigDecimal("1500.00"))
                .totalWithdrawn(new BigDecimal("300.00"))
                .build();

        pixAccount = PlatformBankAccount.builder()
                .id(1L)
                .accountType(PlatformBankAccount.AccountType.PIX_KEY)
                .pixKey("12345678900")
                .pixKeyType("CPF")
                .holderName("Servi Ja")
                .holderCpfCnpj("12345678900")
                .isDefault(true)
                .build();

        tedAccount = PlatformBankAccount.builder()
                .id(2L)
                .accountType(PlatformBankAccount.AccountType.BANK_ACCOUNT)
                .bankName("Banco do Brasil")
                .bankCode("001")
                .agency("1234")
                .agencyDigit("5")
                .accountNumber("567890")
                .accountDigit("1")
                .accountBankType(PlatformBankAccount.BankAccountType.CONTA_CORRENTE)
                .holderName("Servi Ja")
                .holderCpfCnpj("12345678900")
                .build();
    }

    @Test
    @DisplayName("deve solicitar saque PIX com sucesso")
    void shouldCreatePixWithdrawal() {
        mockWithdrawalPersistence();
        when(platformBankAccountService.getRequiredAccount(1L)).thenReturn(pixAccount);
        when(platformBankAccountService.buildDestinationLabel(pixAccount)).thenReturn("PIX - CPF - **900");
        when(platformWalletService.lockWallet()).thenReturn(wallet);
        when(asaasService.createPixTransfer(any(), any(), any(), any()))
                .thenReturn(Map.of("id", "tr_pix_1"));

        PlatformWithdrawalRequest result =
                platformWithdrawalService.requestWithdrawal(new BigDecimal("200.00"), 1L);

        assertThat(result.getStatus()).isEqualTo(PlatformWithdrawalRequest.WithdrawalStatus.COMPLETED);
        assertThat(result.getWithdrawalMethod()).isEqualTo(PlatformWithdrawalRequest.WithdrawalMethod.PIX);
        assertThat(result.getAsaasTransferId()).isEqualTo("tr_pix_1");
        assertThat(result.getProcessedAt()).isNotNull();
        assertThat(wallet.getAvailableBalance()).isEqualByComparingTo(new BigDecimal("800.00"));
        assertThat(wallet.getReservedBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(wallet.getTotalWithdrawn()).isEqualByComparingTo(new BigDecimal("500.00"));
        verify(asaasService).createPixTransfer(
                eq(new BigDecimal("200.00")),
                eq("12345678900"),
                eq("CPF"),
                any()
        );
        verify(asaasService, never()).createTedTransfer(any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("deve solicitar saque TED com sucesso")
    void shouldCreateTedWithdrawal() {
        mockWithdrawalPersistence();
        when(platformBankAccountService.getRequiredAccount(2L)).thenReturn(tedAccount);
        when(platformBankAccountService.buildDestinationLabel(tedAccount))
                .thenReturn("TED - Banco do Brasil - Ag * - **7890");
        when(platformWalletService.lockWallet()).thenReturn(wallet);
        when(asaasService.createTedTransfer(any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(Map.of("id", "tr_ted_1"));

        PlatformWithdrawalRequest result =
                platformWithdrawalService.requestWithdrawal(new BigDecimal("250.00"), 2L);

        assertThat(result.getStatus()).isEqualTo(PlatformWithdrawalRequest.WithdrawalStatus.COMPLETED);
        assertThat(result.getWithdrawalMethod()).isEqualTo(PlatformWithdrawalRequest.WithdrawalMethod.TED);
        assertThat(result.getAsaasTransferId()).isEqualTo("tr_ted_1");
        assertThat(result.getProcessedAt()).isNotNull();
        assertThat(wallet.getAvailableBalance()).isEqualByComparingTo(new BigDecimal("750.00"));
        assertThat(wallet.getReservedBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(wallet.getTotalWithdrawn()).isEqualByComparingTo(new BigDecimal("550.00"));
        verify(asaasService).createTedTransfer(
                eq(new BigDecimal("250.00")),
                eq("001"),
                eq("1234"),
                eq("567890"),
                eq("1"),
                eq("CONTA_CORRENTE"),
                eq("Servi Ja"),
                eq("12345678900"),
                any()
        );
        verify(asaasService, never()).createPixTransfer(any(), any(), any(), any());
    }

    @Test
    @DisplayName("deve rejeitar valor nulo")
    void shouldRejectNullAmount() {
        assertThatThrownBy(() -> platformWithdrawalService.requestWithdrawal(null, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("obrigat");
    }

    @Test
    @DisplayName("deve rejeitar valor abaixo do mínimo")
    void shouldRejectAmountBelowMinimum() {
        assertThatThrownBy(() -> platformWithdrawalService.requestWithdrawal(new BigDecimal("9.99"), 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mínimo");
    }

    @Test
    @DisplayName("deve rejeitar valor acima do máximo")
    void shouldRejectAmountAboveMaximum() {
        assertThatThrownBy(() -> platformWithdrawalService.requestWithdrawal(new BigDecimal("50000.01"), 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("máximo");
    }

    @Test
    @DisplayName("deve rejeitar saque com saldo insuficiente")
    void shouldRejectWhenBalanceIsInsufficient() {
        when(platformBankAccountService.getRequiredAccount(1L)).thenReturn(pixAccount);
        when(platformWalletService.lockWallet()).thenReturn(wallet);

        assertThatThrownBy(() -> platformWithdrawalService.requestWithdrawal(new BigDecimal("2000.00"), 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Saldo insuficiente");

        verify(asaasService, never()).createPixTransfer(any(), any(), any(), any());
    }

    @Test
    @DisplayName("deve rejeitar conta PIX sem chave válida")
    void shouldRejectInvalidPixAccount() {
        PlatformBankAccount invalidPixAccount = PlatformBankAccount.builder()
                .id(1L)
                .accountType(PlatformBankAccount.AccountType.PIX_KEY)
                .pixKey(" ")
                .pixKeyType("CPF")
                .holderName("Servi Ja")
                .holderCpfCnpj("12345678900")
                .build();

        when(platformBankAccountService.getRequiredAccount(1L)).thenReturn(invalidPixAccount);

        assertThatThrownBy(() -> platformWithdrawalService.requestWithdrawal(new BigDecimal("100.00"), 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PIX");
    }

    @Test
    @DisplayName("deve rejeitar conta TED incompleta")
    void shouldRejectIncompleteTedAccount() {
        PlatformBankAccount incompleteTedAccount = PlatformBankAccount.builder()
                .id(2L)
                .accountType(PlatformBankAccount.AccountType.BANK_ACCOUNT)
                .bankName("Banco do Brasil")
                .accountNumber("567890")
                .holderName("Servi Ja")
                .holderCpfCnpj("12345678900")
                .build();

        when(platformBankAccountService.getRequiredAccount(2L)).thenReturn(incompleteTedAccount);

        assertThatThrownBy(() -> platformWithdrawalService.requestWithdrawal(new BigDecimal("100.00"), 2L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("TED");
    }

    @Test
    @DisplayName("deve reverter saldo reservado quando Asaas falha")
    void shouldMarkWithdrawalAsFailedAndReleaseReservedBalance() {
        mockWithdrawalPersistence();
        when(platformBankAccountService.getRequiredAccount(1L)).thenReturn(pixAccount);
        when(platformBankAccountService.buildDestinationLabel(pixAccount)).thenReturn("PIX - CPF - **900");
        when(platformWalletService.lockWallet()).thenReturn(wallet);
        when(asaasService.createPixTransfer(any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Asaas indisponivel"));

        PlatformWithdrawalRequest result =
                platformWithdrawalService.requestWithdrawal(new BigDecimal("120.00"), 1L);

        assertThat(result.getStatus()).isEqualTo(PlatformWithdrawalRequest.WithdrawalStatus.FAILED);
        assertThat(result.getRejectionReason()).contains("Asaas indisponivel");
        assertThat(result.getProcessedAt()).isNull();
        assertThat(wallet.getAvailableBalance()).isEqualByComparingTo(new BigDecimal("1000.00"));
        assertThat(wallet.getReservedBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(wallet.getTotalWithdrawn()).isEqualByComparingTo(new BigDecimal("300.00"));
    }

    private void mockWithdrawalPersistence() {
        when(platformWithdrawalRequestRepository.save(any())).thenAnswer(invocation -> {
            PlatformWithdrawalRequest withdrawal = invocation.getArgument(0);
            if (withdrawal.getId() == null) {
                withdrawal.setId(1L);
            }
            return withdrawal;
        });
    }
}
