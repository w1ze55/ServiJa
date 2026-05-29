package com.servija.servija.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.servija.servija.dto.SupportRequestResponse;
import com.servija.servija.model.SupportRequest;
import com.servija.servija.model.User;
import com.servija.servija.repository.SupportRequestRepository;
import com.servija.servija.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SupportRequestServiceTest {

    @Mock
    private SupportRequestRepository supportRequestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

    private SupportRequestService supportRequestService;

    @BeforeEach
    void setUp() {
        supportRequestService = new SupportRequestService(
                supportRequestRepository,
                userRepository,
                new ObjectMapper(),
                emailService
        );
    }

    @Nested
    @DisplayName("approve")
    class Approve {

        @Test
        @DisplayName("deve aprovar solicitacao pendente")
        void shouldApprovePendingRequest() {
            SupportRequest supportRequest = buildSupportRequest(SupportRequest.Status.PENDING);
            User admin = buildAdmin();

            when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
            when(supportRequestRepository.findById(1L)).thenReturn(Optional.of(supportRequest));
            when(supportRequestRepository.save(any(SupportRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

            SupportRequestResponse response = supportRequestService.approve(1L, admin.getEmail(), "Tudo certo.");

            assertThat(response.getStatus()).isEqualTo(SupportRequest.Status.APPROVED);
            assertThat(response.getAdminResponse()).isEqualTo("Tudo certo.");
            assertThat(response.getAdminName()).isEqualTo(admin.getName());
            assertThat(response.getAnsweredAt()).isNotNull();
            verify(emailService).sendSupportRequestDecisionEmail(
                    supportRequest.getRequester().getEmail(),
                    supportRequest.getRequester().getName(),
                    supportRequest.getId(),
                    SupportRequest.Status.APPROVED,
                    "Tudo certo."
            );
        }

        @Test
        @DisplayName("deve impedir nova decisao apos solicitacao ja respondida")
        void shouldPreventSecondDecisionAfterRequestAnswered() {
            SupportRequest supportRequest = buildSupportRequest(SupportRequest.Status.REJECTED);
            User admin = buildAdmin();

            when(userRepository.findByEmail(admin.getEmail())).thenReturn(Optional.of(admin));
            when(supportRequestRepository.findById(1L)).thenReturn(Optional.of(supportRequest));

            assertThatThrownBy(() -> supportRequestService.approve(1L, admin.getEmail(), "Nova resposta"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("disponivel apenas para visualizacao");

            verify(supportRequestRepository, never()).save(any());
            verify(emailService, never()).sendSupportRequestDecisionEmail(any(), any(), any(), any(), any());
        }
    }

    private SupportRequest buildSupportRequest(SupportRequest.Status status) {
        User requester = User.builder()
                .id(2L)
                .name("Usuario Teste")
                .email("usuario@teste.com")
                .userType(User.UserType.CONTRACTOR)
                .build();

        return SupportRequest.builder()
                .id(1L)
                .requester(requester)
                .category(SupportRequest.Category.BUG)
                .description("Descricao do suporte")
                .status(status)
                .createdAt(LocalDateTime.now().minusDays(1))
                .updatedAt(LocalDateTime.now())
                .build();
    }

    private User buildAdmin() {
        return User.builder()
                .id(10L)
                .name("Admin Teste")
                .email("admin@teste.com")
                .userType(User.UserType.ADMIN)
                .build();
    }
}
