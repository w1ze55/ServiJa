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
public class UpdateAvatarRequest {
    
    @NotBlank(message = "Imagem do avatar é obrigatória")
    private String avatarBase64;
}
