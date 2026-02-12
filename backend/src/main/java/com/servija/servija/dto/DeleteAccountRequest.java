package com.servija.servija.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeleteAccountRequest {
    
    @NotBlank(message = "Senha é obrigatória")
    private String password;
    
    private String reason;
}
