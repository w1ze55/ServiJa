package com.servija.servija.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankAccountResponse {

    private Long id;
    private String accountType;

    // Dados bancários
    private String bankName;
    private String bankCode;
    private String agency;
    private String accountNumber;
    private String accountBankType;

    // Dados PIX
    private String pixKey;
    private String pixKeyValue;
    private String pixKeyType;

    // Titular
    private String holderName;
    private String holderCpfCnpj;

    private Boolean isDefault;
    private LocalDateTime createdAt;
}
