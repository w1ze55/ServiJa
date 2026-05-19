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

    @NotBlank(message = "Token e obrigatorio")
    private String token;

    @NotBlank(message = "Nova senha e obrigatoria")
    @Size(min = 8, max = 256, message = "Senha deve ter entre 8 e 256 caracteres")
    private String newPassword;

    @NotBlank(message = "Confirmacao de senha e obrigatoria")
    private String confirmPassword;
}
