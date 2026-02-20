package com.servija.servija.auth;

import com.servija.servija.dto.AuthResponse;
import com.servija.servija.exception.ResourceNotFoundException;
import com.servija.servija.exception.TooManyAttemptsException;
import com.servija.servija.model.Contractor;
import com.servija.servija.model.User;
import com.servija.servija.repository.ContractorRepository;
import com.servija.servija.repository.FreelancerRepository;
import com.servija.servija.repository.UserRepository;
import com.servija.servija.service.AuthService;
import com.servija.servija.service.CustomUserDetailsService;
import com.servija.servija.service.EmailService;
import com.servija.servija.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceVerificationTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ContractorRepository contractorRepository;
    @Mock
    private FreelancerRepository freelancerRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private CustomUserDetailsService userDetailsService;
    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    private Contractor user;

    @BeforeEach
    void setUp() {
        user = Contractor.builder()
                .id(1L)
                .name("João Silva")
                .email("joao@email.com")
                .cpf("12345678901")
                .phone("11999999999")
                .password("encoded-password")
                .userType(User.UserType.CONTRACTOR)
                .emailVerified(false)
                .active(true)
                .verificationCode("123456")
                .verificationCodeExpiry(LocalDateTime.now().plusHours(24))
                .verificationAttempts(0)
                .totalContracts(0)
                .activeContracts(0)
                .completedContracts(0)
                .build();
    }

    @Nested
    @DisplayName("Verificação de Email")
    class EmailVerification {

        @Test
        @DisplayName("Deve verificar email com código correto")
        void verifyEmail_correctCode_success() {
            when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenReturn(user);

            UserDetails mockUserDetails = mock(UserDetails.class);
            when(userDetailsService.loadUserByUsername("joao@email.com")).thenReturn(mockUserDetails);
            when(jwtService.generateToken(mockUserDetails)).thenReturn("jwt-verified");

            AuthResponse response = authService.verifyEmail("joao@email.com", "123456");

            assertThat(response).isNotNull();
            assertThat(response.getToken()).isEqualTo("jwt-verified");
            assertThat(user.getEmailVerified()).isTrue();
            assertThat(user.getVerificationCode()).isNull();
            assertThat(user.getVerificationCodeExpiry()).isNull();
            assertThat(user.getVerificationAttempts()).isZero();
            assertThat(user.getVerificationLockUntil()).isNull();
        }

        @Test
        @DisplayName("Deve lançar exceção com código incorreto e informar tentativas restantes")
        void verifyEmail_wrongCode_throwsWithRemainingAttempts() {
            when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenReturn(user);

            assertThatThrownBy(() -> authService.verifyEmail("joao@email.com", "000000"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("4 tentativas restantes");

            assertThat(user.getVerificationAttempts()).isEqualTo(1);
        }

        @Test
        @DisplayName("Deve lançar exceção com código expirado")
        void verifyEmail_expiredCode_throwsException() {
            user.setVerificationCodeExpiry(LocalDateTime.now().minusHours(1));

            when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.verifyEmail("joao@email.com", "123456"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("Código de verificação expirado");
        }

        @Test
        @DisplayName("Deve lançar exceção quando email já verificado")
        void verifyEmail_alreadyVerified_throwsException() {
            user.setEmailVerified(true);

            when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.verifyEmail("joao@email.com", "123456"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("E-mail já verificado");
        }
    }

    @Nested
    @DisplayName("Lockout e Tentativas")
    class LockoutAndAttempts {

        @Test
        @DisplayName("Deve aplicar lockout de 30 minutos após 5 tentativas erradas")
        void verifyEmail_maxAttempts_triggersLockout() {
            user.setVerificationAttempts(4); // 4 tentativas anteriores, esta será a 5ª

            when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenReturn(user);

            assertThatThrownBy(() -> authService.verifyEmail("joao@email.com", "000000"))
                    .isInstanceOf(TooManyAttemptsException.class)
                    .hasMessage("Muitas tentativas. Aguarde para tentar novamente.");

            assertThat(user.getVerificationAttempts()).isEqualTo(5);
            assertThat(user.getVerificationLockUntil()).isNotNull();
            assertThat(user.getVerificationLockUntil()).isAfter(LocalDateTime.now().plusMinutes(29));
            assertThat(user.getVerificationLockUntil()).isBefore(LocalDateTime.now().plusMinutes(31));
        }

        @Test
        @DisplayName("Deve bloquear requisição durante lockout ativo")
        void verifyEmail_duringLockout_throwsException() {
            user.setVerificationLockUntil(LocalDateTime.now().plusMinutes(20));
            user.setVerificationAttempts(5);

            when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.verifyEmail("joao@email.com", "123456"))
                    .isInstanceOf(TooManyAttemptsException.class);

            verify(userRepository, never()).save(any());
        }

        @Test
        @DisplayName("Deve resetar tentativas quando lockout expira")
        void verifyEmail_lockoutExpired_resetsAttempts() {
            user.setVerificationLockUntil(LocalDateTime.now().minusMinutes(5));
            user.setVerificationAttempts(5);

            when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenReturn(user);

            UserDetails mockUserDetails = mock(UserDetails.class);
            when(userDetailsService.loadUserByUsername("joao@email.com")).thenReturn(mockUserDetails);
            when(jwtService.generateToken(mockUserDetails)).thenReturn("jwt-after-lockout");

            AuthResponse response = authService.verifyEmail("joao@email.com", "123456");

            assertThat(response).isNotNull();
            assertThat(user.getVerificationAttempts()).isZero();
            assertThat(user.getVerificationLockUntil()).isNull();
        }

        @Test
        @DisplayName("Deve incrementar tentativas corretamente a cada erro")
        void verifyEmail_incrementsAttemptsOnEachError() {
            when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenReturn(user);

            for (int i = 1; i <= 4; i++) {
                user.setVerificationAttempts(i - 1);

                try {
                    authService.verifyEmail("joao@email.com", "000000");
                } catch (IllegalArgumentException e) {
                    assertThat(user.getVerificationAttempts()).isEqualTo(i);
                }
            }
        }
    }

    @Nested
    @DisplayName("Reenvio de Código")
    class ResendCode {

        @Test
        @DisplayName("Deve gerar novo código, resetar tentativas e lockout ao reenviar")
        void resendCode_success() {
            String originalCode = user.getVerificationCode();
            user.setVerificationAttempts(3);
            user.setVerificationLockUntil(LocalDateTime.now().plusMinutes(10));

            when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(user));
            when(userRepository.save(any(User.class))).thenReturn(user);

            authService.resendVerificationCode("joao@email.com");

            assertThat(user.getVerificationCode()).isNotNull();
            assertThat(user.getVerificationCode()).hasSize(6);
            assertThat(user.getVerificationAttempts()).isZero();
            assertThat(user.getVerificationLockUntil()).isNull();
            verify(emailService).sendVerificationEmail(eq("joao@email.com"), eq("João Silva"), anyString());
        }

        @Test
        @DisplayName("Deve lançar exceção ao reenviar para email já verificado")
        void resendCode_alreadyVerified_throwsException() {
            user.setEmailVerified(true);

            when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(user));

            assertThatThrownBy(() -> authService.resendVerificationCode("joao@email.com"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("E-mail já verificado");
        }

        @Test
        @DisplayName("Deve lançar exceção ao reenviar para email inexistente")
        void resendCode_emailNotFound_throwsException() {
            when(userRepository.findByEmail("inexistente@email.com")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> authService.resendVerificationCode("inexistente@email.com"))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessage("Usuário não encontrado");
        }
    }
}
