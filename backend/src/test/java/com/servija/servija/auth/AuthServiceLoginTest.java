package com.servija.servija.auth;

import com.servija.servija.dto.AuthResponse;
import com.servija.servija.dto.LoginRequest;
import com.servija.servija.exception.InvalidCredentialsException;
import com.servija.servija.exception.ResourceNotFoundException;
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
import com.servija.servija.service.UserRoleCompatibilityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceLoginTest {

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
    @Mock private PasswordPolicyService passwordPolicyService;
    @Mock private AdminResetPasswordLinkService adminResetPasswordLinkService;
    @Mock private UserRoleCompatibilityService userRoleCompatibilityService;

    @InjectMocks
    private AuthService authService;

    private LoginRequest loginRequest;
    private User activeUser;

    @BeforeEach
    void setUp() {
        loginRequest = LoginRequest.builder()
                .email("joao@email.com")
                .password("Senha@123")
                .build();

        activeUser = buildUser(1L, "joao@email.com", User.UserType.CONTRACTOR, true);
        lenient().when(userRoleCompatibilityService.repairIfNeeded(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("Deve fazer login com credenciais válidas e retornar token")
    void login_validCredentials_returnsToken() {
        UserDetails mockUserDetails = mock(UserDetails.class);

        when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(activeUser));
        when(userDetailsService.loadUserByUsername("joao@email.com")).thenReturn(mockUserDetails);
        when(jwtService.generateToken(any(), any(UserDetails.class))).thenReturn("jwt-token-login");

        AuthResponse response = authService.login(loginRequest);

        assertThat(response.getToken()).isEqualTo("jwt-token-login");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getUser().getEmail()).isEqualTo("joao@email.com");
        assertThat(response.getUser().getActive()).isTrue();
        assertThat(response.getUser().getActiveRole()).isEqualTo(User.UserType.CONTRACTOR);
        assertThat(response.getUser().getRoles()).containsExactly(User.UserType.CONTRACTOR);
    }

    @Test
    @DisplayName("Deve lançar exceção quando usuário não encontrado")
    void login_userNotFound_throwsException() {
        when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Usuário não encontrado");
    }

    @Test
    @DisplayName("Deve lançar exceção quando conta está desativada")
    void login_inactiveAccount_throwsException() {
        activeUser.setActive(false);
        when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(activeUser));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Conta suspensa");
    }

    @Test
    @DisplayName("Deve chamar authenticationManager.authenticate com credenciais corretas")
    void login_callsAuthenticationManager() {
        when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(activeUser));
        when(userDetailsService.loadUserByUsername("joao@email.com")).thenReturn(mock(UserDetails.class));
        when(jwtService.generateToken(any(), any(UserDetails.class))).thenReturn("token");

        authService.login(loginRequest);

        verify(authenticationManager).authenticate(
                argThat(auth -> {
                    UsernamePasswordAuthenticationToken token = (UsernamePasswordAuthenticationToken) auth;
                    return token.getPrincipal().equals("joao@email.com")
                            && token.getCredentials().equals("Senha@123");
                })
        );
    }

    @Test
    @DisplayName("Login de app deve rejeitar usuário ADMIN")
    void login_adminUser_rejected() {
        User admin = buildUser(99L, "joao@email.com", User.UserType.ADMIN, true);
        when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Credenciais inválidas");
    }

    @Test
    @DisplayName("Deve reparar active role legado antes de validar login de app")
    void login_legacyActiveRoleAdmin_repairsAndAllowsAppLogin() {
        User legacyUser = User.builder()
                .id(1L)
                .name("Joao Silva")
                .email("joao@email.com")
                .cpf("12345678901")
                .phone("11999999999")
                .password("encoded-password")
                .userType(User.UserType.CONTRACTOR)
                .activeRole(User.UserType.ADMIN)
                .emailVerified(true)
                .active(true)
                .build();
        UserDetails mockUserDetails = mock(UserDetails.class);

        when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(legacyUser));
        when(userDetailsService.loadUserByUsername("joao@email.com")).thenReturn(mockUserDetails);
        when(jwtService.generateToken(any(), any(UserDetails.class))).thenReturn("jwt-token-login");

        AuthResponse response = authService.login(loginRequest);

        assertThat(response.getToken()).isEqualTo("jwt-token-login");
        assertThat(response.getUser().getActiveRole()).isEqualTo(User.UserType.CONTRACTOR);
        assertThat(response.getUser().getRoles()).containsExactly(User.UserType.CONTRACTOR);
        verify(userRoleCompatibilityService).repairIfNeeded(legacyUser);
    }

    @Test
    @DisplayName("Login admin deve aceitar apenas ADMIN")
    void loginAdmin_nonAdmin_rejected() {
        when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(activeUser));

        assertThatThrownBy(() -> authService.loginAdmin(loginRequest))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Credenciais inválidas");
    }

    @Test
    @DisplayName("Login admin deve retornar token para ADMIN ativo")
    void loginAdmin_validAdmin_returnsToken() {
        User admin = buildUser(2L, "joao@email.com", User.UserType.ADMIN, true);
        UserDetails mockUserDetails = mock(UserDetails.class);

        when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(admin));
        when(userDetailsService.loadUserByUsername("joao@email.com")).thenReturn(mockUserDetails);
        when(jwtService.generateToken(any(), any(UserDetails.class))).thenReturn("jwt-admin");

        AuthResponse response = authService.loginAdmin(loginRequest);

        assertThat(response.getToken()).isEqualTo("jwt-admin");
        assertThat(response.getUser().getUserType()).isEqualTo(User.UserType.ADMIN);
    }

    private User buildUser(Long id, String email, User.UserType role, boolean active) {
        return User.builder()
                .id(id)
                .name("Joao Silva")
                .email(email)
                .cpf("12345678901")
                .phone("11999999999")
                .password("encoded-password")
                .userType(role)
                .activeRole(role)
                .roles(Set.of(role))
                .emailVerified(true)
                .active(active)
                .build();
    }
}
