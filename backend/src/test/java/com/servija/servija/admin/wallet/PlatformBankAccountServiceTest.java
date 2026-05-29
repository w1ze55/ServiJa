package com.servija.servija.admin.wallet;

import com.servija.servija.dto.BankAccountDTO;
import com.servija.servija.dto.BankAccountResponse;
import com.servija.servija.model.PlatformBankAccount;
import com.servija.servija.repository.PlatformBankAccountRepository;
import com.servija.servija.service.AuditLogger;
import com.servija.servija.service.PlatformBankAccountService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlatformBankAccountServiceTest {

    @Mock
    private PlatformBankAccountRepository platformBankAccountRepository;

    @Mock
    private AuditLogger auditLogger;

    @InjectMocks
    private PlatformBankAccountService platformBankAccountService;

    @Test
    @DisplayName("deve criar chave PIX e marcar primeira conta como padrão")
    void shouldCreatePixKeyAndSetFirstAccountAsDefault() {
        when(platformBankAccountRepository.countBy()).thenReturn(0L);
        when(platformBankAccountRepository.save(any())).thenAnswer(invocation -> {
            PlatformBankAccount account = invocation.getArgument(0);
            account.setId(1L);
            return account;
        });

        BankAccountDTO dto = BankAccountDTO.builder()
                .accountType("PIX_KEY")
                .pixKey("12345678900")
                .pixKeyType("CPF")
                .holderName("Servi Ja")
                .holderCpfCnpj("12345678900")
                .build();

        BankAccountResponse response = platformBankAccountService.createAccount(dto);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getAccountType()).isEqualTo("PIX_KEY");
        assertThat(response.getPixKeyValue()).isEqualTo("12345678900");
        assertThat(response.getIsDefault()).isTrue();
        verify(platformBankAccountRepository).clearDefault();
    }

    @Test
    @DisplayName("deve criar conta TED sem trocar a padrão atual")
    void shouldCreateTedAccountWithoutChangingDefault() {
        when(platformBankAccountRepository.countBy()).thenReturn(1L);
        when(platformBankAccountRepository.save(any())).thenAnswer(invocation -> {
            PlatformBankAccount account = invocation.getArgument(0);
            account.setId(2L);
            return account;
        });

        BankAccountDTO dto = BankAccountDTO.builder()
                .accountType("BANK_ACCOUNT")
                .bankName("Banco do Brasil")
                .bankCode("001")
                .agency("1234")
                .agencyDigit("5")
                .accountNumber("567890")
                .accountDigit("1")
                .accountBankType("CONTA_CORRENTE")
                .holderName("Servi Ja")
                .holderCpfCnpj("12345678900")
                .isDefault(false)
                .build();

        BankAccountResponse response = platformBankAccountService.createAccount(dto);

        assertThat(response.getId()).isEqualTo(2L);
        assertThat(response.getAccountType()).isEqualTo("BANK_ACCOUNT");
        assertThat(response.getBankName()).isEqualTo("Banco do Brasil");
        assertThat(response.getAccountNumber()).isEqualTo("****7890");
        assertThat(response.getIsDefault()).isFalse();
        verify(platformBankAccountRepository, never()).clearDefault();
    }

    @Test
    @DisplayName("deve definir conta como padrão")
    void shouldSetDefaultAccount() {
        PlatformBankAccount account = PlatformBankAccount.builder()
                .id(3L)
                .accountType(PlatformBankAccount.AccountType.PIX_KEY)
                .pixKey("servija@wallet.com")
                .pixKeyType("EMAIL")
                .holderName("Servi Ja")
                .holderCpfCnpj("12345678900")
                .isDefault(false)
                .build();

        when(platformBankAccountRepository.findById(3L)).thenReturn(Optional.of(account));
        when(platformBankAccountRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        BankAccountResponse response = platformBankAccountService.setDefault(3L);

        assertThat(response.getIsDefault()).isTrue();
        verify(platformBankAccountRepository).clearDefault();
        verify(platformBankAccountRepository).save(account);
    }

    @Test
    @DisplayName("deve remover conta padrão e promover a próxima")
    void shouldDeleteDefaultAccountAndPromoteNextOne() {
        PlatformBankAccount deleted = PlatformBankAccount.builder()
                .id(1L)
                .accountType(PlatformBankAccount.AccountType.PIX_KEY)
                .pixKey("12345678900")
                .pixKeyType("CPF")
                .holderName("Servi Ja")
                .holderCpfCnpj("12345678900")
                .isDefault(true)
                .build();

        PlatformBankAccount next = PlatformBankAccount.builder()
                .id(2L)
                .accountType(PlatformBankAccount.AccountType.BANK_ACCOUNT)
                .bankName("Banco do Brasil")
                .bankCode("001")
                .agency("1234")
                .accountNumber("567890")
                .accountBankType(PlatformBankAccount.BankAccountType.CONTA_CORRENTE)
                .holderName("Servi Ja")
                .holderCpfCnpj("12345678900")
                .isDefault(false)
                .build();

        when(platformBankAccountRepository.findById(1L)).thenReturn(Optional.of(deleted));
        when(platformBankAccountRepository.findByOrderByIsDefaultDescCreatedAtDesc()).thenReturn(List.of(next));
        when(platformBankAccountRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        platformBankAccountService.deleteAccount(1L);

        assertThat(next.getIsDefault()).isTrue();
        verify(platformBankAccountRepository).delete(deleted);
        verify(platformBankAccountRepository).save(next);
    }

    @Test
    @DisplayName("deve rejeitar criação ao atingir limite de contas")
    void shouldRejectWhenMaxAccountsReached() {
        when(platformBankAccountRepository.countBy()).thenReturn(5L);

        assertThatThrownBy(() -> platformBankAccountService.createAccount(BankAccountDTO.builder()
                .accountType("PIX_KEY")
                .pixKey("wallet@servija.com")
                .pixKeyType("EMAIL")
                .holderName("Servi Ja")
                .holderCpfCnpj("12345678900")
                .build()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Limite de 5 contas");
    }

    @Test
    @DisplayName("deve rejeitar chave PIX vazia")
    void shouldRejectBlankPixKey() {
        when(platformBankAccountRepository.countBy()).thenReturn(0L);

        assertThatThrownBy(() -> platformBankAccountService.createAccount(BankAccountDTO.builder()
                .accountType("PIX_KEY")
                .pixKey(" ")
                .pixKeyType("CPF")
                .holderName("Servi Ja")
                .holderCpfCnpj("12345678900")
                .build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("PIX");
    }

    @Test
    @DisplayName("deve rejeitar conta bancária com dados incompletos")
    void shouldRejectIncompleteBankAccountData() {
        when(platformBankAccountRepository.countBy()).thenReturn(0L);

        assertThatThrownBy(() -> platformBankAccountService.createAccount(BankAccountDTO.builder()
                .accountType("BANK_ACCOUNT")
                .bankName("Banco do Brasil")
                .holderName("Servi Ja")
                .holderCpfCnpj("12345678900")
                .build()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("banc");
    }
}
