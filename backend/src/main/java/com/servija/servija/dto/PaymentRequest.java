package com.servija.servija.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentRequest {

    @NotNull(message = "O ID da solicitação é obrigatório")
    private Long serviceRequestId;

    @NotNull(message = "O método de pagamento é obrigatório")
    private String paymentMethod;

    @NotNull(message = "O valor é obrigatório")
    @Positive(message = "O valor deve ser positivo")
    private BigDecimal amount;

    private String cpf; // CPF do pagador (opcional, mas recomendado para PIX)

    private CardData cardData;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CardData {
        private String number;
        private String name;
        private String expiry;
        private String cvv;
        private Integer installmentCount;
    }
}
