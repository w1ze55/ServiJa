package com.servija.servija.controller;

import com.servija.servija.service.AsaasTransferAuthorizationService;
import com.servija.servija.service.AsaasWebhookService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsaasWebhookControllerTest {

    @Mock
    private AsaasWebhookService webhookService;

    @Mock
    private AsaasTransferAuthorizationService transferAuthorizationService;

    @InjectMocks
    private AsaasWebhookController controller;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "webhookToken", "token_teste");
    }

    @Test
    @DisplayName("Deve responder REFUSED no webhook de autorizacao quando token for invalido")
    void shouldRefuseTransferAuthorizationWhenTokenIsInvalid() {
        ResponseEntity<Map<String, String>> response = controller.handleTransferAuthorization(
                Map.of("type", "TRANSFER"),
                "token_errado"
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).containsEntry("status", "REFUSED");
        assertThat(response.getBody()).containsEntry("refuseReason", "Token invalido");
        verifyNoInteractions(transferAuthorizationService);
    }

    @Test
    @DisplayName("Deve encaminhar autorizacao valida ao servico")
    void shouldDelegateTransferAuthorizationToService() {
        when(transferAuthorizationService.authorizeTransfer(Map.of("type", "TRANSFER")))
                .thenReturn(Map.of("status", "APPROVED"));

        ResponseEntity<Map<String, String>> response = controller.handleTransferAuthorization(
                Map.of("type", "TRANSFER"),
                "token_teste"
        );

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).containsEntry("status", "APPROVED");
    }
}
