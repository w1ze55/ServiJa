package com.servija.servija.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliverySubmissionDTO {

    @NotBlank(message = "A descrição da entrega é obrigatória")
    private String description;

    private List<FreelancerAttachmentDTO> attachments;
}
