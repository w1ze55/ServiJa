package com.servija.servija.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRequestDTO {

    @NotBlank(message = "O nome do serviço é obrigatório")
    private String serviceName;

    @NotBlank(message = "A categoria é obrigatória")
    private String category;

    @NotBlank(message = "A descrição é obrigatória")
    private String description;

    @NotNull(message = "O orçamento é obrigatório")
    @DecimalMin(value = "5.00", message = "O orçamento mínimo é R$ 5,00")
    @DecimalMax(value = "1000000.00", message = "O orçamento máximo é R$ 1.000.000,00")
    private BigDecimal budget;

    @NotBlank(message = "O prazo é obrigatório")
    @Size(max = 50, message = "O prazo deve ter no máximo 50 caracteres")
    private String deadline;

    @Pattern(regexp = "^(LOW|MEDIUM|HIGH|URGENT)$", message = "Prioridade inválida")
    private String priority;

    @Size(max = 500, message = "O endereço deve ter no máximo 500 caracteres")
    private String address;

    private String desiredDate;

    private List<String> attachments;

    private Long serviceId;

    private Long freelancerId;
}
