package com.servija.servija.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CounterProposalDTO {

    @NotNull(message = "O valor é obrigatório")
    @DecimalMin(value = "40.00", message = "O valor mínimo é R$ 40,00")
    @DecimalMax(value = "1000000.00", message = "O valor máximo é R$ 1.000.000,00")
    private BigDecimal value;

    @NotNull(message = "O prazo é obrigatório")
    @Min(value = 1, message = "O prazo mínimo é de 1 dia")
    @Max(value = 365, message = "O prazo máximo é de 365 dias")
    private Integer executionDays;

    private String notes;
}
