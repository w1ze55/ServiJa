package com.servija.servija.wallet;

import com.servija.servija.exception.ResourceNotFoundException;
import com.servija.servija.model.FreelancerWallet;
import com.servija.servija.model.User;
import com.servija.servija.repository.FreelancerWalletRepository;
import com.servija.servija.repository.UserRepository;
import com.servija.servija.service.FreelancerWalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FreelancerWalletServiceTest {

    @Mock
    private FreelancerWalletRepository walletRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private FreelancerWalletService walletService;

    private User freelancer;
    private FreelancerWallet wallet;

    @BeforeEach
    void setUp() {
        freelancer = User.builder()
                .id(1L)
                .email("freelancer@test.com")
                .name("João Silva")
                .build();

        wallet = FreelancerWallet.builder()
                .id(1L)
                .user(freelancer)
                .availableBalance(new BigDecimal("1000.00"))
                .pendingBalance(new BigDecimal("500.00"))
                .totalEarned(new BigDecimal("5000.00"))
                .totalWithdrawn(new BigDecimal("3500.00"))
                .build();
    }

    @Nested
    @DisplayName("getOrCreateWallet")
    class GetOrCreateWallet {

        @Test
        @DisplayName("deve retornar carteira existente")
        void shouldReturnExistingWallet() {
            when(userRepository.findByEmail("freelancer@test.com")).thenReturn(Optional.of(freelancer));
            when(walletRepository.findByUser(freelancer)).thenReturn(Optional.of(wallet));

            FreelancerWallet result = walletService.getOrCreateWallet("freelancer@test.com");

            assertThat(result).isEqualTo(wallet);
            verify(walletRepository, never()).save(any());
        }

        @Test
        @DisplayName("deve criar nova carteira se não existir")
        void shouldCreateWalletIfNotExists() {
            when(userRepository.findByEmail("freelancer@test.com")).thenReturn(Optional.of(freelancer));
            when(walletRepository.findByUser(freelancer)).thenReturn(Optional.empty());
            when(walletRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            FreelancerWallet result = walletService.getOrCreateWallet("freelancer@test.com");

            assertThat(result.getAvailableBalance()).isEqualByComparingTo(BigDecimal.ZERO);
            assertThat(result.getPendingBalance()).isEqualByComparingTo(BigDecimal.ZERO);
            verify(walletRepository).save(any());
        }

        @Test
        @DisplayName("deve lançar exceção se usuário não existir")
        void shouldThrowIfUserNotFound() {
            when(userRepository.findByEmail("nope@test.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> walletService.getOrCreateWallet("nope@test.com"))
                    .isInstanceOf(ResourceNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("addPendingBalance")
    class AddPendingBalance {

        @Test
        @DisplayName("deve adicionar saldo pendente")
        void shouldAddPendingBalance() {
            when(walletRepository.findByUser(freelancer)).thenReturn(Optional.of(wallet));
            BigDecimal amount = new BigDecimal("200.00");

            walletService.addPendingBalance(freelancer, amount);

            verify(walletRepository).save(wallet);
            // pendingBalance era 500, agora deve ser 700
            assertThat(wallet.getPendingBalance()).isEqualByComparingTo(new BigDecimal("700.00"));
        }
    }

    @Nested
    @DisplayName("deductAvailableBalance")
    class DeductAvailableBalance {

        @Test
        @DisplayName("deve deduzir saldo disponível")
        void shouldDeductBalance() {
            when(walletRepository.findByUserIdForUpdate(freelancer.getId())).thenReturn(Optional.of(wallet));
            BigDecimal amount = new BigDecimal("300.00");

            walletService.deductAvailableBalance(freelancer, amount);

            verify(walletRepository).save(wallet);
        }

        @Test
        @DisplayName("deve lançar exceção se saldo insuficiente")
        void shouldThrowIfInsufficientBalance() {
            when(walletRepository.findByUserIdForUpdate(freelancer.getId())).thenReturn(Optional.of(wallet));
            BigDecimal amount = new BigDecimal("2000.00"); // Mais que o disponível (1000)

            assertThatThrownBy(() -> walletService.deductAvailableBalance(freelancer, amount))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("Saldo insuficiente");
        }
    }

    @Nested
    @DisplayName("hasSufficientBalance")
    class HasSufficientBalance {

        @Test
        @DisplayName("deve retornar true quando saldo é suficiente")
        void shouldReturnTrueWhenSufficient() {
            when(walletRepository.findByUser(freelancer)).thenReturn(Optional.of(wallet));

            boolean result = walletService.hasSufficientBalance(freelancer, new BigDecimal("500.00"));

            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("deve retornar false quando saldo é insuficiente")
        void shouldReturnFalseWhenInsufficient() {
            when(walletRepository.findByUser(freelancer)).thenReturn(Optional.of(wallet));

            boolean result = walletService.hasSufficientBalance(freelancer, new BigDecimal("1500.00"));

            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("getWalletBalance")
    class GetWalletBalance {

        @Test
        @DisplayName("deve retornar mapa com todos os saldos")
        void shouldReturnAllBalances() {
            when(userRepository.findByEmail("freelancer@test.com")).thenReturn(Optional.of(freelancer));
            when(walletRepository.findByUser(freelancer)).thenReturn(Optional.of(wallet));

            Map<String, BigDecimal> balance = walletService.getWalletBalance("freelancer@test.com");

            assertThat(balance.get("availableBalance")).isEqualByComparingTo(new BigDecimal("1000.00"));
            assertThat(balance.get("pendingBalance")).isEqualByComparingTo(new BigDecimal("500.00"));
            assertThat(balance.get("totalEarned")).isEqualByComparingTo(new BigDecimal("5000.00"));
            assertThat(balance.get("totalWithdrawn")).isEqualByComparingTo(new BigDecimal("3500.00"));
        }
    }

    @Nested
    @DisplayName("releasePendingToAvailable")
    class ReleasePendingToAvailable {

        @Test
        @DisplayName("deve mover saldo de pendente para disponível")
        void shouldMovePendingToAvailable() {
            when(userRepository.findByEmail("freelancer@test.com")).thenReturn(Optional.of(freelancer));
            when(walletRepository.findByUser(freelancer)).thenReturn(Optional.of(wallet));

            walletService.releasePendingToAvailable("freelancer@test.com", new BigDecimal("200.00"));

            verify(walletRepository).save(wallet);
            // available: 1000 + 200 = 1200, pending: 500 - 200 = 300
            assertThat(wallet.getAvailableBalance()).isEqualByComparingTo(new BigDecimal("1200.00"));
            assertThat(wallet.getPendingBalance()).isEqualByComparingTo(new BigDecimal("300.00"));
        }
    }
}
