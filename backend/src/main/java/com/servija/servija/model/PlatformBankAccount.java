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

import java.time.LocalDateTime;

@Entity
@Table(name = "platform_bank_accounts")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformBankAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    @Builder.Default
    private AccountType accountType = AccountType.PIX_KEY;

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
    private BankAccountType accountBankType;

    @Column(name = "pix_key", length = 255)
    private String pixKey;

    @Column(name = "pix_key_type", length = 20)
    private String pixKeyType;

    @Column(name = "holder_name", nullable = false, length = 255)
    private String holderName;

    @Column(name = "holder_cpf_cnpj", nullable = false, length = 20)
    private String holderCpfCnpj;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private Boolean isDefault = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum AccountType {
        PIX_KEY, BANK_ACCOUNT
    }

    public enum BankAccountType {
        CONTA_CORRENTE, CONTA_POUPANCA
    }
}
