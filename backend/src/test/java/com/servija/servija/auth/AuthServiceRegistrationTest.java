package com.servija.servija.auth;

import com.servija.servija.dto.AddressRequest;
import com.servija.servija.dto.AuthResponse;
import com.servija.servija.dto.RegisterRequest;
import com.servija.servija.exception.DuplicateResourceException;
import com.servija.servija.model.Contractor;
import com.servija.servija.model.Freelancer;
import com.servija.servija.model.User;
import com.servija.servija.repository.ContractorRepository;
import com.servija.servija.repository.FreelancerRepository;
import com.servija.servija.repository.UserRepository;
import com.servija.servija.service.AddressService;
import com.servija.servija.service.AuditLogger;
import com.servija.servija.service.AuthService;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceRegistrationTest {

    @Mock private UserRepository userRepository;
    @Mock private ContractorRepository contractorRepository;
    @Mock private FreelancerRepository freelancerRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private CustomUserDetailsService userDetailsService;
    @Mock private EmailService emailService;
    @Mock private AuditLogger auditLogger;
    @Mock private AddressService addressService;

    @InjectMocks
    private AuthService authService;
    private RegisterRequest contractorRequest;
    private RegisterRequest freelancerRequest;

    @BeforeEach
    void setUp() {
        contractorRequest = buildRequest(
                "Joao Silva",
                "joao@email.com",
                "12345678901",
                "11999999999",
                LocalDate.of(1995, 5, 10),
                "Senha@123",
                User.UserType.CONTRACTOR);

        freelancerRequest = buildRequest(
                "Maria Santos",
                "maria@email.com",
                "98765432100",
                "11888888888",
                LocalDate.of(1998, 8, 20),
                "Senha@456",
                User.UserType.FREELANCER);
    }

    @Test
    @DisplayName("Deve registrar um CONTRACTOR com sucesso")
    void register_contractor_success() {
        setupBaseRegistrationMocks();
        when(contractorRepository.findById(anyLong())).thenReturn(Optional.empty());
        when(contractorRepository.save(any(Contractor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.register(contractorRequest);

        assertThat(response.getToken()).isNull();
        assertThat(response.getUser().getName()).isEqualTo("Joao Silva");
        assertThat(response.getUser().getUserType()).isEqualTo(User.UserType.CONTRACTOR);
        assertThat(response.getUser().getActiveRole()).isEqualTo(User.UserType.CONTRACTOR);
        assertThat(response.getUser().getRoles()).containsExactly(User.UserType.CONTRACTOR);
        assertThat(response.getUser().getBirthDate()).isEqualTo(LocalDate.of(1995, 5, 10));
        verify(contractorRepository).save(any(Contractor.class));
        verify(freelancerRepository, never()).save(any(Freelancer.class));
    }

    @Test
    @DisplayName("Deve registrar um FREELANCER com sucesso")
    void register_freelancer_success() {
        setupBaseRegistrationMocks();
        when(freelancerRepository.findById(anyLong())).thenReturn(Optional.empty());
        when(freelancerRepository.save(any(Freelancer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AuthResponse response = authService.register(freelancerRequest);

        assertThat(response.getToken()).isNull();
        assertThat(response.getUser().getUserType()).isEqualTo(User.UserType.FREELANCER);
        assertThat(response.getUser().getActiveRole()).isEqualTo(User.UserType.FREELANCER);
        assertThat(response.getUser().getRoles()).containsExactly(User.UserType.FREELANCER);
        assertThat(response.getUser().getBirthDate()).isEqualTo(LocalDate.of(1998, 8, 20));
        verify(freelancerRepository).save(any(Freelancer.class));
        verify(contractorRepository, never()).save(any(Contractor.class));
    }

    @Test
    @DisplayName("Deve lancar excecao ao cadastrar email duplicado")
    void register_duplicateEmail_throwsException() {
        when(userRepository.existsByEmail("joao@email.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(contractorRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("E-mail ja cadastrado");

        verify(contractorRepository, never()).save(any());
        verify(freelancerRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve lancar excecao ao cadastrar CPF duplicado")
    void register_duplicateCpf_throwsException() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByCpf("12345678901")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(contractorRequest))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessage("CPF ja cadastrado");

        verify(contractorRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve enviar email de verificacao apos cadastro")
    void register_sendsVerificationEmail() {
        setupBaseRegistrationMocks();
        when(contractorRepository.findById(anyLong())).thenReturn(Optional.empty());
        when(contractorRepository.save(any(Contractor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.register(contractorRequest);

        verify(emailService).sendVerificationEmail(eq("joao@email.com"), eq("Joao Silva"), anyString());
    }

    @Test
    @DisplayName("Deve gerar codigo de verificacao com 6 digitos")
    void register_generatesValidVerificationCode() {
        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        setupBaseRegistrationMocks();
        when(contractorRepository.findById(anyLong())).thenReturn(Optional.empty());
        when(contractorRepository.save(any(Contractor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.register(contractorRequest);

        verify(userRepository, org.mockito.Mockito.atLeastOnce()).save(userCaptor.capture());
        User firstSavedUser = userCaptor.getAllValues().get(0);
        assertThat(firstSavedUser.getVerificationCode()).isNotNull().hasSize(6).matches("\\d{6}");
        assertThat(firstSavedUser.getBirthDate()).isEqualTo(LocalDate.of(1995, 5, 10));
    }

    private void setupBaseRegistrationMocks() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByCpf(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            if (user.getId() == null) {
                user.setId(user.getEmail().equals("joao@email.com") ? 1L : 2L);
            }
            if (user.getActiveRole() == null) {
                user.setActiveRole(user.getUserType());
            }
            if (user.getRoles() == null || user.getRoles().isEmpty()) {
                user.setRoles(Set.of(user.getUserType()));
            }
            return user;
        });
    }

    private RegisterRequest buildRequest(
            String name,
            String email,
            String cpf,
            String phone,
            LocalDate birthDate,
            String password,
            User.UserType role) {
        return RegisterRequest.builder()
                .name(name)
                .email(email)
                .cpf(cpf)
                .phone(phone)
                .birthDate(birthDate)
                .password(password)
                .userType(role)
                .address(AddressRequest.builder()
                        .label("Principal")
                        .street("Rua das Flores")
                        .number("123")
                        .neighborhood("Centro")
                        .city("Sao Paulo")
                        .state("SP")
                        .zipCode("01001-000")
                        .isDefault(true)
                        .build())
                .acceptTerms(true)
                .build();
    }
}
