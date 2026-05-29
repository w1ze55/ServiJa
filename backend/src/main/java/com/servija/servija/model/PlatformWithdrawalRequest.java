package com.servija.servija.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "platform_withdrawal_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformWithdrawalRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private WithdrawalStatus status = WithdrawalStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "withdrawal_method", nullable = false, length = 20)
    private WithdrawalMethod withdrawalMethod;

    @Column(name = "bank_account_id", nullable = false)
    private Long bankAccountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "bank_account_type", nullable = false, length = 20)
    private PlatformBankAccount.AccountType bankAccountType;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "bank_code", length = 10)
    private String bankCode;

    @Column(length = 10)
    private String agency;

    @Column(name = "agency_digit", length = 2)
    private String agencyDigit;

    @Column(name = "account_number", length = 20)
    private String accountNumber;

    @Column(name = "account_digit", length = 2)
    private String accountDigit;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_bank_type", length = 20)
    private PlatformBankAccount.BankAccountType accountBankType;

    @Column(name = "pix_key", length = 255)
    private String pixKey;

    @Column(name = "pix_key_type", length = 20)
    private String pixKeyType;

    @Column(name = "holder_name", nullable = false, length = 255)
    private String holderName;

    @Column(name = "holder_cpf_cnpj", nullable = false, length = 20)
    private String holderCpfCnpj;

    @Column(name = "destination_label", nullable = false, length = 255)
    private String destinationLabel;

    @Column(name = "asaas_transfer_id", length = 255)
    private String asaasTransferId;

    @Column(name = "transaction_id", nullable = false, length = 255)
    private String transactionId;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum WithdrawalStatus {
        PENDING, PROCESSING, COMPLETED, FAILED, CANCELLED
    }

    public enum WithdrawalMethod {
        PIX, TED
    }
}
