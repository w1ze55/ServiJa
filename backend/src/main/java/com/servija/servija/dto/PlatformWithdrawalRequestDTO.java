package com.servija.servija.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlatformWithdrawalRequestDTO {

    @NotNull(message = "O valor do saque é obrigatório")
    @DecimalMin(value = "0.01", message = "O valor do saque deve ser maior que zero")
    private BigDecimal amount;

    @NotNull(message = "A conta de destino é obrigatória")
    private Long bankAccountId;
}
