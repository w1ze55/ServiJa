package com.servija.servija.service;

import com.servija.servija.dto.PaymentRequest;
import com.servija.servija.dto.PaymentResponse;
import com.servija.servija.model.Payment;
import com.servija.servija.model.ServiceRequest;
import com.servija.servija.model.User;
import com.servija.servija.repository.PaymentRepository;
import com.servija.servija.repository.PaymentStatusHistoryRepository;
import com.servija.servija.repository.ServiceRequestRepository;
import com.servija.servija.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private PaymentStatusHistoryRepository paymentStatusHistoryRepository;

    @Mock
    private ServiceRequestRepository serviceRequestRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AsaasService asaasService;

    @Mock
    private FreelancerWalletService walletService;

    @Mock
    private ServiceRequestHistoryService serviceRequestHistoryService;

    @Mock
    private WebSocketService webSocketService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    @DisplayName("Deve rejeitar pagamento com valor divergente do valor aceito")
    void createPayment_rejectsMismatchedAmount() {
        User contractor = buildUser(1L, "cliente@servija.com", User.UserType.CONTRACTOR);
        ServiceRequest serviceRequest = buildAcceptedRequest(contractor, new BigDecimal("50.00"), false);

        when(userRepository.findByEmail("cliente@servija.com")).thenReturn(Optional.of(contractor));
        when(serviceRequestRepository.findById(10L)).thenReturn(Optional.of(serviceRequest));
        when(paymentRepository.existsByServiceRequest(serviceRequest)).thenReturn(false);

        PaymentRequest request = PaymentRequest.builder()
                .serviceRequestId(10L)
                .paymentMethod("pix")
                .amount(new BigDecimal("45.00"))
                .build();

        assertThatThrownBy(() -> paymentService.createPayment("cliente@servija.com", request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("O valor do pagamento não confere");

        verify(asaasService, never()).getOrCreateCustomer(any(), any(), any());
    }

    @Test
    @DisplayName("Deve bloquear pagamento novo abaixo de R$ 20,00 quando nao for legado")
    void createPayment_rejectsLowValueWhenNotLegacy() {
        User contractor = buildUser(1L, "cliente@servija.com", User.UserType.CONTRACTOR);
        ServiceRequest serviceRequest = buildAcceptedRequest(contractor, new BigDecimal("19.99"), false);

        when(userRepository.findByEmail("cliente@servija.com")).thenReturn(Optional.of(contractor));
        when(serviceRequestRepository.findById(10L)).thenReturn(Optional.of(serviceRequest));
        when(paymentRepository.existsByServiceRequest(serviceRequest)).thenReturn(false);

        PaymentRequest request = PaymentRequest.builder()
                .serviceRequestId(10L)
                .paymentMethod("wallet")
                .amount(new BigDecimal("19.99"))
                .build();

        assertThatThrownBy(() -> paymentService.createPayment("cliente@servija.com", request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("O valor mínimo para pagamento é R$ 20,00");

        verify(asaasService, never()).getOrCreateCustomer(any(), any(), any());
    }

    @Test
    @DisplayName("Deve permitir pagamento legado abaixo de R$ 20,00")
    void createPayment_allowsLegacyLowValue() {
        User contractor = buildUser(1L, "cliente@servija.com", User.UserType.CONTRACTOR);
        ServiceRequest serviceRequest = buildAcceptedRequest(contractor, new BigDecimal("19.99"), true);

        when(userRepository.findByEmail("cliente@servija.com")).thenReturn(Optional.of(contractor));
        when(serviceRequestRepository.findById(10L)).thenReturn(Optional.of(serviceRequest));
        when(paymentRepository.existsByServiceRequest(serviceRequest)).thenReturn(false);
        when(asaasService.getOrCreateCustomer(any(), any(), any())).thenReturn("cus_123");
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(77L);
            return payment;
        });

        PaymentRequest request = PaymentRequest.builder()
                .serviceRequestId(10L)
                .paymentMethod("wallet")
                .amount(new BigDecimal("19.99"))
                .cpf("12345678901")
                .build();

        PaymentResponse response = paymentService.createPayment("cliente@servija.com", request);

        assertThat(response.getId()).isEqualTo(77L);
        assertThat(response.getTotalAmount()).isEqualByComparingTo("19.99");
        assertThat(response.getStatus()).isEqualTo("PROCESSING");
        verify(asaasService).getOrCreateCustomer(contractor.getEmail(), contractor.getName(), "12345678901");
    }

    private ServiceRequest buildAcceptedRequest(User contractor, BigDecimal responseValue, boolean legacyAllowed) {
        User freelancer = buildUser(2L, "freela@servija.com", User.UserType.FREELANCER);

        return ServiceRequest.builder()
                .id(10L)
                .contractor(contractor)
                .freelancer(freelancer)
                .serviceName("Instalacao eletrica")
                .category("Eletricista")
                .description("Servico residencial")
                .deadline("5 dias")
                .workModel(ServiceRequest.WorkModel.PRESENCIAL)
                .status(ServiceRequest.Status.ACCEPTED)
                .responseValue(responseValue)
                .legacyLowValueAllowed(legacyAllowed)
                .build();
    }

    private User buildUser(Long id, String email, User.UserType userType) {
        return User.builder()
                .id(id)
                .name(userType == User.UserType.CONTRACTOR ? "Cliente" : "Freelancer")
                .email(email)
                .cpf("12345678901")
                .phone("11999999999")
                .password("senha")
                .userType(userType)
                .build();
    }
}
