package com.servija.servija.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BankAccountDTO {

    @NotNull(message = "O tipo de conta é obrigatório")
    private String accountType; // PIX_KEY, BANK_ACCOUNT

    // Campos para conta bancária (TED)
    private String bankName;
    private String bankCode;
    private String agency;
    private String agencyDigit;
    private String accountNumber;
    private String accountDigit;
    private String accountBankType; // CONTA_CORRENTE, CONTA_POUPANCA

    // Campos para chave PIX
    private String pixKey;
    private String pixKeyType; // CPF, CNPJ, EMAIL, PHONE, RANDOM

    @NotBlank(message = "O nome do titular é obrigatório")
    private String holderName;

    @NotBlank(message = "O CPF/CNPJ do titular é obrigatório")
    private String holderCpfCnpj;

    private Boolean isDefault;
}
