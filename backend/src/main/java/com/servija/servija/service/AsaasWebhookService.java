package com.servija.servija.service;

import com.servija.servija.model.Payment;
import com.servija.servija.model.ServiceRequest;
import com.servija.servija.model.WithdrawalRequest;
import com.servija.servija.repository.PaymentRepository;
import com.servija.servija.repository.ServiceRequestRepository;
import com.servija.servija.repository.WithdrawalRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsaasWebhookService {

    private final PaymentRepository paymentRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final WithdrawalRequestRepository withdrawalRepository;
    private final FreelancerWalletService walletService;
    private final WebSocketService webSocketService;

    /**
     * Processa webhook recebido do Asaas
     */
    @Transactional
    public void processWebhook(Map<String, Object> payload) {
        String event = (String) payload.get("event");
        
        if (event == null) {
            log.warn("Webhook sem evento especificado");
            return;
        }

        log.info("Processando evento do webhook: {}", event);

        @SuppressWarnings("unchecked")
        Map<String, Object> payment = (Map<String, Object>) payload.get("payment");
        
        @SuppressWarnings("unchecked")
        Map<String, Object> transfer = (Map<String, Object>) payload.get("transfer");

        switch (event) {
            // Eventos de pagamento confirmado/recebido
            case "PAYMENT_RECEIVED":
            case "PAYMENT_CONFIRMED":
                handlePaymentReceived(payment);
                break;

            // Eventos de autorização e análise de risco
            case "PAYMENT_AUTHORIZED":
                handlePaymentAuthorized(payment);
                break;

            case "PAYMENT_AWAITING_RISK_ANALYSIS":
                handlePaymentAwaitingRiskAnalysis(payment);
                break;

            case "PAYMENT_APPROVED_BY_RISK_ANALYSIS":
                handlePaymentApprovedByRiskAnalysis(payment);
                break;

            case "PAYMENT_REPROVED_BY_RISK_ANALYSIS":
                handlePaymentReprovedByRiskAnalysis(payment);
                break;

            // Eventos de criação e atualização
            case "PAYMENT_CREATED":
                handlePaymentCreated(payment);
                break;

            case "PAYMENT_UPDATED":
                handlePaymentUpdated(payment);
                break;

            // Eventos de problemas
            case "PAYMENT_OVERDUE":
                handlePaymentOverdue(payment);
                break;

            case "PAYMENT_CREDIT_CARD_CAPTURE_REFUSED":
                handlePaymentCaptureRefused(payment);
                break;

            // Eventos de estorno
            case "PAYMENT_REFUNDED":
            case "PAYMENT_PARTIALLY_REFUNDED":
                handlePaymentRefunded(payment);
                break;

            case "PAYMENT_REFUND_IN_PROGRESS":
                handlePaymentRefundInProgress(payment);
                break;

            case "PAYMENT_REFUND_DENIED":
                handlePaymentRefundDenied(payment);
                break;

            // Eventos de chargeback
            case "PAYMENT_CHARGEBACK_REQUESTED":
                handlePaymentChargebackRequested(payment);
                break;

            case "PAYMENT_CHARGEBACK_DISPUTE":
                handlePaymentChargebackDispute(payment);
                break;

            case "PAYMENT_AWAITING_CHARGEBACK_REVERSAL":
                handlePaymentAwaitingChargebackReversal(payment);
                break;

            // Eventos de remoção e restauração
            case "PAYMENT_DELETED":
                handlePaymentDeleted(payment);
                break;

            case "PAYMENT_RESTORED":
                handlePaymentRestored(payment);
                break;

            // Eventos de split
            case "PAYMENT_SPLIT_CANCELLED":
                handlePaymentSplitCancelled(payment);
                break;

            case "PAYMENT_SPLIT_DIVERGENCE_BLOCK":
                handlePaymentSplitDivergenceBlock(payment);
                break;

            case "PAYMENT_SPLIT_DIVERGENCE_BLOCK_FINISHED":
                handlePaymentSplitDivergenceBlockFinished(payment);
                break;

            // Eventos de visualização
            case "PAYMENT_BANK_SLIP_VIEWED":
                handlePaymentBankSlipViewed(payment);
                break;

            case "PAYMENT_CHECKOUT_VIEWED":
                handlePaymentCheckoutViewed(payment);
                break;

            case "PAYMENT_BANK_SLIP_CANCELLED":
                handlePaymentBankSlipCancelled(payment);
                break;

            // Eventos de antecipação e negativação
            case "PAYMENT_ANTICIPATED":
                handlePaymentAnticipated(payment);
                break;

            case "PAYMENT_DUNNING_REQUESTED":
                handlePaymentDunningRequested(payment);
                break;

            case "PAYMENT_DUNNING_RECEIVED":
                handlePaymentDunningReceived(payment);
                break;

            case "PAYMENT_RECEIVED_IN_CASH_UNDONE":
                handlePaymentReceivedInCashUndone(payment);
                break;

            // Eventos de transferência (saques)
            case "TRANSFER_CREATED":
            case "TRANSFER_DONE":
                handleTransferCompleted(transfer);
                break;

            case "TRANSFER_FAILED":
                handleTransferFailed(transfer);
                break;

            default:
                log.info("Evento não tratado: {}", event);
        }
    }

    /**
     * Pagamento recebido/confirmado
     */
    private void handlePaymentReceived(Map<String, Object> paymentData) {
        if (paymentData == null) {
            log.warn("Dados do pagamento não encontrados no webhook");
            return;
        }

        String asaasPaymentId = (String) paymentData.get("id");
        
        List<Payment> payments = paymentRepository.findAll().stream()
                .filter(p -> asaasPaymentId.equals(p.getAsaasPaymentId()))
                .toList();

        if (payments.isEmpty()) {
            log.warn("Pagamento não encontrado no sistema: {}", asaasPaymentId);
            return;
        }

        Payment payment = payments.get(0);

        if (payment.getStatus() != Payment.PaymentStatus.COMPLETED) {
            payment.setStatus(Payment.PaymentStatus.COMPLETED);
            payment.setCompletedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            // Adicionar saldo pendente na carteira do freelancer
            walletService.addPendingBalance(
                payment.getFreelancer(),
                payment.getAmount()
            );

            // Atualizar status da solicitação para COMPLETED
            ServiceRequest serviceRequest = payment.getServiceRequest();
            if (serviceRequest.getStatus() != ServiceRequest.Status.COMPLETED) {
                serviceRequest.setStatus(ServiceRequest.Status.COMPLETED);
                serviceRequestRepository.save(serviceRequest);
                log.info("Status da solicitação atualizado para COMPLETED: {}", serviceRequest.getId());
            }

            // Enviar notificação WebSocket para o cliente
            webSocketService.notifyPixPaymentConfirmed(
                payment.getId(),
                payment.getContractor().getEmail()
            );

            log.info("Pagamento confirmado via webhook: {} - R$ {}", 
                    asaasPaymentId, payment.getAmount());
        }
    }

    /**
     * Pagamento vencido
     */
    private void handlePaymentOverdue(Map<String, Object> paymentData) {
        if (paymentData == null) return;

        String asaasPaymentId = (String) paymentData.get("id");
        log.info("Pagamento vencido: {}", asaasPaymentId);
        
        List<Payment> payments = paymentRepository.findAll().stream()
                .filter(p -> asaasPaymentId.equals(p.getAsaasPaymentId()))
                .toList();

        if (!payments.isEmpty()) {
            Payment payment = payments.get(0);
            payment.setStatus(Payment.PaymentStatus.OVERDUE);
            paymentRepository.save(payment);
            log.info("Status do pagamento atualizado para OVERDUE: {}", asaasPaymentId);
        }
    }

    // ==================== EVENTOS DE AUTORIZAÇÃO E ANÁLISE DE RISCO ====================

    /**
     * Pagamento autorizado (aguardando captura)
     */
    private void handlePaymentAuthorized(Map<String, Object> paymentData) {
        if (paymentData == null) return;
        String asaasPaymentId = (String) paymentData.get("id");
        log.info("Pagamento autorizado (aguardando captura): {}", asaasPaymentId);
    }

    /**
     * Pagamento aguardando análise de risco
     */
    private void handlePaymentAwaitingRiskAnalysis(Map<String, Object> paymentData) {
        if (paymentData == null) return;
        String asaasPaymentId = (String) paymentData.get("id");
        log.info("Pagamento aguardando análise de risco: {}", asaasPaymentId);
    }

    /**
     * Pagamento aprovado pela análise de risco
     */
    private void handlePaymentApprovedByRiskAnalysis(Map<String, Object> paymentData) {
        if (paymentData == null) return;
        String asaasPaymentId = (String) paymentData.get("id");
        log.info("Pagamento aprovado pela análise de risco: {}", asaasPaymentId);
    }

    /**
     * Pagamento reprovado pela análise de risco
     */
    private void handlePaymentReprovedByRiskAnalysis(Map<String, Object> paymentData) {
        if (paymentData == null) return;

        String asaasPaymentId = (String) paymentData.get("id");
        
        List<Payment> payments = paymentRepository.findAll().stream()
                .filter(p -> asaasPaymentId.equals(p.getAsaasPaymentId()))
                .toList();

        if (!payments.isEmpty()) {
            Payment payment = payments.get(0);
            payment.setStatus(Payment.PaymentStatus.FAILED);
            paymentRepository.save(payment);
            
            webSocketService.notifyPaymentFailed(
                payment.getId(),
                payment.getContractor().getEmail(),
                "Pagamento reprovado pela análise de risco"
            );
            
            log.warn("Pagamento reprovado pela análise de risco: {}", asaasPaymentId);
        }
    }

    // ==================== EVENTOS DE CRIAÇÃO E ATUALIZAÇÃO ====================

    /**
     * Pagamento criado
     */
    private void handlePaymentCreated(Map<String, Object> paymentData) {
        if (paymentData == null) return;
        String asaasPaymentId = (String) paymentData.get("id");
        log.info("Pagamento criado no Asaas: {}", asaasPaymentId);
    }

    /**
     * Pagamento atualizado
     */
    private void handlePaymentUpdated(Map<String, Object> paymentData) {
        if (paymentData == null) return;
        String asaasPaymentId = (String) paymentData.get("id");
        log.info("Pagamento atualizado: {}", asaasPaymentId);
    }

    /**
     * Captura de cartão recusada
     */
    private void handlePaymentCaptureRefused(Map<String, Object> paymentData) {
        if (paymentData == null) return;

        String asaasPaymentId = (String) paymentData.get("id");
        
        List<Payment> payments = paymentRepository.findAll().stream()
                .filter(p -> asaasPaymentId.equals(p.getAsaasPaymentId()))
                .toList();

        if (!payments.isEmpty()) {
            Payment payment = payments.get(0);
            payment.setStatus(Payment.PaymentStatus.FAILED);
            paymentRepository.save(payment);
            
            webSocketService.notifyPaymentFailed(
                payment.getId(),
                payment.getContractor().getEmail(),
                "Captura de cartão recusada"
            );
            
            log.error("Captura de cartão recusada: {}", asaasPaymentId);
        }
    }

    // ==================== EVENTOS DE ESTORNO ====================

    /**
     * Estorno em progresso
     */
    private void handlePaymentRefundInProgress(Map<String, Object> paymentData) {
        if (paymentData == null) return;
        String asaasPaymentId = (String) paymentData.get("id");
        log.info("Estorno em progresso: {}", asaasPaymentId);
    }

    /**
     * Estorno negado
     */
    private void handlePaymentRefundDenied(Map<String, Object> paymentData) {
        if (paymentData == null) return;
        String asaasPaymentId = (String) paymentData.get("id");
        log.warn("Estorno negado: {}", asaasPaymentId);
    }

    // ==================== EVENTOS DE CHARGEBACK ====================

    /**
     * Chargeback solicitado
     */
    private void handlePaymentChargebackRequested(Map<String, Object> paymentData) {
        if (paymentData == null) return;

        String asaasPaymentId = (String) paymentData.get("id");
        
        List<Payment> payments = paymentRepository.findAll().stream()
                .filter(p -> asaasPaymentId.equals(p.getAsaasPaymentId()))
                .toList();

        if (!payments.isEmpty()) {
            Payment payment = payments.get(0);
            payment.setStatus(Payment.PaymentStatus.CHARGEBACK);
            paymentRepository.save(payment);
            
            log.error("⚠️ CHARGEBACK solicitado: {} - R$ {}", 
                    asaasPaymentId, payment.getAmount());
        }
    }

    /**
     * Chargeback em disputa
     */
    private void handlePaymentChargebackDispute(Map<String, Object> paymentData) {
        if (paymentData == null) return;
        String asaasPaymentId = (String) paymentData.get("id");
        log.info("Chargeback em disputa: {}", asaasPaymentId);
    }

    /**
     * Aguardando reversão de chargeback
     */
    private void handlePaymentAwaitingChargebackReversal(Map<String, Object> paymentData) {
        if (paymentData == null) return;
        String asaasPaymentId = (String) paymentData.get("id");
        log.info("Aguardando reversão de chargeback (disputa vencida): {}", asaasPaymentId);
    }

    // ==================== EVENTOS DE REMOÇÃO E RESTAURAÇÃO ====================

    /**
     * Pagamento deletado
     */
    private void handlePaymentDeleted(Map<String, Object> paymentData) {
        if (paymentData == null) return;

        String asaasPaymentId = (String) paymentData.get("id");
        
        List<Payment> payments = paymentRepository.findAll().stream()
                .filter(p -> asaasPaymentId.equals(p.getAsaasPaymentId()))
                .toList();

        if (!payments.isEmpty()) {
            Payment payment = payments.get(0);
            payment.setStatus(Payment.PaymentStatus.CANCELLED);
            paymentRepository.save(payment);
            log.info("Pagamento deletado: {}", asaasPaymentId);
        }
    }

    /**
     * Pagamento restaurado
     */
    private void handlePaymentRestored(Map<String, Object> paymentData) {
        if (paymentData == null) return;

        String asaasPaymentId = (String) paymentData.get("id");
        
        List<Payment> payments = paymentRepository.findAll().stream()
                .filter(p -> asaasPaymentId.equals(p.getAsaasPaymentId()))
                .toList();

        if (!payments.isEmpty()) {
            Payment payment = payments.get(0);
            payment.setStatus(Payment.PaymentStatus.PENDING);
            paymentRepository.save(payment);
            log.info("Pagamento restaurado: {}", asaasPaymentId);
        }
    }

    // ==================== EVENTOS DE SPLIT ====================

    /**
     * Split cancelado
     */
    private void handlePaymentSplitCancelled(Map<String, Object> paymentData) {
        if (paymentData == null) return;
        String asaasPaymentId = (String) paymentData.get("id");
        log.warn("Split cancelado: {}", asaasPaymentId);
    }

    /**
     * Bloqueio por divergência de split
     */
    private void handlePaymentSplitDivergenceBlock(Map<String, Object> paymentData) {
        if (paymentData == null) return;
        String asaasPaymentId = (String) paymentData.get("id");
        log.warn("Valor bloqueado por divergência de split: {}", asaasPaymentId);
    }

    /**
     * Bloqueio por divergência de split finalizado
     */
    private void handlePaymentSplitDivergenceBlockFinished(Map<String, Object> paymentData) {
        if (paymentData == null) return;
        String asaasPaymentId = (String) paymentData.get("id");
        log.info("Bloqueio por divergência de split finalizado: {}", asaasPaymentId);
    }

    // ==================== EVENTOS DE VISUALIZAÇÃO ====================

    /**
     * Boleto visualizado
     */
    private void handlePaymentBankSlipViewed(Map<String, Object> paymentData) {
        if (paymentData == null) return;
        String asaasPaymentId = (String) paymentData.get("id");
        log.info("Boleto visualizado pelo cliente: {}", asaasPaymentId);
    }

    /**
     * Checkout visualizado
     */
    private void handlePaymentCheckoutViewed(Map<String, Object> paymentData) {
        if (paymentData == null) return;
        String asaasPaymentId = (String) paymentData.get("id");
        log.info("Checkout visualizado pelo cliente: {}", asaasPaymentId);
    }

    /**
     * Boleto cancelado por expiração
     */
    private void handlePaymentBankSlipCancelled(Map<String, Object> paymentData) {
        if (paymentData == null) return;

        String asaasPaymentId = (String) paymentData.get("id");
        
        List<Payment> payments = paymentRepository.findAll().stream()
                .filter(p -> asaasPaymentId.equals(p.getAsaasPaymentId()))
                .toList();

        if (!payments.isEmpty()) {
            Payment payment = payments.get(0);
            payment.setStatus(Payment.PaymentStatus.CANCELLED);
            paymentRepository.save(payment);
            log.info("Boleto cancelado por expiração: {}", asaasPaymentId);
        }
    }

    // ==================== EVENTOS DIVERSOS ====================

    /**
     * Pagamento antecipado
     */
    private void handlePaymentAnticipated(Map<String, Object> paymentData) {
        if (paymentData == null) return;
        String asaasPaymentId = (String) paymentData.get("id");
        log.info("Pagamento antecipado: {}", asaasPaymentId);
    }

    /**
     * Negativação solicitada
     */
    private void handlePaymentDunningRequested(Map<String, Object> paymentData) {
        if (paymentData == null) return;
        String asaasPaymentId = (String) paymentData.get("id");
        log.warn("Negativação solicitada: {}", asaasPaymentId);
    }

    /**
     * Negativação recebida
     */
    private void handlePaymentDunningReceived(Map<String, Object> paymentData) {
        if (paymentData == null) return;
        String asaasPaymentId = (String) paymentData.get("id");
        log.info("Recebimento de negativação: {}", asaasPaymentId);
    }

    /**
     * Recebimento em dinheiro desfeito
     */
    private void handlePaymentReceivedInCashUndone(Map<String, Object> paymentData) {
        if (paymentData == null) return;

        String asaasPaymentId = (String) paymentData.get("id");
        
        List<Payment> payments = paymentRepository.findAll().stream()
                .filter(p -> asaasPaymentId.equals(p.getAsaasPaymentId()))
                .toList();

        if (!payments.isEmpty()) {
            Payment payment = payments.get(0);
            payment.setStatus(Payment.PaymentStatus.PENDING);
            paymentRepository.save(payment);
            log.warn("Recebimento em dinheiro desfeito: {}", asaasPaymentId);
        }
    }

    /**
     * Pagamento reembolsado
     */
    private void handlePaymentRefunded(Map<String, Object> paymentData) {
        if (paymentData == null) return;

        String asaasPaymentId = (String) paymentData.get("id");
        
        List<Payment> payments = paymentRepository.findAll().stream()
                .filter(p -> asaasPaymentId.equals(p.getAsaasPaymentId()))
                .toList();

        if (payments.isEmpty()) {
            log.warn("Pagamento não encontrado no sistema: {}", asaasPaymentId);
            return;
        }

        Payment payment = payments.get(0);
        payment.setStatus(Payment.PaymentStatus.REFUNDED);
        paymentRepository.save(payment);

        log.info("Pagamento reembolsado via webhook: {}", asaasPaymentId);
    }

    /**
     * Transferência concluída (saque do freelancer)
     */
    private void handleTransferCompleted(Map<String, Object> transferData) {
        if (transferData == null) {
            log.warn("Dados da transferência não encontrados no webhook");
            return;
        }

        String asaasTransferId = (String) transferData.get("id");
        
        List<WithdrawalRequest> withdrawals = withdrawalRepository.findAll().stream()
                .filter(w -> asaasTransferId.equals(w.getAsaasTransferId()))
                .toList();

        if (withdrawals.isEmpty()) {
            log.warn("Solicitação de saque não encontrada no sistema: {}", asaasTransferId);
            return;
        }

        WithdrawalRequest withdrawal = withdrawals.get(0);

        if (withdrawal.getStatus() != WithdrawalRequest.WithdrawalStatus.COMPLETED) {
            withdrawal.setStatus(WithdrawalRequest.WithdrawalStatus.COMPLETED);
            withdrawal.setProcessedAt(LocalDateTime.now());
            withdrawalRepository.save(withdrawal);

            log.info("Saque confirmado via webhook: {} - R$ {}", 
                    asaasTransferId, withdrawal.getAmount());
        }
    }

    /**
     * Transferência falhou
     */
    private void handleTransferFailed(Map<String, Object> transferData) {
        if (transferData == null) return;

        String asaasTransferId = (String) transferData.get("id");
        String failReason = (String) transferData.get("failReason");
        
        List<WithdrawalRequest> withdrawals = withdrawalRepository.findAll().stream()
                .filter(w -> asaasTransferId.equals(w.getAsaasTransferId()))
                .toList();

        if (withdrawals.isEmpty()) {
            log.warn("Solicitação de saque não encontrada no sistema: {}", asaasTransferId);
            return;
        }

        WithdrawalRequest withdrawal = withdrawals.get(0);
        withdrawal.setStatus(WithdrawalRequest.WithdrawalStatus.FAILED);
        withdrawal.setRejectionReason(failReason);
        withdrawalRepository.save(withdrawal);

        // Reverter saldo do freelancer
        walletService.addPendingBalance(
            withdrawal.getFreelancer(),
            withdrawal.getAmount()
        );

        log.error("Saque falhou via webhook: {} - Motivo: {}", 
                asaasTransferId, failReason);
    }
}
