package com.servija.servija.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRequestDTO {

    @NotBlank(message = "O nome do servico e obrigatorio")
    private String serviceName;

    @NotBlank(message = "A categoria e obrigatoria")
    private String category;

    @NotBlank(message = "A descricao e obrigatoria")
    private String description;

    private BigDecimal budget;

    @NotBlank(message = "O prazo e obrigatorio")
    @Size(max = 50, message = "O prazo deve ter no maximo 50 caracteres")
    private String deadline;

    @Pattern(regexp = "^(PRESENCIAL|REMOTO|HIBRIDO)$", message = "Modelo de trabalho invalido")
    private String workModel;

    @Size(max = 500, message = "O endereco deve ter no maximo 500 caracteres")
    private String address;

    private String desiredDate;

    private List<String> attachments;

    private Long serviceId;

    private Long freelancerId;
}
