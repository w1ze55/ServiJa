package com.servija.servija.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupportRequestActionRequest {
    @NotBlank(message = "A mensagem de resposta e obrigatoria")
    private String responseMessage;
}
