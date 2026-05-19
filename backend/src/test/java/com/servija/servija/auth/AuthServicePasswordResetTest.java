package com.servija.servija.auth;

import com.servija.servija.model.Contractor;
import com.servija.servija.model.User;
import com.servija.servija.repository.ContractorRepository;
import com.servija.servija.repository.FreelancerRepository;
import com.servija.servija.repository.UserRepository;
import com.servija.servija.service.AddressService;
import com.servija.servija.service.AdminResetPasswordLinkService;
import com.servija.servija.service.AuditLogger;
import com.servija.servija.service.AuthService;
import com.servija.servija.service.CustomUserDetailsService;
import com.servija.servija.service.EmailService;
import com.servija.servija.service.JwtService;
import com.servija.servija.service.PasswordPolicyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServicePasswordResetTest {

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
    @Mock
    private AuditLogger auditLogger;
    @Mock
    private AddressService addressService;
    @Mock
    private PasswordPolicyService passwordPolicyService;
    @Mock
    private AdminResetPasswordLinkService adminResetPasswordLinkService;

    @InjectMocks
    private AuthService authService;

    private Contractor user;

    @BeforeEach
    void setUp() {
        user = Contractor.builder()
                .id(1L)
                .name("Joao Silva")
                .email("joao@email.com")
                .cpf("12345678901")
                .phone("11999999999")
                .password("encoded-password")
                .userType(User.UserType.CONTRACTOR)
                .active(true)
                .resetPasswordToken("123456")
                .resetPasswordTokenExpiry(LocalDateTime.now().plusMinutes(30))
                .resetPasswordAttempts(2)
                .resetPasswordLockUntil(LocalDateTime.now().plusMinutes(5))
                .adminResetPasswordTokenHash("existing-admin-link-hash")
                .adminResetPasswordTokenExpiry(LocalDateTime.now().plusMinutes(30))
                .totalContracts(0)
                .activeContracts(0)
                .completedContracts(0)
                .build();
    }

    @Test
    @DisplayName("forgotPassword deve gerar codigo e enviar email do fluxo por codigo")
    void forgotPassword_generatesCodeAndSendsCodeEmail() {
        when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.forgotPassword("joao@email.com");

        assertThat(user.getResetPasswordToken()).isNotNull().hasSize(6);
        assertThat(user.getResetPasswordTokenExpiry()).isAfter(LocalDateTime.now().plusMinutes(29));
        assertThat(user.getResetPasswordAttempts()).isZero();
        assertThat(user.getResetPasswordLockUntil()).isNull();
        verify(emailService).sendPasswordResetCodeEmail(
                eq("joao@email.com"),
                eq("Joao Silva"),
                eq(user.getResetPasswordToken()));
    }

    @Test
    @DisplayName("resetPassword por codigo deve limpar o link pendente do admin")
    void resetPassword_clearsAdminLinkAndResetChallenge() {
        when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.encode("NovaSenha@123")).thenReturn("encoded-new-password");
        when(contractorRepository.save(any(Contractor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.resetPassword("joao@email.com", "123456", "NovaSenha@123", "NovaSenha@123");

        assertThat(user.getPassword()).isEqualTo("encoded-new-password");
        assertThat(user.getResetPasswordToken()).isNull();
        assertThat(user.getResetPasswordTokenExpiry()).isNull();
        assertThat(user.getResetPasswordAttempts()).isZero();
        assertThat(user.getResetPasswordLockUntil()).isNull();
        verify(passwordPolicyService).validateNewPassword("NovaSenha@123", "NovaSenha@123");
        verify(adminResetPasswordLinkService).clearAdminResetLink(user);
    }
}
