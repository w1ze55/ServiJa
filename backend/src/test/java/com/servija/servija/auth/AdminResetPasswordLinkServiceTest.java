package com.servija.servija.auth;

import com.servija.servija.dto.AdminResetPasswordLinkValidationResponse;
import com.servija.servija.model.User;
import com.servija.servija.repository.UserRepository;
import com.servija.servija.service.AdminResetPasswordLinkService;
import com.servija.servija.service.EmailService;
import com.servija.servija.service.PasswordPolicyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminResetPasswordLinkServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private EmailService emailService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private PasswordPolicyService passwordPolicyService;

    @InjectMocks
    private AdminResetPasswordLinkService adminResetPasswordLinkService;

    private User user;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(adminResetPasswordLinkService, "publicFrontendUrl", "http://localhost:5173");

        user = User.builder()
                .id(1L)
                .name("Joao Silva")
                .email("joao@email.com")
                .cpf("12345678901")
                .phone("11999999999")
                .password("encoded-password")
                .userType(User.UserType.CONTRACTOR)
                .activeRole(User.UserType.CONTRACTOR)
                .roles(Set.of(User.UserType.CONTRACTOR))
                .active(true)
                .resetPasswordToken("654321")
                .resetPasswordTokenExpiry(LocalDateTime.now().plusMinutes(30))
                .resetPasswordAttempts(3)
                .resetPasswordLockUntil(LocalDateTime.now().plusMinutes(5))
                .build();

        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.findByAdminResetPasswordTokenHash(anyString())).thenAnswer(invocation -> {
            String requestedHash = invocation.getArgument(0);
            return requestedHash.equals(user.getAdminResetPasswordTokenHash())
                    ? Optional.of(user)
                    : Optional.empty();
        });
    }

    @Test
    @DisplayName("sendResetLink deve enviar link válido e reemissão invalida o token anterior")
    void sendResetLink_reissueInvalidatesPreviousToken() {
        adminResetPasswordLinkService.sendResetLink(user);
        String firstHash = user.getAdminResetPasswordTokenHash();
        adminResetPasswordLinkService.sendResetLink(user);

        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService, times(2)).sendAdminPasswordResetLinkEmail(
                org.mockito.ArgumentMatchers.eq("joao@email.com"),
                org.mockito.ArgumentMatchers.eq("Joao Silva"),
                linkCaptor.capture());

        String firstToken = extractToken(linkCaptor.getAllValues().get(0));
        String secondToken = extractToken(linkCaptor.getAllValues().get(1));

        assertThat(firstToken).isNotEqualTo(secondToken);
        assertThat(user.getAdminResetPasswordTokenHash()).isNotEqualTo(firstHash);
        assertThat(user.getAdminResetPasswordTokenHash()).hasSize(64);
        assertThat(user.getAdminResetPasswordTokenExpiry()).isAfter(LocalDateTime.now().plusMinutes(29));

        assertThatThrownBy(() -> adminResetPasswordLinkService.validateToken(firstToken))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Link de redefinição inválido.");

        AdminResetPasswordLinkValidationResponse response =
                adminResetPasswordLinkService.validateToken(secondToken);

        assertThat(response.getEmailHint()).isEqualTo("joa***@email.com");
        assertThat(response.getUserType()).isEqualTo(User.UserType.CONTRACTOR);
    }

    @Test
    @DisplayName("validateToken deve falhar quando o link expira")
    void validateToken_expiredLink_throwsException() {
        String token = issueLinkAndExtractToken();
        user.setAdminResetPasswordTokenExpiry(LocalDateTime.now().minusMinutes(1));

        assertThatThrownBy(() -> adminResetPasswordLinkService.validateToken(token))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Este link de redefinição expirou.");
    }

    @Test
    @DisplayName("validateToken deve falhar quando o link já foi utilizado")
    void validateToken_usedLink_throwsException() {
        String token = issueLinkAndExtractToken();
        user.setAdminResetPasswordTokenExpiry(null);

        assertThatThrownBy(() -> adminResetPasswordLinkService.validateToken(token))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Este link de redefinição já foi utilizado.");
    }

    @Test
    @DisplayName("resetPassword deve consumir o link e limpar o fluxo por código")
    void resetPassword_consumesLinkAndClearsCodeChallenge() {
        String token = issueLinkAndExtractToken();
        when(passwordEncoder.encode("NovaSenha@123")).thenReturn("encoded-new-password");

        adminResetPasswordLinkService.resetPassword(token, "NovaSenha@123", "NovaSenha@123");

        assertThat(user.getPassword()).isEqualTo("encoded-new-password");
        assertThat(user.getAdminResetPasswordTokenHash()).isNotNull();
        assertThat(user.getAdminResetPasswordTokenExpiry()).isNull();
        assertThat(user.getResetPasswordToken()).isNull();
        assertThat(user.getResetPasswordTokenExpiry()).isNull();
        assertThat(user.getResetPasswordAttempts()).isZero();
        assertThat(user.getResetPasswordLockUntil()).isNull();
        verify(passwordPolicyService).validateNewPassword("NovaSenha@123", "NovaSenha@123");
    }

    private String issueLinkAndExtractToken() {
        ArgumentCaptor<String> linkCaptor = ArgumentCaptor.forClass(String.class);
        adminResetPasswordLinkService.sendResetLink(user);
        verify(emailService).sendAdminPasswordResetLinkEmail(
                org.mockito.ArgumentMatchers.eq("joao@email.com"),
                org.mockito.ArgumentMatchers.eq("Joao Silva"),
                linkCaptor.capture());
        return extractToken(linkCaptor.getValue());
    }

    private String extractToken(String link) {
        String query = URI.create(link).getQuery();
        return query.substring("resetToken=".length());
    }
}
