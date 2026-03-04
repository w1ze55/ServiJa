package com.servija.servija.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO para mapear webhooks de transações PIX recebidas do Asaas.
 * Representa transações PIX recebidas via QR Code estático ou chave PIX.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PixTransactionWebhookDTO {

    private String id;
    private String transferId;
    private String endToEndIdentifier;
    private String finality;
    private BigDecimal value;
    private BigDecimal changeValue;
    private BigDecimal refundedValue;
    private String dateCreated;
    private String effectiveDate;
    private String scheduledDate;
    private String status;
    private String type;
    private String originType;
    private String conciliationIdentifier;
    private String description;
    private String transactionReceiptUrl;
    private BigDecimal chargedFeeValue;
    private Boolean canBeRefunded;
    private String refundDisabledReason;
    private String refusalReason;
    private Boolean canBeCanceled;
    private PixTransactionOriginal originalTransaction;
    private ExternalAccount externalAccount;
    private QrCode qrCode;
    private Object payment;
    private String addressKey;
    private String addressKeyType;
    private String externalReference;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ExternalAccount {
        private Long ispb;
        private String ispbName;
        private String name;
        private String agency;
        private String account;
        private String accountDigit;
        private String accountType;
        private String cpfCnpj;
        private String addressKey;
        private String addressKeyType;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class QrCode {
        private String payer;
        private String conciliationIdentifier;
        private BigDecimal originalValue;
        private String dueDate;
        private BigDecimal interest;
        private BigDecimal fine;
        private BigDecimal discount;
        private String expirationDate;
        private String description;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PixTransactionOriginal {
        private String id;
        private String endToEndIdentifier;
    }

    /**
     * Status possíveis de uma transação PIX no Asaas
     */
    public enum PixTransactionStatus {
        AWAITING_CRITICAL_ACTION_AUTHORIZATION,  // Aguardando autorização de ação crítica
        DONE,                                     // Transação concluída
        CANCELLED,                               // Transação cancelada
        PENDING,                                 // Pendente
        SCHEDULED,                               // Agendada
        IN_BANK_PROCESSING,                      // Em processamento bancário
        FAILED,                                  // Falhou
        REFUNDED,                                // Estornada
        PARTIALLY_REFUNDED                       // Parcialmente estornada
    }

    /**
     * Tipos de origem da transação PIX
     */
    public enum PixOriginType {
        STATIC_QRCODE,      // QR Code estático
        DYNAMIC_QRCODE,     // QR Code dinâmico (cobrança)
        MANUAL,             // PIX manual via chave
        AUTOMATIC           // PIX automático
    }

    /**
     * Tipo da transação PIX
     */
    public enum PixTransactionType {
        DEBIT,   // Débito (saída de dinheiro)
        CREDIT   // Crédito (entrada de dinheiro)
    }

    /**
     * Verifica se a transação está concluída
     */
    public boolean isDone() {
        return "DONE".equals(this.status);
    }

    /**
     * Verifica se a transação está aguardando autorização
     */
    public boolean isAwaitingAuthorization() {
        return "AWAITING_CRITICAL_ACTION_AUTHORIZATION".equals(this.status);
    }

    /**
     * Verifica se a transação foi cancelada
     */
    public boolean isCancelled() {
        return "CANCELLED".equals(this.status);
    }

    /**
     * Verifica se a transação falhou
     */
    public boolean isFailed() {
        return "FAILED".equals(this.status);
    }

    /**
     * Verifica se é uma transação de crédito (recebimento)
     */
    public boolean isCredit() {
        return "CREDIT".equals(this.type);
    }

    /**
     * Verifica se é uma transação originada de QR Code estático
     */
    public boolean isFromStaticQrCode() {
        return "STATIC_QRCODE".equals(this.originType);
    }
}
