package com.servija.servija.service;

import com.servija.servija.dto.AdjustmentStatusUpdate;
import com.servija.servija.dto.DeliveryAdjustmentRequest;
import com.servija.servija.model.DeliveryAdjustment;
import com.servija.servija.model.ServiceRequest;
import com.servija.servija.model.User;
import com.servija.servija.repository.DeliveryAdjustmentRepository;
import com.servija.servija.repository.ServiceRequestRepository;
import com.servija.servija.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DeliveryAdjustmentServiceTest {

    @Mock
    private DeliveryAdjustmentRepository adjustmentRepository;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ServiceRequestHistoryService historyService;
    @Mock
    private WebSocketService webSocketService;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AuditLogger auditLogger;

    @InjectMocks
    private DeliveryAdjustmentService service;

    @Test
    @DisplayName("requestAdjustment cria ajuste e notifica o freelancer")
    void requestAdjustmentCreatesAndNotifies() {
        ServiceRequest request = deliveredRequest();
        User contractor = request.getContractor();
        when(serviceRequestRepository.findById(10L)).thenReturn(Optional.of(request));
        when(userRepository.findByEmail(contractor.getEmail())).thenReturn(Optional.of(contractor));
        when(adjustmentRepository.existsByServiceRequestId(10L)).thenReturn(false);
        when(adjustmentRepository.save(any(DeliveryAdjustment.class))).thenAnswer(invocation -> {
            DeliveryAdjustment adjustment = invocation.getArgument(0);
            adjustment.setId(99L);
            return adjustment;
        });

        var response = service.requestAdjustment(10L, DeliveryAdjustmentRequest.builder().reason("Ajuste fino").build(), contractor.getEmail());

        assertThat(response.getId()).isEqualTo(99L);
        verify(historyService).record(any(), any(), any(), any());
        verify(webSocketService).notifyServiceRequestUpdate(
                request.getFreelancer().getEmail(),
                10L,
                "ADJUSTMENTS_REQUESTED",
                contractor.getName() + " solicitou ajustes na entrega",
                contractor.getName());
        verify(notificationService).notifyAdjustmentsRequested(request, contractor);
    }

    @Test
    @DisplayName("requestAdjustment rejeita solicitacao quando o servico nao esta entregue")
    void requestAdjustmentRejectsWrongStatus() {
        ServiceRequest request = deliveredRequest();
        request.setStatus(ServiceRequest.Status.IN_PROGRESS);
        when(serviceRequestRepository.findById(10L)).thenReturn(Optional.of(request));
        when(userRepository.findByEmail(request.getContractor().getEmail())).thenReturn(Optional.of(request.getContractor()));

        assertThatThrownBy(() -> service.requestAdjustment(10L, DeliveryAdjustmentRequest.builder().reason("Ajuste").build(), request.getContractor().getEmail()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("entregue");
    }

    @Test
    @DisplayName("deleteAdjustment rejeita ajuste nao pendente")
    void deleteAdjustmentRejectsNonPendingAdjustment() {
        DeliveryAdjustment adjustment = DeliveryAdjustment.builder()
                .id(1L)
                .serviceRequest(deliveredRequest())
                .requestedBy(deliveredRequest().getContractor())
                .status(DeliveryAdjustment.Status.COMPLETED)
                .build();
        when(adjustmentRepository.findById(1L)).thenReturn(Optional.of(adjustment));

        assertThatThrownBy(() -> service.deleteAdjustment(1L, "client@test.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("pendentes");
    }

    private ServiceRequest deliveredRequest() {
        User contractor = User.builder()
                .id(1L)
                .name("Cliente")
                .email("client@test.com")
                .cpf("123")
                .phone("999")
                .password("pwd")
                .userType(User.UserType.CONTRACTOR)
                .activeRole(User.UserType.CONTRACTOR)
                .roles(Set.of(User.UserType.CONTRACTOR))
                .build();
        User freelancer = User.builder()
                .id(2L)
                .name("Freela")
                .email("freela@test.com")
                .cpf("456")
                .phone("888")
                .password("pwd")
                .userType(User.UserType.FREELANCER)
                .activeRole(User.UserType.FREELANCER)
                .roles(Set.of(User.UserType.FREELANCER))
                .build();
        return ServiceRequest.builder()
                .id(10L)
                .contractor(contractor)
                .freelancer(freelancer)
                .status(ServiceRequest.Status.DELIVERED)
                .build();
    }
}
