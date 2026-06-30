package com.servija.servija.service;

import com.servija.servija.dto.ServiceRequestHistoryResponse;
import com.servija.servija.model.ServiceRequest;
import com.servija.servija.model.ServiceRequestHistory;
import com.servija.servija.model.User;
import com.servija.servija.repository.ServiceRequestHistoryRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceRequestHistoryServiceTest {

    @Mock
    private ServiceRequestHistoryRepository historyRepository;

    @InjectMocks
    private ServiceRequestHistoryService historyService;

    @Test
    @DisplayName("Deve traduzir status de ajuste salvo em inglês ao carregar histórico")
    void getHistory_translatesStoredAdjustmentStatusDetails() {
        ServiceRequest request = ServiceRequest.builder()
                .id(15L)
                .build();

        User user = User.builder()
                .id(7L)
                .name("Caue Peres")
                .email("caue@servija.com")
                .cpf("12345678901")
                .phone("11999999999")
                .password("senha")
                .userType(User.UserType.FREELANCER)
                .build();

        ServiceRequestHistory history = ServiceRequestHistory.builder()
                .id(3L)
                .serviceRequest(request)
                .user(user)
                .operationType(ServiceRequestHistory.OperationType.ADJUSTMENT_STATUS_UPDATED)
                .createdAt(LocalDateTime.of(2026, 3, 22, 0, 29))
                .details("Status do ajuste atualizado para: COMPLETED")
                .build();

        when(historyRepository.findByServiceRequestIdOrderByCreatedAtDesc(15L)).thenReturn(List.of(history));

        List<ServiceRequestHistoryResponse> response = historyService.getHistory(15L);

        assertThat(response).singleElement().satisfies(entry -> {
            assertThat(entry.getOperationKey()).isEqualTo("ADJUSTMENT_STATUS_UPDATED");
            assertThat(entry.getDetails()).isEqualTo("Ajustes finalizados");
            assertThat(entry.getUserName()).isEqualTo("Caue Peres");
        });
    }
}
