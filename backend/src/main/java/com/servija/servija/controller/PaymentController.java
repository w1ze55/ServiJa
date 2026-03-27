package com.servija.servija.controller;

import com.servija.servija.dto.PaymentRequest;
import com.servija.servija.dto.PaymentResponse;
import com.servija.servija.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Pagamentos", description = "Endpoints para gerenciamento de pagamentos")
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping
    @Operation(summary = "Criar pagamento", description = "Cria um novo pagamento para uma solicitação aceita")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<PaymentResponse> createPayment(
            @Valid @RequestBody PaymentRequest request,
            Authentication authentication) {
        String email = authentication.getName();
        PaymentResponse response = paymentService.createPayment(email, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obter detalhes do pagamento", description = "Retorna os detalhes de um pagamento específico")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<PaymentResponse> getPayment(@PathVariable Long id) {
        PaymentResponse response = paymentService.getPaymentById(id);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    @Operation(summary = "Listar meus pagamentos", description = "Lista todos os pagamentos do usuário autenticado")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<PaymentResponse>> getMyPayments(Authentication authentication) {
        String email = authentication.getName();
        List<PaymentResponse> payments = paymentService.getMyPayments(email);
        return ResponseEntity.ok(payments);
    }

    @GetMapping("/{id}/status")
    @Operation(summary = "Verificar status do pagamento", description = "Verifica o status atual de um pagamento e atualiza com dados do Asaas")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<PaymentResponse> checkPaymentStatus(@PathVariable Long id) {
        PaymentResponse response = paymentService.checkAsaasPaymentStatus(id);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Atualizar status do pagamento", description = "Atualiza o status de um pagamento")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<PaymentResponse> updatePaymentStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String status = body.get("status");
        PaymentResponse response = paymentService.updatePaymentStatus(id, status);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/pix-qrcode")
    @Operation(summary = "Obter QR Code PIX", description = "Retorna o QR Code PIX para um pagamento")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Map<String, Object>> getPixQrCode(@PathVariable Long id) {
        Map<String, Object> qrCodeData = paymentService.getPixQrCode(id);
        return ResponseEntity.ok(qrCodeData);
    }

    @GetMapping("/service-request/{serviceRequestId}")
    @Operation(summary = "Obter pagamento por solicitação", description = "Retorna o pagamento de uma solicitação específica")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<PaymentResponse> getPaymentByServiceRequest(@PathVariable Long serviceRequestId) {
        PaymentResponse response = paymentService.getPaymentByServiceRequestId(serviceRequestId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/history")
    @Operation(summary = "Obter histórico de status", description = "Retorna o histórico de mudanças de status de um pagamento")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<Map<String, Object>>> getPaymentHistory(@PathVariable Long id) {
        List<Map<String, Object>> history = paymentService.getPaymentHistory(id);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/{id}/receipt-url")
    @Operation(summary = "Obter URL do recibo", description = "Retorna a URL do comprovante/recibo de um pagamento via Asaas")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Map<String, String>> getReceiptUrl(@PathVariable Long id) {
        Map<String, String> receiptData = paymentService.getReceiptUrl(id);
        return ResponseEntity.ok(receiptData);
    }

    /**
     * 🧪 ENDPOINT DE TESTE - REMOVER EM PRODUÇÃO
     * Simula a confirmação de um pagamento PIX para testes locais
     */
    @PostMapping("/{id}/simulate-confirmation")
    @Operation(summary = "[TESTE] Simular confirmação de pagamento", description = "Simula a confirmação de um pagamento PIX para testes. REMOVER EM PRODUÇÃO!")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Map<String, Object>> simulatePaymentConfirmation(@PathVariable Long id) {
        paymentService.simulatePaymentConfirmation(id);
        return ResponseEntity.ok(Map.of(
            "success", true,
            "message", "Simulação de confirmação enviada com sucesso"
        ));
    }
}
