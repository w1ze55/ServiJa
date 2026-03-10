package com.servija.servija.service;

import com.servija.servija.dto.PaymentRequest;
import com.servija.servija.dto.PaymentResponse;
import com.servija.servija.exception.ResourceNotFoundException;
import com.servija.servija.model.Payment;
import com.servija.servija.model.PaymentStatusHistory;
import com.servija.servija.model.ServiceRequest;
import com.servija.servija.model.User;
import com.servija.servija.repository.PaymentRepository;
import com.servija.servija.repository.PaymentStatusHistoryRepository;
import com.servija.servija.repository.ServiceRequestRepository;
import com.servija.servija.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentStatusHistoryRepository paymentStatusHistoryRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final UserRepository userRepository;
    private final AsaasService asaasService;
    private final FreelancerWalletService walletService;
    private final ServiceRequestHistoryService serviceRequestHistoryService;
    private final WebSocketService webSocketService;

    private static final BigDecimal PLATFORM_FEE_PERCENTAGE = new BigDecimal("0.10"); // 10%
    private static final BigDecimal ASAAS_PIX_FEE = new BigDecimal("0.99"); // Taxa Asaas para PIX (promocional)
    private static final BigDecimal ASAAS_BOLETO_FEE = new BigDecimal("0.99"); // Taxa Asaas para Boleto (promocional)
    private static final BigDecimal ASAAS_CREDIT_CARD_FIXED_FEE = new BigDecimal("0.49"); // Taxa fixa cartão
    private static final BigDecimal ASAAS_CREDIT_CARD_PERCENTAGE = new BigDecimal("0.0199"); // 1,99% cartão
                                                                                             // (promocional)
    private static final int RELEASE_DAYS = 7; // Dias para liberar saldo ao freelancer

    @Transactional
    public PaymentResponse createPayment(String email, PaymentRequest request) {
        User contractor = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        ServiceRequest serviceRequest = serviceRequestRepository.findById(request.getServiceRequestId())
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação de serviço não encontrada"));

        if (!serviceRequest.getContractor().getId().equals(contractor.getId())) {
            throw new IllegalStateException("Você não tem permissão para realizar pagamento desta solicitação");
        }

        if (!serviceRequest.getStatus().equals(ServiceRequest.Status.ACCEPTED)) {
            throw new IllegalStateException("A solicitação deve estar aceita para realizar o pagamento");
        }

        // Verificar se já existe um pagamento para esta solicitação
        boolean paymentExists = paymentRepository.existsByServiceRequest(serviceRequest);
        if (paymentExists) {
            throw new IllegalStateException("Já existe um pagamento para esta solicitação");
        }

        BigDecimal amount = request.getAmount(); // Valor que o CLIENTE paga

        // Calcular taxa do gateway baseado no método de pagamento
        BigDecimal gatewayFee = BigDecimal.ZERO;
        switch (request.getPaymentMethod().toLowerCase()) {
            case "pix":
                gatewayFee = ASAAS_PIX_FEE;
                break;
            case "boleto":
                gatewayFee = ASAAS_BOLETO_FEE;
                break;
            case "credit-card":
                gatewayFee = ASAAS_CREDIT_CARD_FIXED_FEE
                        .add(amount.multiply(ASAAS_CREDIT_CARD_PERCENTAGE));
                break;
        }

        // Valor líquido após deduzir taxa do gateway
        BigDecimal netAmount = amount.subtract(gatewayFee);

        // Taxa da plataforma (10% do valor líquido)
        BigDecimal platformFee = netAmount.multiply(PLATFORM_FEE_PERCENTAGE);

        // Valor que o freelancer receberá (após todas as deduções)
        BigDecimal freelancerAmount = netAmount.subtract(platformFee);

        // totalAmount é o que o cliente paga (não muda)
        BigDecimal totalAmount = amount;

        // Obter CPF do contratante (usar CPF do request se fornecido, senão usar do
        // perfil)
        String cpfCnpj = request.getCpf() != null && !request.getCpf().isEmpty()
                ? request.getCpf()
                : contractor.getCpf();

        if (cpfCnpj == null || cpfCnpj.isEmpty()) {
            throw new IllegalStateException(
                    "CPF/CNPJ do contratante não cadastrado. Por favor, informe o CPF para realizar o pagamento.");
        }

        // Criar ou recuperar cliente no Asaas
        String asaasCustomerId = asaasService.getOrCreateCustomer(
                contractor.getEmail(),
                contractor.getName(),
                cpfCnpj);

        Payment payment = Payment.builder()
                .serviceRequest(serviceRequest)
                .contractor(contractor)
                .freelancer(serviceRequest.getFreelancer())
                .amount(freelancerAmount) // Valor que o freelancer recebe
                .platformFee(platformFee)
                .gatewayFee(gatewayFee)
                .totalAmount(totalAmount) // Valor que o cliente paga
                .paymentMethod(request.getPaymentMethod())
                .status(Payment.PaymentStatus.PENDING)
                .transactionId(UUID.randomUUID().toString())
                .asaasCustomerId(asaasCustomerId)
                .build();

        // Processar pagamento baseado no método
        switch (request.getPaymentMethod().toLowerCase()) {
            case "pix":
                processPixPayment(payment, asaasCustomerId, serviceRequest);
                break;
            case "credit-card":
                processCreditCardPayment(payment, request.getCardData());
                break;
            case "wallet":
                processWalletPayment(payment);
                break;
            default:
                throw new IllegalArgumentException("Método de pagamento inválido");
        }

        payment = paymentRepository.save(payment);

        // Registrar no histórico de status do pagamento
        PaymentStatusHistory initialHistory = PaymentStatusHistory.builder()
                .payment(payment)
                .status(payment.getStatus())
                .asaasEvent("PAYMENT_CREATED")
                .details("Pagamento criado via " + request.getPaymentMethod())
                .build();
        paymentStatusHistoryRepository.save(initialHistory);

        // Registrar no histórico da solicitação de serviço
        serviceRequestHistoryService.record(
                serviceRequest,
                contractor,
                com.servija.servija.model.ServiceRequestHistory.OperationType.PAYMENT_CREATED,
                "Pagamento de R$ " + String.format("%.2f", totalAmount) + " criado via " + request.getPaymentMethod().toUpperCase());

        // NÃO atualizar status da solicitação aqui!
        // O status só deve mudar para COMPLETED quando o pagamento for confirmado (via
        // webhook)

        log.info("Pagamento criado com sucesso. ID: {}, Asaas ID: {}", payment.getId(), payment.getAsaasPaymentId());

        return mapToResponse(payment);
    }

    private void processPixPayment(Payment payment, String asaasCustomerId, ServiceRequest serviceRequest) {
        try {
            // Criar cobrança PIX no Asaas
            LocalDate dueDate = LocalDate.now().plusDays(1); // PIX expira em 1 dia
            String description = "ServiJá - " + serviceRequest.getServiceName();

            Map<String, Object> asaasPayment = asaasService.createPayment(
                    asaasCustomerId,
                    payment.getTotalAmount(),
                    dueDate,
                    "PIX",
                    description);

            payment.setAsaasPaymentId((String) asaasPayment.get("id"));

            // Obter QR Code PIX
            Map<String, String> pixData = asaasService.getPixQrCode(payment.getAsaasPaymentId());
            payment.setPixQrCode("data:image/png;base64," + pixData.get("encodedImage"));
            payment.setPixCopyPaste(pixData.get("payload"));
            payment.setStatus(Payment.PaymentStatus.PROCESSING);

            log.info("Pagamento PIX criado no Asaas: {}", payment.getAsaasPaymentId());
        } catch (Exception e) {
            log.error("Erro ao criar pagamento PIX no Asaas", e);
            throw new RuntimeException("Erro ao processar pagamento PIX: " + e.getMessage());
        }
    }

    private void processCreditCardPayment(Payment payment, PaymentRequest.CardData cardData) {
        if (cardData == null || cardData.getNumber() == null) {
            throw new IllegalArgumentException("Dados do cartão são obrigatórios");
        }

        // Extrair últimos 4 dígitos
        String cardNumber = cardData.getNumber().replaceAll("\\s", "");
        payment.setCardLastDigits(cardNumber.substring(cardNumber.length() - 4));

        // Por enquanto, simular aprovação instantânea
        // TODO: Integrar com gateway de cartão de crédito do Asaas
        payment.setStatus(Payment.PaymentStatus.COMPLETED);
        payment.setCompletedAt(LocalDateTime.now());

        // Adicionar saldo pendente na carteira do freelancer
        walletService.addPendingBalance(payment.getFreelancer(), payment.getAmount());

        log.info("Pagamento com cartão de crédito simulado (aprovação instantânea)");
    }

    private void processWalletPayment(Payment payment) {
        // Simular processamento de carteira digital
        payment.setStatus(Payment.PaymentStatus.PROCESSING);
        log.info("Pagamento com carteira digital em processamento");
    }

    public PaymentResponse getPaymentById(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pagamento não encontrado"));
        return mapToResponse(payment);
    }

    public PaymentResponse getPaymentByServiceRequestId(Long serviceRequestId) {
        List<Payment> payments = paymentRepository.findByServiceRequestId(serviceRequestId);
        if (payments.isEmpty()) {
            throw new ResourceNotFoundException("Nenhum pagamento encontrado para esta solicitação");
        }
        // Retornar o pagamento mais recente
        return mapToResponse(payments.get(0));
    }

    public List<PaymentResponse> getMyPayments(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        List<Payment> payments;
        if (user.getUserType() == User.UserType.CONTRACTOR) {
            payments = paymentRepository.findByContractor(user);
        } else {
            payments = paymentRepository.findByFreelancer(user);
        }

        return payments.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public PaymentResponse updatePaymentStatus(Long id, String status) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pagamento não encontrado"));

        payment.setStatus(Payment.PaymentStatus.valueOf(status.toUpperCase()));

        if (status.equalsIgnoreCase("COMPLETED")) {
            payment.setCompletedAt(LocalDateTime.now());
        }

        payment = paymentRepository.save(payment);
        return mapToResponse(payment);
    }

    /**
     * Verifica status do pagamento no Asaas e atualiza localmente
     */
    @Transactional
    public PaymentResponse checkAsaasPaymentStatus(Long id) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pagamento não encontrado"));

        if (payment.getAsaasPaymentId() != null) {
            try {
                String asaasStatus = asaasService.getPaymentStatus(payment.getAsaasPaymentId());

                // Mapear status do Asaas para nosso sistema
                Payment.PaymentStatus newStatus = mapAsaasStatus(asaasStatus);

                if (newStatus != payment.getStatus()) {
                    payment.setStatus(newStatus);

                    if (newStatus == Payment.PaymentStatus.COMPLETED) {
                        payment.setCompletedAt(LocalDateTime.now());

                        // Adicionar saldo pendente na carteira do freelancer
                        walletService.addPendingBalance(
                                payment.getFreelancer(),
                                payment.getAmount());

                        log.info("Saldo adicionado à carteira do freelancer: R$ {}", payment.getAmount());
                    }

                    payment = paymentRepository.save(payment);
                    log.info("Status do pagamento {} atualizado para {}", id, newStatus);
                }
            } catch (Exception e) {
                log.error("Erro ao verificar status no Asaas", e);
            }
        }

        return mapToResponse(payment);
    }

    private Payment.PaymentStatus mapAsaasStatus(String asaasStatus) {
        switch (asaasStatus) {
            case "PENDING":
            case "AWAITING_RISK_ANALYSIS":
                return Payment.PaymentStatus.PROCESSING;
            case "RECEIVED":
            case "CONFIRMED":
                return Payment.PaymentStatus.COMPLETED;
            case "REFUNDED":
                return Payment.PaymentStatus.REFUNDED;
            case "OVERDUE":
            case "RECEIVED_IN_CASH":
            default:
                return Payment.PaymentStatus.PENDING;
        }
    }

    public Map<String, Object> getPixQrCode(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Pagamento não encontrado"));

        // Se o pagamento já foi confirmado, retornar dados salvos ao invés de buscar no
        // Asaas
        if (payment.getStatus() == Payment.PaymentStatus.COMPLETED) {
            log.info("Pagamento {} já confirmado, retornando dados salvos", paymentId);
            return Map.of(
                    "encodedImage", payment.getPixQrCode() != null ? payment.getPixQrCode() : "",
                    "payload", payment.getPixCopyPaste() != null ? payment.getPixCopyPaste() : "",
                    "expirationDate", "",
                    "description", "Pagamento já confirmado",
                    "status", "COMPLETED",
                    "message", "Este pagamento já foi confirmado e processado!");
        }

        if (payment.getAsaasPaymentId() == null) {
            throw new IllegalStateException("Pagamento não possui ID do Asaas");
        }

        if (!"PIX".equalsIgnoreCase(payment.getPaymentMethod())) {
            throw new IllegalStateException("Este pagamento não é do tipo PIX");
        }

        try {
            Map<String, String> pixData = asaasService.getPixQrCode(payment.getAsaasPaymentId());

            return Map.of(
                    "encodedImage", pixData.get("encodedImage"),
                    "payload", pixData.get("payload"),
                    "expirationDate", pixData.getOrDefault("expirationDate", ""),
                    "description", pixData.getOrDefault("description", ""),
                    "status", payment.getStatus().name());
        } catch (Exception e) {
            log.error("Erro ao obter QR Code PIX", e);
            throw new RuntimeException("Erro ao obter QR Code PIX: " + e.getMessage());
        }
    }

    private PaymentResponse mapToResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .serviceRequestId(payment.getServiceRequest().getId())
                .amount(payment.getAmount())
                .platformFee(payment.getPlatformFee())
                .gatewayFee(payment.getGatewayFee())
                .totalAmount(payment.getTotalAmount())
                .paymentMethod(payment.getPaymentMethod())
                .status(payment.getStatus().name())
                .transactionId(payment.getTransactionId())
                .pixQrCode(payment.getPixQrCode())
                .pixCopyPaste(payment.getPixCopyPaste())
                .asaasPaymentId(payment.getAsaasPaymentId())
                .createdAt(payment.getCreatedAt())
                .completedAt(payment.getCompletedAt())
                .build();
    }

    /**
     * Buscar histórico de status de um pagamento
     */
    public List<Map<String, Object>> getPaymentHistory(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Pagamento não encontrado"));

        List<PaymentStatusHistory> history = paymentStatusHistoryRepository
                .findByPaymentOrderByCreatedAtAsc(payment);

        return history.stream()
                .map(h -> {
                    Map<String, Object> map = new java.util.HashMap<>();
                    map.put("id", h.getId());
                    map.put("status", h.getStatus().name());
                    map.put("asaasEvent", h.getAsaasEvent() != null ? h.getAsaasEvent() : "");
                    map.put("details", h.getDetails() != null ? h.getDetails() : "");
                    map.put("createdAt", h.getCreatedAt().toString());
                    return map;
                })
                .collect(Collectors.toList());
    }

    /**
     * Obtém URL do recibo/comprovante de pagamento via Asaas
     */
    public Map<String, String> getReceiptUrl(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Pagamento não encontrado"));

        if (payment.getAsaasPaymentId() == null) {
            throw new IllegalStateException("Pagamento não possui integração com Asaas");
        }

        return asaasService.getPaymentReceiptUrl(payment.getAsaasPaymentId());
    }

    /**
     * 🧪 MÉTODO DE TESTE - REMOVER EM PRODUÇÃO
     * Simula a confirmação de um pagamento PIX para testes locais
     */
    @Transactional
    public void simulatePaymentConfirmation(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Pagamento não encontrado"));

        if (payment.getStatus() == Payment.PaymentStatus.COMPLETED) {
            log.warn("🧪 Pagamento já está confirmado: {}", paymentId);
            return;
        }

        log.info("🧪 SIMULANDO confirmação de pagamento: {}", paymentId);

        // Atualizar status do pagamento
        payment.setStatus(Payment.PaymentStatus.COMPLETED);
        payment.setCompletedAt(LocalDateTime.now());
        paymentRepository.save(payment);

        // Registrar no histórico do pagamento
        PaymentStatusHistory history = PaymentStatusHistory.builder()
                .payment(payment)
                .status(Payment.PaymentStatus.COMPLETED)
                .asaasEvent("SIMULATED_CONFIRMATION")
                .details("🧪 Confirmação simulada para testes")
                .build();
        paymentStatusHistoryRepository.save(history);

        // Adicionar saldo pendente na carteira do freelancer
        walletService.addPendingBalance(
                payment.getFreelancer(),
                payment.getAmount());

        log.info("🧪 Saldo adicionado à carteira: R$ {}", payment.getAmount());

        // Atualizar status da solicitação para IN_PROGRESS e habilitar chat
        ServiceRequest serviceRequest = payment.getServiceRequest();
        if (serviceRequest.getStatus() != ServiceRequest.Status.IN_PROGRESS && 
            serviceRequest.getStatus() != ServiceRequest.Status.COMPLETED) {
            serviceRequest.setStatus(ServiceRequest.Status.IN_PROGRESS);
            serviceRequest.setChatEnabled(true);
            serviceRequestRepository.save(serviceRequest);

            // Registrar no histórico da solicitação
            serviceRequestHistoryService.record(
                    serviceRequest,
                    payment.getContractor(),
                    com.servija.servija.model.ServiceRequestHistory.OperationType.PAYMENT_CONFIRMED,
                    "🧪 Pagamento simulado de R$ " + String.format("%.2f", payment.getTotalAmount()) + " confirmado");

            log.info("🧪 Solicitação atualizada para IN_PROGRESS, chat habilitado");
        }

        // Notificar via WebSocket - CLIENTE E FREELANCER
        webSocketService.notifyPixPaymentConfirmed(
                payment.getId(),
                payment.getContractor().getEmail());
        
        webSocketService.notifyPixPaymentConfirmed(
                payment.getId(),
                payment.getFreelancer().getEmail());

        log.info("🧪 Simulação de confirmação concluída com sucesso!");
    }
}
