package com.servija.servija.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminResetPasswordLinkResetRequest {

    @NotBlank(message = "Token é obrigatório")
    private String token;

    @NotBlank(message = "Nova senha é obrigatória")
    @Size(min = 8, max = 256, message = "Senha deve ter entre 8 e 256 caracteres")
    private String newPassword;

    @NotBlank(message = "Confirmação de senha é obrigatória")
    private String confirmPassword;
}
