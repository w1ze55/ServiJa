package com.servija.servija.wallet;

import com.servija.servija.dto.BankAccountDTO;
import com.servija.servija.dto.BankAccountResponse;
import com.servija.servija.model.FreelancerBankAccount;
import com.servija.servija.model.User;
import com.servija.servija.repository.FreelancerBankAccountRepository;
import com.servija.servija.repository.UserRepository;
import com.servija.servija.service.AuditLogger;
import com.servija.servija.service.BankAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BankAccountServiceTest {

    @Mock
    private FreelancerBankAccountRepository bankAccountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AuditLogger auditLogger;

    @InjectMocks
    private BankAccountService bankAccountService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .id(1L)
                .email("freelancer@test.com")
                .name("João Silva")
                .build();
    }

    @Nested
    @DisplayName("createAccount - PIX")
    class CreatePixAccount {

        @Test
        @DisplayName("deve criar chave PIX com sucesso")
        void shouldCreatePixKey() {
            when(userRepository.findByEmail("freelancer@test.com")).thenReturn(Optional.of(user));
            when(bankAccountRepository.countByUserId(1L)).thenReturn(0L);
            when(bankAccountRepository.save(any())).thenAnswer(inv -> {
                FreelancerBankAccount acc = inv.getArgument(0);
                acc.setId(1L);
                return acc;
            });

            BankAccountDTO dto = BankAccountDTO.builder()
                    .accountType("PIX_KEY")
                    .pixKey("12345678900")
                    .pixKeyType("CPF")
                    .holderName("João Silva")
                    .holderCpfCnpj("12345678900")
                    .build();

            BankAccountResponse response = bankAccountService.createAccount("freelancer@test.com", dto);

            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getAccountType()).isEqualTo("PIX_KEY");
            assertThat(response.getIsDefault()).isTrue(); // Primeira conta = padrão
            verify(bankAccountRepository).save(any());
        }

        @Test
        @DisplayName("deve rejeitar chave PIX vazia")
        void shouldRejectEmptyPixKey() {
            when(userRepository.findByEmail("freelancer@test.com")).thenReturn(Optional.of(user));
            when(bankAccountRepository.countByUserId(1L)).thenReturn(0L);

            BankAccountDTO dto = BankAccountDTO.builder()
                    .accountType("PIX_KEY")
                    .pixKey("")
                    .holderName("João")
                    .holderCpfCnpj("12345678900")
                    .build();

            assertThatThrownBy(() -> bankAccountService.createAccount("freelancer@test.com", dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Chave PIX");
        }
    }

    @Nested
    @DisplayName("createAccount - Bank Account")
    class CreateBankAccount {

        @Test
        @DisplayName("deve criar conta bancária com sucesso")
        void shouldCreateBankAccount() {
            when(userRepository.findByEmail("freelancer@test.com")).thenReturn(Optional.of(user));
            when(bankAccountRepository.countByUserId(1L)).thenReturn(1L);
            when(bankAccountRepository.save(any())).thenAnswer(inv -> {
                FreelancerBankAccount acc = inv.getArgument(0);
                acc.setId(2L);
                return acc;
            });

            BankAccountDTO dto = BankAccountDTO.builder()
                    .accountType("BANK_ACCOUNT")
                    .bankName("Banco do Brasil")
                    .bankCode("001")
                    .agency("1234")
                    .accountNumber("567890")
                    .accountDigit("1")
                    .accountBankType("CONTA_CORRENTE")
                    .holderName("João Silva")
                    .holderCpfCnpj("12345678900")
                    .build();

            BankAccountResponse response = bankAccountService.createAccount("freelancer@test.com", dto);

            assertThat(response.getAccountType()).isEqualTo("BANK_ACCOUNT");
            verify(bankAccountRepository).save(any());
        }

        @Test
        @DisplayName("deve rejeitar se dados bancários incompletos")
        void shouldRejectIncompleteBankData() {
            when(userRepository.findByEmail("freelancer@test.com")).thenReturn(Optional.of(user));
            when(bankAccountRepository.countByUserId(1L)).thenReturn(0L);

            BankAccountDTO dto = BankAccountDTO.builder()
                    .accountType("BANK_ACCOUNT")
                    .bankCode(null)
                    .agency(null)
                    .accountNumber(null)
                    .holderName("João")
                    .holderCpfCnpj("12345678900")
                    .build();

            assertThatThrownBy(() -> bankAccountService.createAccount("freelancer@test.com", dto))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("bancários");
        }
    }

    @Nested
    @DisplayName("createAccount - Limites")
    class AccountLimits {

        @Test
        @DisplayName("deve rejeitar se atingiu limite de 5 contas")
        void shouldRejectWhenMaxAccountsReached() {
            when(userRepository.findByEmail("freelancer@test.com")).thenReturn(Optional.of(user));
            when(bankAccountRepository.countByUserId(1L)).thenReturn(5L);

            BankAccountDTO dto = BankAccountDTO.builder()
                    .accountType("PIX_KEY")
                    .pixKey("test@email.com")
                    .pixKeyType("EMAIL")
                    .holderName("João")
                    .holderCpfCnpj("12345678900")
                    .build();

            assertThatThrownBy(() -> bankAccountService.createAccount("freelancer@test.com", dto))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Limite");
        }
    }

    @Nested
    @DisplayName("deleteAccount")
    class DeleteAccount {

        @Test
        @DisplayName("deve remover conta com sucesso")
        void shouldDeleteAccount() {
            FreelancerBankAccount account = FreelancerBankAccount.builder()
                    .id(1L)
                    .user(user)
                    .isDefault(false)
                    .build();

            when(userRepository.findByEmail("freelancer@test.com")).thenReturn(Optional.of(user));
            when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(account));

            bankAccountService.deleteAccount("freelancer@test.com", 1L);

            verify(bankAccountRepository).delete(account);
        }

        @Test
        @DisplayName("deve rejeitar se conta não pertence ao usuário")
        void shouldRejectIfNotOwner() {
            User otherUser = User.builder().id(2L).build();
            FreelancerBankAccount account = FreelancerBankAccount.builder()
                    .id(1L)
                    .user(otherUser)
                    .build();

            when(userRepository.findByEmail("freelancer@test.com")).thenReturn(Optional.of(user));
            when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(account));

            assertThatThrownBy(() -> bankAccountService.deleteAccount("freelancer@test.com", 1L))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("setDefault")
    class SetDefault {

        @Test
        @DisplayName("deve definir conta como padrão")
        void shouldSetDefault() {
            FreelancerBankAccount account = FreelancerBankAccount.builder()
                    .id(1L)
                    .user(user)
                    .accountType(FreelancerBankAccount.AccountType.PIX_KEY)
                    .pixKey("key")
                    .holderName("João")
                    .holderCpfCnpj("12345678900")
                    .isDefault(false)
                    .build();

            when(userRepository.findByEmail("freelancer@test.com")).thenReturn(Optional.of(user));
            when(bankAccountRepository.findById(1L)).thenReturn(Optional.of(account));
            when(bankAccountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            BankAccountResponse response = bankAccountService.setDefault("freelancer@test.com", 1L);

            assertThat(response.getIsDefault()).isTrue();
            verify(bankAccountRepository).clearDefaultForUser(1L);
        }
    }
}
