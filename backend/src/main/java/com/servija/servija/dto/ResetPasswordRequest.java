package com.servija.servija.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResetPasswordRequest {

    @NotBlank(message = "E-mail e obrigatorio")
    @Email(message = "E-mail invalido")
    private String email;

    @NotBlank(message = "Codigo e obrigatorio")
    @Pattern(regexp = "\\d{6}", message = "Codigo deve conter 6 digitos")
    private String code;

    @NotBlank(message = "Nova senha e obrigatoria")
    @Size(min = 8, max = 256, message = "Senha deve ter entre 8 e 256 caracteres")
    private String newPassword;

    @NotBlank(message = "Confirmacao de senha e obrigatoria")
    private String confirmPassword;
}
