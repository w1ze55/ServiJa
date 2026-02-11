package com.servija.servija.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfessionalLinkRequest {
    
    @NotNull(message = "Tipo do link é obrigatório")
    private String type;
    
    @NotBlank(message = "URL é obrigatória")
    @Size(max = 500, message = "URL deve ter no máximo 500 caracteres")
    private String url;
    
    @Size(max = 100, message = "Rótulo deve ter no máximo 100 caracteres")
    private String label;
}
