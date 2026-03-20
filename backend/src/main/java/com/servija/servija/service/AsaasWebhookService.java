package com.servija.servija.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.servija.servija.dto.PixTransactionWebhookDTO;
import com.servija.servija.model.Payment;
import com.servija.servija.model.PaymentStatusHistory;
import com.servija.servija.model.ServiceRequest;
import com.servija.servija.model.ServiceRequestHistory;
import com.servija.servija.model.WithdrawalRequest;
import com.servija.servija.repository.PaymentRepository;
import com.servija.servija.repository.PaymentStatusHistoryRepository;
import com.servija.servija.repository.ServiceRequestRepository;
import com.servija.servija.repository.WithdrawalRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsaasWebhookService {

    private final PaymentRepository paymentRepository;
    private final PaymentStatusHistoryRepository paymentStatusHistoryRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final WithdrawalRequestRepository withdrawalRepository;
    private final FreelancerWalletService walletService;
    private final WebSocketService webSocketService;
    private final ServiceRequestHistoryService serviceRequestHistoryService;
    private final ObjectMapper objectMapper;

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
        String asaasStatus = (String) paymentData.get("status");

        log.info("🔔 Webhook PAYMENT_RECEIVED/CONFIRMED recebido - Asaas Payment ID: {}, Status: {}",
                asaasPaymentId, asaasStatus);

        Payment payment = paymentRepository.findByAsaasPaymentId(asaasPaymentId);

        if (payment == null) {
            log.error("❌ Pagamento não encontrado no sistema: {}", asaasPaymentId);
            log.error("📋 Todos os IDs Asaas no banco: {}",
                    paymentRepository.findAll().stream()
                            .map(Payment::getAsaasPaymentId)
                            .toList());
            return;
        }

        log.info("✅ Pagamento encontrado no banco - ID: {}, Status Atual: {}",
                payment.getId(), payment.getStatus());

        if (payment.getStatus() != Payment.PaymentStatus.COMPLETED) {
            payment.setStatus(Payment.PaymentStatus.COMPLETED);
            payment.setCompletedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            log.info("💾 Status atualizado para COMPLETED");

            // Registrar no histórico
            saveStatusHistory(payment, Payment.PaymentStatus.COMPLETED, "PAYMENT_RECEIVED",
                    "Pagamento recebido/confirmado via webhook Asaas");

            // Adicionar saldo pendente na carteira do freelancer
            walletService.addPendingBalance(
                    payment.getFreelancer(),
                    payment.getAmount());

            log.info("💰 Saldo adicionado à carteira: R$ {}", payment.getAmount());

            // Atualizar status da solicitação para IN_PROGRESS E HABILITAR CHAT
            ServiceRequest serviceRequest = payment.getServiceRequest();
            if (serviceRequest.getStatus() != ServiceRequest.Status.IN_PROGRESS && 
                serviceRequest.getStatus() != ServiceRequest.Status.COMPLETED) {
                serviceRequest.setStatus(ServiceRequest.Status.IN_PROGRESS);
                serviceRequest.setChatEnabled(true); // 🔓 HABILITAR CHAT
                serviceRequestRepository.save(serviceRequest);
                log.info("🔓 Chat habilitado! Solicitação ID: {} - Status: IN_PROGRESS", serviceRequest.getId());
                
                // Registrar no histórico da solicitação
                serviceRequestHistoryService.record(
                        serviceRequest,
                        payment.getContractor(),
                        ServiceRequestHistory.OperationType.PAYMENT_CONFIRMED,
                        "Pagamento de R$ " + String.format("%.2f", payment.getTotalAmount()) + " confirmado");
                
                // Notificar mudança de status da solicitação para ambas as partes
                webSocketService.notifyServiceRequestStatusChange(
                        serviceRequest.getId(),
                        "IN_PROGRESS",
                        payment.getContractor().getEmail(),
                        payment.getFreelancer().getEmail());
            } else if (!Boolean.TRUE.equals(serviceRequest.getChatEnabled())) {
                // Se já estava em andamento mas chat não estava habilitado
                serviceRequest.setChatEnabled(true);
                serviceRequestRepository.save(serviceRequest);
                log.info("🔓 Chat habilitado (solicitação já estava {}))", serviceRequest.getStatus());
            }

            // Enviar notificação WebSocket de pagamento para o cliente E FREELANCER
            webSocketService.notifyPixPaymentConfirmed(
                    payment.getId(),
                    payment.getContractor().getEmail());
            
            webSocketService.notifyPixPaymentConfirmed(
                    payment.getId(),
                    payment.getFreelancer().getEmail());

            log.info("✅ Pagamento confirmado via webhook: {} - R$ {}",
                    asaasPaymentId, payment.getAmount());
        } else {
            log.info("ℹ️ Pagamento já estava COMPLETED, nenhuma ação necessária");
        }
    }

    /**
     * Salvar histórico de mudança de status
     */
    private void saveStatusHistory(Payment payment, Payment.PaymentStatus newStatus,
            String asaasEvent, String details) {
        PaymentStatusHistory history = PaymentStatusHistory.builder()
                .payment(payment)
                .status(newStatus)
                .asaasEvent(asaasEvent)
                .details(details)
                .build();

        paymentStatusHistoryRepository.save(history);
        log.debug("Histórico de status salvo: Payment ID={}, Status={}, Event={}",
                payment.getId(), newStatus, asaasEvent);
    }

    /**
     * Pagamento vencido
     */
    private void handlePaymentOverdue(Map<String, Object> paymentData) {
        if (paymentData == null)
            return;

        String asaasPaymentId = (String) paymentData.get("id");
        log.info("Pagamento vencido: {}", asaasPaymentId);

        Payment payment = paymentRepository.findByAsaasPaymentId(asaasPaymentId);

        if (payment != null) {
            payment.setStatus(Payment.PaymentStatus.OVERDUE);
            paymentRepository.save(payment);

            saveStatusHistory(payment, Payment.PaymentStatus.OVERDUE, "PAYMENT_OVERDUE",
                    "Pagamento vencido");

            webSocketService.notifyPaymentFailed(
                    payment.getId(),
                    payment.getContractor().getEmail(),
                    "Pagamento vencido");

            log.info("Status do pagamento atualizado para OVERDUE: {}", asaasPaymentId);
        }
    }

    // ==================== EVENTOS DE AUTORIZAÇÃO E ANÁLISE DE RISCO
    // ====================

    /**
     * Pagamento autorizado (aguardando captura)
     */
    private void handlePaymentAuthorized(Map<String, Object> paymentData) {
        if (paymentData == null)
            return;
        String asaasPaymentId = (String) paymentData.get("id");
        log.info("Pagamento autorizado (aguardando captura): {}", asaasPaymentId);
    }

    /**
     * Pagamento aguardando análise de risco
     */
    private void handlePaymentAwaitingRiskAnalysis(Map<String, Object> paymentData) {
        if (paymentData == null)
            return;
        String asaasPaymentId = (String) paymentData.get("id");
        log.info("Pagamento aguardando análise de risco: {}", asaasPaymentId);
    }

    /**
     * Pagamento aprovado pela análise de risco
     */
    private void handlePaymentApprovedByRiskAnalysis(Map<String, Object> paymentData) {
        if (paymentData == null)
            return;
        String asaasPaymentId = (String) paymentData.get("id");
        log.info("Pagamento aprovado pela análise de risco: {}", asaasPaymentId);
    }

    /**
     * Pagamento reprovado pela análise de risco
     */
    private void handlePaymentReprovedByRiskAnalysis(Map<String, Object> paymentData) {
        if (paymentData == null)
            return;

        String asaasPaymentId = (String) paymentData.get("id");

        Payment payment = paymentRepository.findByAsaasPaymentId(asaasPaymentId);

        if (payment != null) {
            payment.setStatus(Payment.PaymentStatus.FAILED);
            paymentRepository.save(payment);

            saveStatusHistory(payment, Payment.PaymentStatus.FAILED, "PAYMENT_REPROVED_BY_RISK_ANALYSIS",
                    "Pagamento reprovado pela análise de risco");

            webSocketService.notifyPaymentFailed(
                    payment.getId(),
                    payment.getContractor().getEmail(),
                    "Pagamento reprovado pela análise de risco");

            log.warn("Pagamento reprovado pela análise de risco: {}", asaasPaymentId);
        }
    }

    // ==================== EVENTOS DE CRIAÇÃO E ATUALIZAÇÃO ====================

    /**
     * Pagamento criado
     */
    private void handlePaymentCreated(Map<String, Object> paymentData) {
        if (paymentData == null)
            return;
        String asaasPaymentId = (String) paymentData.get("id");
        log.info("Pagamento criado no Asaas: {}", asaasPaymentId);
    }

    /**
     * Pagamento atualizado
     */
    private void handlePaymentUpdated(Map<String, Object> paymentData) {
        if (paymentData == null)
            return;
        String asaasPaymentId = (String) paymentData.get("id");
        log.info("Pagamento atualizado: {}", asaasPaymentId);
    }

    /**
     * Captura de cartão recusada
     */
    private void handlePaymentCaptureRefused(Map<String, Object> paymentData) {
        if (paymentData == null)
            return;

        String asaasPaymentId = (String) paymentData.get("id");

        List<Payment> payments = paymentRepository.findAll().stream()
                .filter(p -> asaasPaymentId.equals(p.getAsaasPaymentId()))
                .toList();

        if (!payments.isEmpty()) {
            Payment payment = payments.get(0);
            payment.setStatus(Payment.PaymentStatus.FAILED);
            paymentRepository.save(payment);

            saveStatusHistory(payment, Payment.PaymentStatus.FAILED, "PAYMENT_CREDIT_CARD_CAPTURE_REFUSED",
                    "Captura de cartão recusada");

            webSocketService.notifyPaymentFailed(
                    payment.getId(),
                    payment.getContractor().getEmail(),
                    "Captura de cartão recusada");

            log.error("Captura de cartão recusada: {}", asaasPaymentId);
        }
    }

    // ==================== EVENTOS DE ESTORNO ====================

    /**
     * Estorno em progresso
     */
    private void handlePaymentRefundInProgress(Map<String, Object> paymentData) {
        if (paymentData == null)
            return;
        String asaasPaymentId = (String) paymentData.get("id");
        log.info("Estorno em progresso: {}", asaasPaymentId);
    }

    /**
     * Estorno negado
     */
    private void handlePaymentRefundDenied(Map<String, Object> paymentData) {
        if (paymentData == null)
            return;
        String asaasPaymentId = (String) paymentData.get("id");
        log.warn("Estorno negado: {}", asaasPaymentId);
    }

    // ==================== EVENTOS DE CHARGEBACK ====================

    /**
     * Chargeback solicitado
     */
    private void handlePaymentChargebackRequested(Map<String, Object> paymentData) {
        if (paymentData == null)
            return;

        String asaasPaymentId = (String) paymentData.get("id");

        List<Payment> payments = paymentRepository.findAll().stream()
                .filter(p -> asaasPaymentId.equals(p.getAsaasPaymentId()))
                .toList();

        if (!payments.isEmpty()) {
            Payment payment = payments.get(0);
            payment.setStatus(Payment.PaymentStatus.CHARGEBACK);
            paymentRepository.save(payment);

            saveStatusHistory(payment, Payment.PaymentStatus.CHARGEBACK, "PAYMENT_CHARGEBACK_REQUESTED",
                    "Chargeback solicitado");

            log.error("⚠️ CHARGEBACK solicitado: {} - R$ {}",
                    asaasPaymentId, payment.getAmount());
        }
    }

    /**
     * Chargeback em disputa
     */
    private void handlePaymentChargebackDispute(Map<String, Object> paymentData) {
        if (paymentData == null)
            return;
        String asaasPaymentId = (String) paymentData.get("id");
        log.info("Chargeback em disputa: {}", asaasPaymentId);
    }

    /**
     * Aguardando reversão de chargeback
     */
    private void handlePaymentAwaitingChargebackReversal(Map<String, Object> paymentData) {
        if (paymentData == null)
            return;
        String asaasPaymentId = (String) paymentData.get("id");
        log.info("Aguardando reversão de chargeback (disputa vencida): {}", asaasPaymentId);
    }

    // ==================== EVENTOS DE REMOÇÃO E RESTAURAÇÃO ====================

    /**
     * Pagamento deletado
     */
    private void handlePaymentDeleted(Map<String, Object> paymentData) {
        if (paymentData == null)
            return;

        String asaasPaymentId = (String) paymentData.get("id");

        List<Payment> payments = paymentRepository.findAll().stream()
                .filter(p -> asaasPaymentId.equals(p.getAsaasPaymentId()))
                .toList();

        if (!payments.isEmpty()) {
            Payment payment = payments.get(0);
            payment.setStatus(Payment.PaymentStatus.CANCELLED);
            paymentRepository.save(payment);

            saveStatusHistory(payment, Payment.PaymentStatus.CANCELLED, "PAYMENT_DELETED",
                    "Pagamento deletado/cancelado");

            webSocketService.notifyPaymentFailed(
                    payment.getId(),
                    payment.getContractor().getEmail(),
                    "Pagamento cancelado/deletado");

            log.info("Pagamento deletado: {}", asaasPaymentId);
        }
    }

    /**
     * Pagamento restaurado
     */
    private void handlePaymentRestored(Map<String, Object> paymentData) {
        if (paymentData == null)
            return;

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
        if (paymentData == null)
            return;
        String asaasPaymentId = (String) paymentData.get("id");
        log.warn("Split cancelado: {}", asaasPaymentId);
    }

    /**
     * Bloqueio por divergência de split
     */
    private void handlePaymentSplitDivergenceBlock(Map<String, Object> paymentData) {
        if (paymentData == null)
            return;
        String asaasPaymentId = (String) paymentData.get("id");
        log.warn("Valor bloqueado por divergência de split: {}", asaasPaymentId);
    }

    /**
     * Bloqueio por divergência de split finalizado
     */
    private void handlePaymentSplitDivergenceBlockFinished(Map<String, Object> paymentData) {
        if (paymentData == null)
            return;
        String asaasPaymentId = (String) paymentData.get("id");
        log.info("Bloqueio por divergência de split finalizado: {}", asaasPaymentId);
    }

    // ==================== EVENTOS DE VISUALIZAÇÃO ====================

    /**
     * Boleto visualizado
     */
    private void handlePaymentBankSlipViewed(Map<String, Object> paymentData) {
        if (paymentData == null)
            return;
        String asaasPaymentId = (String) paymentData.get("id");
        log.info("Boleto visualizado pelo cliente: {}", asaasPaymentId);
    }

    /**
     * Checkout visualizado
     */
    private void handlePaymentCheckoutViewed(Map<String, Object> paymentData) {
        if (paymentData == null)
            return;
        String asaasPaymentId = (String) paymentData.get("id");
        log.info("Checkout visualizado pelo cliente: {}", asaasPaymentId);
    }

    /**
     * Boleto cancelado por expiração
     */
    private void handlePaymentBankSlipCancelled(Map<String, Object> paymentData) {
        if (paymentData == null)
            return;

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
        if (paymentData == null)
            return;
        String asaasPaymentId = (String) paymentData.get("id");
        log.info("Pagamento antecipado: {}", asaasPaymentId);
    }

    /**
     * Negativação solicitada
     */
    private void handlePaymentDunningRequested(Map<String, Object> paymentData) {
        if (paymentData == null)
            return;
        String asaasPaymentId = (String) paymentData.get("id");
        log.warn("Negativação solicitada: {}", asaasPaymentId);
    }

    /**
     * Negativação recebida
     */
    private void handlePaymentDunningReceived(Map<String, Object> paymentData) {
        if (paymentData == null)
            return;
        String asaasPaymentId = (String) paymentData.get("id");
        log.info("Recebimento de negativação: {}", asaasPaymentId);
    }

    /**
     * Recebimento em dinheiro desfeito
     */
    private void handlePaymentReceivedInCashUndone(Map<String, Object> paymentData) {
        if (paymentData == null)
            return;

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
        if (paymentData == null)
            return;

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

        saveStatusHistory(payment, Payment.PaymentStatus.REFUNDED, "PAYMENT_REFUNDED",
                "Pagamento reembolsado/estornado");

        webSocketService.notifyPaymentStatusChange(
                payment.getId(), "REFUNDED", payment.getContractor().getEmail());
        if (payment.getFreelancer() != null) {
            webSocketService.notifyPaymentStatusChange(
                    payment.getId(), "REFUNDED", payment.getFreelancer().getEmail());
        }

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
        if (transferData == null)
            return;

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
                withdrawal.getAmount());

        // Notificar freelancer sobre falha no saque
        webSocketService.notifyPaymentFailed(
                null,
                withdrawal.getFreelancer().getEmail(),
                "Falha no saque" + (failReason != null ? ": " + failReason : ""));

        log.error("Saque falhou via webhook: {} - Motivo: {}",
                asaasTransferId, failReason);
    }

    // ==================== PROCESSAMENTO DE TRANSAÇÕES PIX ====================

    /**
     * Processa webhook de transação PIX recebida (via QR Code estático ou chave PIX)
     * 
     * Status possíveis:
     * - AWAITING_CRITICAL_ACTION_AUTHORIZATION: Aguardando autorização de ação crítica
     * - DONE: Transação concluída com sucesso
     * - CANCELLED: Transação cancelada
     * - PENDING: Pendente
     * - SCHEDULED: Agendada
     * - IN_BANK_PROCESSING: Em processamento bancário
     * - FAILED: Falhou
     * - REFUNDED: Estornada
     * - PARTIALLY_REFUNDED: Parcialmente estornada
     */
    @Transactional
    public void processPixTransactionWebhook(Map<String, Object> payload) {
        try {
            // Converter payload para DTO
            PixTransactionWebhookDTO pixTransaction = objectMapper.convertValue(payload, PixTransactionWebhookDTO.class);

            String transactionId = pixTransaction.getId();
            String status = pixTransaction.getStatus();
            String originType = pixTransaction.getOriginType();
            BigDecimal value = pixTransaction.getValue();
            String conciliationId = pixTransaction.getConciliationIdentifier();
            String endToEndId = pixTransaction.getEndToEndIdentifier();

            log.info("🔔 Processando PIX Transaction - ID: {}, Status: {}, Origem: {}, Valor: R$ {}",
                    transactionId, status, originType, value);

            // Tentar encontrar o pagamento relacionado
            Payment payment = findPaymentByPixTransaction(pixTransaction);

            if (payment == null) {
                log.warn("⚠️ Pagamento não encontrado para transação PIX: {} (conciliação: {})",
                        transactionId, conciliationId);
                log.info("📋 Dados da transação não vinculada: Origem={}, Valor=R${}, Pagador={}",
                        originType, value,
                        pixTransaction.getExternalAccount() != null ? pixTransaction.getExternalAccount().getName() : "N/A");
                return;
            }

            // Atualizar informações da transação PIX no pagamento
            payment.setPixTransactionId(transactionId);
            payment.setPixEndToEndId(endToEndId);
            payment.setPixTransactionStatus(status);

            // Processar baseado no status
            switch (status) {
                case "DONE":
                    handlePixTransactionDone(payment, pixTransaction);
                    break;

                case "AWAITING_CRITICAL_ACTION_AUTHORIZATION":
                    handlePixTransactionAwaitingAuthorization(payment, pixTransaction);
                    break;

                case "CANCELLED":
                    handlePixTransactionCancelled(payment, pixTransaction);
                    break;

                case "FAILED":
                    handlePixTransactionFailed(payment, pixTransaction);
                    break;

                case "REFUNDED":
                case "PARTIALLY_REFUNDED":
                    handlePixTransactionRefunded(payment, pixTransaction);
                    break;

                case "PENDING":
                case "SCHEDULED":
                case "IN_BANK_PROCESSING":
                    handlePixTransactionPending(payment, pixTransaction);
                    break;

                default:
                    log.info("Status de transação PIX não tratado especificamente: {}", status);
                    paymentRepository.save(payment);
            }

        } catch (Exception e) {
            log.error("Erro ao processar webhook de transação PIX: {}", e.getMessage(), e);
            throw new RuntimeException("Erro ao processar webhook de transação PIX", e);
        }
    }

    /**
     * Tenta encontrar o pagamento relacionado à transação PIX
     */
    private Payment findPaymentByPixTransaction(PixTransactionWebhookDTO pixTransaction) {
        // 1. Buscar por pixTransactionId (se já foi vinculado antes)
        if (pixTransaction.getId() != null) {
            Payment byTransactionId = paymentRepository.findByPixTransactionId(pixTransaction.getId());
            if (byTransactionId != null) {
                return byTransactionId;
            }
        }

        // 2. Buscar por valor e status PROCESSING/PENDING (pagamento PIX aguardando confirmação)
        BigDecimal value = pixTransaction.getValue();
        if (value != null) {
            List<Payment> pendingPixPayments = paymentRepository.findByPaymentMethodAndStatusInAndTotalAmount(
                    "pix",
                    List.of(Payment.PaymentStatus.PROCESSING, Payment.PaymentStatus.PENDING),
                    value);

            if (pendingPixPayments.size() == 1) {
                return pendingPixPayments.get(0);
            } else if (pendingPixPayments.size() > 1) {
                log.warn("Múltiplos pagamentos PIX pendentes com valor R$ {} encontrados. Não foi possível vincular automaticamente.",
                        value);
            }
        }

        // 3. Se tiver externalReference, tentar buscar por ela
        String externalRef = pixTransaction.getExternalReference();
        if (externalRef != null && !externalRef.isEmpty()) {
            try {
                Long paymentId = Long.parseLong(externalRef);
                return paymentRepository.findById(paymentId).orElse(null);
            } catch (NumberFormatException e) {
                // externalReference não é um ID numérico
            }
        }

        return null;
    }

    /**
     * Transação PIX concluída (DONE) - Pagamento confirmado
     */
    private void handlePixTransactionDone(Payment payment, PixTransactionWebhookDTO pixTransaction) {
        log.info("✅ PIX Transaction DONE - Payment ID: {}, Valor: R$ {}",
                payment.getId(), pixTransaction.getValue());

        if (payment.getStatus() != Payment.PaymentStatus.COMPLETED) {
            payment.setStatus(Payment.PaymentStatus.COMPLETED);
            payment.setCompletedAt(LocalDateTime.now());
            paymentRepository.save(payment);

            // Registrar no histórico
            saveStatusHistory(payment, Payment.PaymentStatus.COMPLETED, "PIX_TRANSACTION_DONE",
                    "Transação PIX concluída com sucesso. ID: " + pixTransaction.getId());

            // Adicionar saldo pendente na carteira do freelancer
            walletService.addPendingBalance(
                    payment.getFreelancer(),
                    payment.getAmount());

            log.info("💰 Saldo adicionado à carteira do freelancer: R$ {}", payment.getAmount());

            // Atualizar status da solicitação para IN_PROGRESS e habilitar chat
            ServiceRequest serviceRequest = payment.getServiceRequest();
            if (serviceRequest.getStatus() != ServiceRequest.Status.IN_PROGRESS && 
                serviceRequest.getStatus() != ServiceRequest.Status.COMPLETED) {
                serviceRequest.setStatus(ServiceRequest.Status.IN_PROGRESS);
                serviceRequest.setChatEnabled(true);
                serviceRequestRepository.save(serviceRequest);
                log.info("🔓 Chat habilitado! Solicitação ID: {} - Status: IN_PROGRESS", serviceRequest.getId());
                
                // Registrar no histórico da solicitação
                serviceRequestHistoryService.record(
                        serviceRequest,
                        payment.getContractor(),
                        ServiceRequestHistory.OperationType.PAYMENT_CONFIRMED,
                        "Pagamento PIX de R$ " + String.format("%.2f", payment.getTotalAmount()) + " confirmado");
                
                // Notificar mudança de status da solicitação
                webSocketService.notifyServiceRequestStatusChange(
                        serviceRequest.getId(),
                        "IN_PROGRESS",
                        payment.getContractor().getEmail(),
                        payment.getFreelancer().getEmail());
            } else if (!Boolean.TRUE.equals(serviceRequest.getChatEnabled())) {
                serviceRequest.setChatEnabled(true);
                serviceRequestRepository.save(serviceRequest);
                log.info("🔓 Chat habilitado (solicitação já estava {})", serviceRequest.getStatus());
            }

            // Notificar pagamento confirmado via WebSocket - CLIENTE E FREELANCER
            webSocketService.notifyPixPaymentConfirmed(
                    payment.getId(),
                    payment.getContractor().getEmail());
            
            webSocketService.notifyPixPaymentConfirmed(
                    payment.getId(),
                    payment.getFreelancer().getEmail());

            log.info("✅ Pagamento PIX confirmado via webhook: Payment ID={}, Transaction ID={}, Valor=R$ {}",
                    payment.getId(), pixTransaction.getId(), payment.getAmount());
        } else {
            paymentRepository.save(payment);
            log.info("ℹ️ Pagamento já estava COMPLETED, apenas atualizando dados da transação PIX");
        }
    }

    /**
     * Transação PIX aguardando autorização de ação crítica
     */
    private void handlePixTransactionAwaitingAuthorization(Payment payment, PixTransactionWebhookDTO pixTransaction) {
        log.info("⏳ PIX Transaction AWAITING_CRITICAL_ACTION_AUTHORIZATION - Payment ID: {}",
                payment.getId());

        // Manter status como PROCESSING
        if (payment.getStatus() == Payment.PaymentStatus.PENDING) {
            payment.setStatus(Payment.PaymentStatus.PROCESSING);
        }
        paymentRepository.save(payment);

        saveStatusHistory(payment, payment.getStatus(), "PIX_TRANSACTION_AWAITING_AUTHORIZATION",
                "Transação PIX aguardando autorização de ação crítica. ID: " + pixTransaction.getId());
    }

    /**
     * Transação PIX cancelada
     */
    private void handlePixTransactionCancelled(Payment payment, PixTransactionWebhookDTO pixTransaction) {
        log.warn("❌ PIX Transaction CANCELLED - Payment ID: {}", payment.getId());

        payment.setStatus(Payment.PaymentStatus.CANCELLED);
        paymentRepository.save(payment);

        saveStatusHistory(payment, Payment.PaymentStatus.CANCELLED, "PIX_TRANSACTION_CANCELLED",
                "Transação PIX cancelada. ID: " + pixTransaction.getId());

        // Registrar no histórico da solicitação
        ServiceRequest serviceRequest = payment.getServiceRequest();
        serviceRequestHistoryService.record(
                serviceRequest,
                payment.getContractor(),
                ServiceRequestHistory.OperationType.PAYMENT_CANCELLED,
                "Pagamento PIX cancelado");

        // Notificar via WebSocket
        webSocketService.notifyPaymentFailed(
                payment.getId(),
                payment.getContractor().getEmail(),
                "Transação PIX cancelada");
    }

    /**
     * Transação PIX falhou
     */
    private void handlePixTransactionFailed(Payment payment, PixTransactionWebhookDTO pixTransaction) {
        log.error("❌ PIX Transaction FAILED - Payment ID: {}, Motivo: {}",
                payment.getId(), pixTransaction.getRefusalReason());

        payment.setStatus(Payment.PaymentStatus.FAILED);
        paymentRepository.save(payment);

        saveStatusHistory(payment, Payment.PaymentStatus.FAILED, "PIX_TRANSACTION_FAILED",
                "Transação PIX falhou. ID: " + pixTransaction.getId() +
                        (pixTransaction.getRefusalReason() != null ? " Motivo: " + pixTransaction.getRefusalReason() : ""));

        // Registrar no histórico da solicitação
        ServiceRequest serviceRequest = payment.getServiceRequest();
        String reason = pixTransaction.getRefusalReason() != null ? ": " + pixTransaction.getRefusalReason() : "";
        serviceRequestHistoryService.record(
                serviceRequest,
                payment.getContractor(),
                ServiceRequestHistory.OperationType.PAYMENT_FAILED,
                "Pagamento PIX falhou" + reason);

        // Notificar via WebSocket
        webSocketService.notifyPaymentFailed(
                payment.getId(),
                payment.getContractor().getEmail(),
                "Transação PIX falhou" + (pixTransaction.getRefusalReason() != null ?
                        ": " + pixTransaction.getRefusalReason() : ""));
    }

    /**
     * Transação PIX estornada
     */
    private void handlePixTransactionRefunded(Payment payment, PixTransactionWebhookDTO pixTransaction) {
        log.warn("↩️ PIX Transaction REFUNDED - Payment ID: {}, Valor estornado: R$ {}",
                payment.getId(), pixTransaction.getRefundedValue());

        payment.setStatus(Payment.PaymentStatus.REFUNDED);
        paymentRepository.save(payment);

        saveStatusHistory(payment, Payment.PaymentStatus.REFUNDED, "PIX_TRANSACTION_REFUNDED",
                "Transação PIX estornada. ID: " + pixTransaction.getId() +
                        " Valor estornado: R$ " + pixTransaction.getRefundedValue());
    }

    /**
     * Transação PIX pendente/agendada/em processamento
     */
    private void handlePixTransactionPending(Payment payment, PixTransactionWebhookDTO pixTransaction) {
        log.info("⏳ PIX Transaction {} - Payment ID: {}", pixTransaction.getStatus(), payment.getId());

        if (payment.getStatus() == Payment.PaymentStatus.PENDING) {
            payment.setStatus(Payment.PaymentStatus.PROCESSING);
        }
        paymentRepository.save(payment);

        saveStatusHistory(payment, payment.getStatus(), "PIX_TRANSACTION_" + pixTransaction.getStatus(),
                "Transação PIX " + pixTransaction.getStatus() + ". ID: " + pixTransaction.getId());
    }
}
