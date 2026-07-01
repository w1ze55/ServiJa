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
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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

        mockBaseLookups(contractor, serviceRequest);

        PaymentRequest request = PaymentRequest.builder()
                .serviceRequestId(10L)
                .paymentMethod("pix")
                .amount(new BigDecimal("45.00"))
                .build();

        assertThatThrownBy(() -> paymentService.createPayment("cliente@servija.com", request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("O valor do pagamento");

        verify(asaasService, never()).getOrCreateCustomer(any(), any(), any());
    }

    @Test
    @DisplayName("Deve bloquear pagamento abaixo de R$ 40,00")
    void createPayment_rejectsLowValue() {
        User contractor = buildUser(1L, "cliente@servija.com", User.UserType.CONTRACTOR);
        ServiceRequest serviceRequest = buildAcceptedRequest(contractor, new BigDecimal("39.99"), true);

        mockBaseLookups(contractor, serviceRequest);

        PaymentRequest request = PaymentRequest.builder()
                .serviceRequestId(10L)
                .paymentMethod("wallet")
                .amount(new BigDecimal("39.99"))
                .build();

        assertThatThrownBy(() -> paymentService.createPayment("cliente@servija.com", request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("O valor mínimo para pagamento é R$ 40,00");

        verify(asaasService, never()).getOrCreateCustomer(any(), any(), any());
    }

    @Test
    @DisplayName("Deve criar pagamento PIX com taxa fixa da Asaas e ignorar parcelas")
    void createPayment_createsPixPaymentWithFixedGatewayFee() {
        User contractor = buildUser(1L, "cliente@servija.com", User.UserType.CONTRACTOR);
        ServiceRequest serviceRequest = buildAcceptedRequest(contractor, new BigDecimal("100.00"), false);

        mockBaseLookups(contractor, serviceRequest);
        when(asaasService.getOrCreateCustomer(anyString(), anyString(), anyString())).thenReturn("cus_123");
        when(asaasService.createPayment(anyString(), any(BigDecimal.class), any(), anyString(), anyString()))
                .thenReturn(Map.of("id", "pay_123"));
        when(asaasService.getPixQrCode("pay_123"))
                .thenReturn(Map.of("encodedImage", "qr-image", "payload", "pix-copy-paste"));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(77L);
            return payment;
        });

        PaymentRequest request = PaymentRequest.builder()
                .serviceRequestId(10L)
                .paymentMethod("pix")
                .amount(new BigDecimal("100.00"))
                .cpf("12345678901")
                .cardData(PaymentRequest.CardData.builder().installmentCount(6).build())
                .build();

        PaymentResponse response = paymentService.createPayment("cliente@servija.com", request);

        assertThat(response.getId()).isEqualTo(77L);
        assertThat(response.getAmount()).isEqualByComparingTo("95.00");
        assertThat(response.getPlatformFee()).isEqualByComparingTo("5.00");
        assertThat(response.getInstallmentFee()).isEqualByComparingTo("0.00");
        assertThat(response.getGatewayFee()).isEqualByComparingTo("1.99");
        assertThat(response.getTotalAmount()).isEqualByComparingTo("100.00");
        assertThat(response.getInstallmentCount()).isNull();
        assertThat(response.getPaymentMethod()).isEqualTo("pix");
        assertThat(response.getStatus()).isEqualTo("PROCESSING");
        verify(walletService, never()).addPendingBalance(any(), any());
    }

    @Test
    @DisplayName("Deve criar pagamento no cartão à vista com taxa de 2,99 por cento")
    void createPayment_createsOneTimeCreditCardPayment() {
        User contractor = buildUser(1L, "cliente@servija.com", User.UserType.CONTRACTOR);
        ServiceRequest serviceRequest = buildAcceptedRequest(contractor, new BigDecimal("100.00"), false);

        mockBaseLookups(contractor, serviceRequest);
        when(asaasService.getOrCreateCustomer(anyString(), anyString(), anyString())).thenReturn("cus_123");
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(78L);
            return payment;
        });

        PaymentRequest request = PaymentRequest.builder()
                .serviceRequestId(10L)
                .paymentMethod("credit-card")
                .amount(new BigDecimal("100.00"))
                .cardData(buildCardData(1))
                .build();

        PaymentResponse response = paymentService.createPayment("cliente@servija.com", request);

        assertThat(response.getId()).isEqualTo(78L);
        assertThat(response.getAmount()).isEqualByComparingTo("95.00");
        assertThat(response.getPlatformFee()).isEqualByComparingTo("5.00");
        assertThat(response.getInstallmentFee()).isEqualByComparingTo("0.00");
        assertThat(response.getGatewayFee()).isEqualByComparingTo("3.48");
        assertThat(response.getTotalAmount()).isEqualByComparingTo("100.00");
        assertThat(response.getInstallmentCount()).isEqualTo(1);
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        verify(walletService).addPendingBalance(serviceRequest.getFreelancer(), new BigDecimal("95.00"));
    }

    @Test
    @DisplayName("Deve criar pagamento parcelado em 2x com acréscimo de 1,2 por cento")
    void createPayment_createsTwoInstallmentCreditCardPaymentWithSurcharge() {
        User contractor = buildUser(1L, "cliente@servija.com", User.UserType.CONTRACTOR);
        ServiceRequest serviceRequest = buildAcceptedRequest(contractor, new BigDecimal("100.00"), false);

        mockBaseLookups(contractor, serviceRequest);
        when(asaasService.getOrCreateCustomer(anyString(), anyString(), anyString())).thenReturn("cus_123");
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(79L);
            return payment;
        });

        PaymentRequest request = PaymentRequest.builder()
                .serviceRequestId(10L)
                .paymentMethod("credit-card")
                .amount(new BigDecimal("100.00"))
                .cardData(buildCardData(2))
                .build();

        PaymentResponse response = paymentService.createPayment("cliente@servija.com", request);

        assertThat(response.getAmount()).isEqualByComparingTo("95.00");
        assertThat(response.getPlatformFee()).isEqualByComparingTo("5.00");
        assertThat(response.getInstallmentFee()).isEqualByComparingTo("1.20");
        assertThat(response.getGatewayFee()).isEqualByComparingTo("4.02");
        assertThat(response.getTotalAmount()).isEqualByComparingTo("101.20");
        assertThat(response.getInstallmentCount()).isEqualTo(2);
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        verify(walletService).addPendingBalance(serviceRequest.getFreelancer(), new BigDecimal("95.00"));
    }

    @Test
    @DisplayName("Deve criar pagamento parcelado no cartão com taxa de 3,49 por cento")
    void createPayment_createsInstallmentCreditCardPayment() {
        User contractor = buildUser(1L, "cliente@servija.com", User.UserType.CONTRACTOR);
        ServiceRequest serviceRequest = buildAcceptedRequest(contractor, new BigDecimal("100.00"), false);

        mockBaseLookups(contractor, serviceRequest);
        when(asaasService.getOrCreateCustomer(anyString(), anyString(), anyString())).thenReturn("cus_123");
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(80L);
            return payment;
        });

        PaymentRequest request = PaymentRequest.builder()
                .serviceRequestId(10L)
                .paymentMethod("credit-card")
                .amount(new BigDecimal("100.00"))
                .cardData(buildCardData(6))
                .build();

        PaymentResponse response = paymentService.createPayment("cliente@servija.com", request);

        assertThat(response.getAmount()).isEqualByComparingTo("95.00");
        assertThat(response.getPlatformFee()).isEqualByComparingTo("5.00");
        assertThat(response.getInstallmentFee()).isEqualByComparingTo("2.00");
        assertThat(response.getGatewayFee()).isEqualByComparingTo("4.05");
        assertThat(response.getTotalAmount()).isEqualByComparingTo("102.00");
        assertThat(response.getInstallmentCount()).isEqualTo(6);
        assertThat(response.getStatus()).isEqualTo("COMPLETED");
        verify(walletService).addPendingBalance(serviceRequest.getFreelancer(), new BigDecimal("95.00"));
    }

    @Test
    @DisplayName("Deve rejeitar cartão sem quantidade de parcelas")
    void createPayment_rejectsCreditCardWithoutInstallmentCount() {
        User contractor = buildUser(1L, "cliente@servija.com", User.UserType.CONTRACTOR);
        ServiceRequest serviceRequest = buildAcceptedRequest(contractor, new BigDecimal("100.00"), false);

        mockBaseLookups(contractor, serviceRequest);

        PaymentRequest request = PaymentRequest.builder()
                .serviceRequestId(10L)
                .paymentMethod("credit-card")
                .amount(new BigDecimal("100.00"))
                .cardData(PaymentRequest.CardData.builder()
                        .number("4111111111111111")
                        .name("Cliente Teste")
                        .expiry("12/30")
                        .cvv("123")
                        .build())
                .build();

        assertThatThrownBy(() -> paymentService.createPayment("cliente@servija.com", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("parcelas");
    }

    @Test
    @DisplayName("Deve rejeitar cartão com parcelamento acima de 6x")
    void createPayment_rejectsCreditCardWithUnsupportedInstallments() {
        User contractor = buildUser(1L, "cliente@servija.com", User.UserType.CONTRACTOR);
        ServiceRequest serviceRequest = buildAcceptedRequest(contractor, new BigDecimal("100.00"), false);

        mockBaseLookups(contractor, serviceRequest);

        PaymentRequest request = PaymentRequest.builder()
                .serviceRequestId(10L)
                .paymentMethod("credit-card")
                .amount(new BigDecimal("100.00"))
                .cardData(buildCardData(7))
                .build();

        assertThatThrownBy(() -> paymentService.createPayment("cliente@servija.com", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("1x e 6x");
    }

    private void mockBaseLookups(User contractor, ServiceRequest serviceRequest) {
        when(userRepository.findByEmail("cliente@servija.com")).thenReturn(Optional.of(contractor));
        when(serviceRequestRepository.findById(10L)).thenReturn(Optional.of(serviceRequest));
        when(paymentRepository.existsByServiceRequest(serviceRequest)).thenReturn(false);
    }

    private PaymentRequest.CardData buildCardData(int installmentCount) {
        return PaymentRequest.CardData.builder()
                .number("4111 1111 1111 1111")
                .name("Cliente Teste")
                .expiry("12/30")
                .cvv("123")
                .installmentCount(installmentCount)
                .build();
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
