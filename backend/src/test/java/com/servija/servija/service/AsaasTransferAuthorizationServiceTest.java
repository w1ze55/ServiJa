package com.servija.servija.service;

import com.servija.servija.model.WithdrawalRequest;
import com.servija.servija.repository.WithdrawalRequestRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsaasTransferAuthorizationServiceTest {

    @Mock
    private WithdrawalRequestRepository withdrawalRepository;

    @InjectMocks
    private AsaasTransferAuthorizationService service;

    @Test
    @DisplayName("Deve aprovar transferencia conhecida do Asaas")
    void shouldApproveRecognizedTransfer() {
        WithdrawalRequest withdrawal = WithdrawalRequest.builder()
                .id(10L)
                .amount(new BigDecimal("25.00"))
                .withdrawalMethod(WithdrawalRequest.WithdrawalMethod.PIX)
                .asaasTransferId("trf_123")
                .build();

        when(withdrawalRepository.findFirstByAsaasTransferId("trf_123"))
                .thenReturn(Optional.of(withdrawal));

        Map<String, String> response = service.authorizeTransfer(Map.of(
                "type", "TRANSFER",
                "transfer", Map.of(
                        "id", "trf_123",
                        "value", new BigDecimal("25.00"),
                        "operationType", "PIX"
                )
        ));

        assertThat(response).containsEntry("status", "APPROVED");
        assertThat(response).doesNotContainKey("refuseReason");
    }

    @Test
    @DisplayName("Deve recusar transferencia nao encontrada")
    void shouldRefuseUnknownTransfer() {
        when(withdrawalRepository.findFirstByAsaasTransferId("trf_404"))
                .thenReturn(Optional.empty());

        Map<String, String> response = service.authorizeTransfer(Map.of(
                "type", "TRANSFER",
                "transfer", Map.of(
                        "id", "trf_404",
                        "value", new BigDecimal("25.00"),
                        "operationType", "PIX"
                )
        ));

        assertThat(response).containsEntry("status", "REFUSED");
        assertThat(response.get("refuseReason")).contains("não encontrada");
    }

    @Test
    @DisplayName("Deve recusar transferencia com valor divergente")
    void shouldRefuseTransferWithDifferentValue() {
        WithdrawalRequest withdrawal = WithdrawalRequest.builder()
                .id(10L)
                .amount(new BigDecimal("25.00"))
                .withdrawalMethod(WithdrawalRequest.WithdrawalMethod.PIX)
                .asaasTransferId("trf_123")
                .build();

        when(withdrawalRepository.findFirstByAsaasTransferId("trf_123"))
                .thenReturn(Optional.of(withdrawal));

        Map<String, String> response = service.authorizeTransfer(Map.of(
                "type", "TRANSFER",
                "transfer", Map.of(
                        "id", "trf_123",
                        "value", new BigDecimal("30.00"),
                        "operationType", "PIX"
                )
        ));

        assertThat(response).containsEntry("status", "REFUSED");
        assertThat(response.get("refuseReason")).contains("divergente");
    }
}
