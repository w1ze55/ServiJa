package com.servija.servija.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.NotBlank;
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

    @NotBlank(message = "O nome do serviço é obrigatório")
    private String serviceName;

    private Long categoryId;

    private Long subcategoryId;

    private String category;

    private String subcategory;

    @NotBlank(message = "A descrição é obrigatória")
    private String description;

    private BigDecimal budget;

    @NotBlank(message = "O prazo é obrigatório")
    @Size(max = 50, message = "O prazo deve ter no máximo 50 caracteres")
    private String deadline;

    @Pattern(regexp = "^(PRESENCIAL|REMOTO|HIBRIDO)$", message = "Modelo de trabalho inválido")
    private String workModel;

    @Size(max = 500, message = "O endereço deve ter no máximo 500 caracteres")
    private String address;

    private String desiredDate;

    private List<String> attachments;

    private Long serviceId;

    private Long freelancerId;
}
