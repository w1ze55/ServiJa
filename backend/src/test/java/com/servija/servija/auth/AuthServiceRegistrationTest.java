package com.servija.servija.auth;

import com.servija.servija.dto.AuthResponse;
import com.servija.servija.dto.RegisterRequest;
import com.servija.servija.exception.DuplicateResourceException;
import com.servija.servija.model.Contractor;
import com.servija.servija.model.Freelancer;
import com.servija.servija.model.User;
import com.servija.servija.repository.ContractorRepository;
import com.servija.servija.repository.FreelancerRepository;
import com.servija.servija.repository.UserRepository;
import com.servija.servija.service.AuthService;
import com.servija.servija.service.AuditLogger;
import com.servija.servija.service.CustomUserDetailsService;
import com.servija.servija.service.EmailService;
import com.servija.servija.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceRegistrationTest {

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

    @InjectMocks
    private AuthService authService;

    private RegisterRequest contractorRequest;
    private RegisterRequest freelancerRequest;

    @BeforeEach
    void setUp() {
        contractorRequest = RegisterRequest.builder()
                .name("Joao Silva")
                .email("joao@email.com")
                .cpf("12345678901")
                .phone("11999999999")
                .birthDate(LocalDate.of(1995, 5, 10))
                .password("Senha@123")
                .userType(User.UserType.CONTRACTOR)
                .acceptTerms(true)
                .build();

        freelancerRequest = RegisterRequest.builder()
                .name("Maria Santos")
                .email("maria@email.com")
                .cpf("98765432100")
                .phone("11888888888")
                .birthDate(LocalDate.of(1998, 8, 20))
                .password("Senha@456")
                .userType(User.UserType.FREELANCER)
                .acceptTerms(true)
                .build();
    }

    @Test
    @DisplayName("Deve registrar um CONTRACTOR com sucesso e retornar token")
    void register_contractor_success() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByCpf(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");

        Contractor savedContractor = Contractor.builder()
                .id(1L)
                .name("Joao Silva")
                .email("joao@email.com")
                .cpf("12345678901")
                .phone("11999999999")
                .birthDate(LocalDate.of(1995, 5, 10))
                .password("encoded-password")
                .userType(User.UserType.CONTRACTOR)
                .emailVerified(false)
                .active(true)
                .totalContracts(0)
                .activeContracts(0)
                .completedContracts(0)
                .build();

        when(contractorRepository.save(any(Contractor.class))).thenReturn(savedContractor);

        UserDetails mockUserDetails = mock(UserDetails.class);
        when(userDetailsService.loadUserByUsername("joao@email.com")).thenReturn(mockUserDetails);
        when(jwtService.generateToken(mockUserDetails)).thenReturn("jwt-token-123");

        AuthResponse response = authService.register(contractorRequest);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt-token-123");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getUser().getName()).isEqualTo("Joao Silva");
        assertThat(response.getUser().getUserType()).isEqualTo(User.UserType.CONTRACTOR);
        assertThat(response.getUser().getEmailVerified()).isFalse();
        assertThat(response.getUser().getBirthDate()).isEqualTo(LocalDate.of(1995, 5, 10));
    }

    @Test
    @DisplayName("Deve registrar um FREELANCER com sucesso e retornar token")
    void register_freelancer_success() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByCpf(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");

        Freelancer savedFreelancer = Freelancer.builder()
                .id(2L)
                .name("Maria Santos")
                .email("maria@email.com")
                .cpf("98765432100")
                .phone("11888888888")
                .birthDate(LocalDate.of(1998, 8, 20))
                .password("encoded-password")
                .userType(User.UserType.FREELANCER)
                .emailVerified(false)
                .active(true)
                .totalReviews(0)
                .completedJobs(0)
                .activeJobs(0)
                .kycCompleted(false)
                .profileCompleted(false)
                .available(true)
                .build();

        when(freelancerRepository.save(any(Freelancer.class))).thenReturn(savedFreelancer);

        UserDetails mockUserDetails = mock(UserDetails.class);
        when(userDetailsService.loadUserByUsername("maria@email.com")).thenReturn(mockUserDetails);
        when(jwtService.generateToken(mockUserDetails)).thenReturn("jwt-token-456");

        AuthResponse response = authService.register(freelancerRequest);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt-token-456");
        assertThat(response.getUser().getUserType()).isEqualTo(User.UserType.FREELANCER);
        assertThat(response.getUser().getBirthDate()).isEqualTo(LocalDate.of(1998, 8, 20));
    }

    @Test
    @DisplayName("Deve lançar exceção ao cadastrar email duplicado")
    void register_duplicateEmail_throwsException() {
        when(userRepository.existsByEmail("joao@email.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(contractorRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("E-mail ja cadastrado");

        verify(contractorRepository, never()).save(any());
        verify(freelancerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lançar exceção ao cadastrar CPF duplicado")
    void register_duplicateCpf_throwsException() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByCpf("12345678901")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(contractorRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("CPF ja cadastrado");

        verify(contractorRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve enviar email de verificação após cadastro")
    void register_sendsVerificationEmail() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByCpf(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");

        Contractor saved = Contractor.builder()
                .id(1L)
                .name("Joao Silva")
                .email("joao@email.com")
                .cpf("12345678901")
                .phone("11999999999")
                .birthDate(LocalDate.of(1995, 5, 10))
                .password("encoded")
                .userType(User.UserType.CONTRACTOR)
                .emailVerified(false)
                .active(true)
                .totalContracts(0)
                .activeContracts(0)
                .completedContracts(0)
                .build();
        when(contractorRepository.save(any(Contractor.class))).thenReturn(saved);
        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(mock(UserDetails.class));
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn("token");

        authService.register(contractorRequest);

        verify(emailService).sendVerificationEmail(eq("joao@email.com"), eq("Joao Silva"), anyString());
    }

    @Test
    @DisplayName("Deve gerar código de verificação com 6 dígitos")
    void register_generatesValidVerificationCode() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByCpf(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");

        ArgumentCaptor<Contractor> captor = ArgumentCaptor.forClass(Contractor.class);

        Contractor saved = Contractor.builder()
                .id(1L)
                .name("Joao Silva")
                .email("joao@email.com")
                .cpf("12345678901")
                .phone("11999999999")
                .birthDate(LocalDate.of(1995, 5, 10))
                .password("encoded")
                .userType(User.UserType.CONTRACTOR)
                .emailVerified(false)
                .active(true)
                .totalContracts(0)
                .activeContracts(0)
                .completedContracts(0)
                .build();
        when(contractorRepository.save(any(Contractor.class))).thenReturn(saved);
        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(mock(UserDetails.class));
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn("token");

        authService.register(contractorRequest);

        verify(contractorRepository).save(captor.capture());
        String code = captor.getValue().getVerificationCode();

        assertThat(code).isNotNull();
        assertThat(code).hasSize(6);
        assertThat(code).matches("\\d{6}");
        assertThat(captor.getValue().getBirthDate()).isEqualTo(LocalDate.of(1995, 5, 10));
    }
}
