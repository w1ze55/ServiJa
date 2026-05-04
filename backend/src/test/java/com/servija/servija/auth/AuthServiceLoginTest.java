package com.servija.servija.auth;

import com.servija.servija.dto.AuthResponse;
import com.servija.servija.dto.LoginRequest;
import com.servija.servija.exception.InvalidCredentialsException;
import com.servija.servija.exception.ResourceNotFoundException;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceLoginTest {

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

    private LoginRequest loginRequest;
    private Contractor activeUser;

    @BeforeEach
    void setUp() {
        loginRequest = LoginRequest.builder()
                .email("joao@email.com")
                .password("Senha@123")
                .build();

        activeUser = Contractor.builder()
                .id(1L)
                .name("João Silva")
                .email("joao@email.com")
                .cpf("12345678901")
                .phone("11999999999")
                .password("encoded-password")
                .userType(User.UserType.CONTRACTOR)
                .emailVerified(true)
                .active(true)
                .totalContracts(0)
                .activeContracts(0)
                .completedContracts(0)
                .build();
    }

    @Test
    @DisplayName("Deve fazer login com credenciais válidas e retornar token")
    void login_validCredentials_returnsToken() {
        when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(activeUser));

        UserDetails mockUserDetails = mock(UserDetails.class);
        when(userDetailsService.loadUserByUsername("joao@email.com")).thenReturn(mockUserDetails);
        when(jwtService.generateToken(mockUserDetails)).thenReturn("jwt-token-login");

        AuthResponse response = authService.login(loginRequest);

        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("jwt-token-login");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getUser().getEmail()).isEqualTo("joao@email.com");
        assertThat(response.getUser().getActive()).isTrue();
    }

    @Test
    @DisplayName("Deve lançar exceção quando usuário não encontrado")
    void login_userNotFound_throwsException() {
        when(authenticationManager.authenticate(any())).thenReturn(null);
        when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Usuario nao encontrado");
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
        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(mock(UserDetails.class));
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn("token");

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
    @DisplayName("Login de app deve rejeitar usuario ADMIN")
    void login_adminUser_rejected() {
        User admin = User.builder()
                .id(99L)
                .name("Admin")
                .email("joao@email.com")
                .cpf("12345678901")
                .phone("11999999999")
                .password("encoded-password")
                .userType(User.UserType.ADMIN)
                .emailVerified(true)
                .active(true)
                .build();

        when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> authService.login(loginRequest))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Credenciais invalidas");
    }

    @Test
    @DisplayName("Login admin deve aceitar apenas ADMIN")
    void loginAdmin_nonAdmin_rejected() {
        when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(activeUser));

        assertThatThrownBy(() -> authService.loginAdmin(loginRequest))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Credenciais invalidas");
    }

    @Test
    @DisplayName("Login admin deve retornar token para ADMIN ativo")
    void loginAdmin_validAdmin_returnsToken() {
        User admin = User.builder()
                .id(2L)
                .name("Admin")
                .email("joao@email.com")
                .cpf("10987654321")
                .phone("11888888888")
                .password("encoded-password")
                .userType(User.UserType.ADMIN)
                .emailVerified(true)
                .active(true)
                .build();

        when(userRepository.findByEmail("joao@email.com")).thenReturn(Optional.of(admin));

        UserDetails mockUserDetails = mock(UserDetails.class);
        when(userDetailsService.loadUserByUsername("joao@email.com")).thenReturn(mockUserDetails);
        when(jwtService.generateToken(mockUserDetails)).thenReturn("jwt-admin");

        AuthResponse response = authService.loginAdmin(loginRequest);

        assertThat(response.getToken()).isEqualTo("jwt-admin");
        assertThat(response.getUser().getUserType()).isEqualTo(User.UserType.ADMIN);
    }
}
