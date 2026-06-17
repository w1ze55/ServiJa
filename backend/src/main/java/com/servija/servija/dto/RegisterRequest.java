package com.servija.servija.dto;

import com.servija.servija.model.User;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "Nome é obrigatório")
    @Size(min = 3, max = 100, message = "Nome deve ter entre 3 e 100 caracteres")
    private String name;

    @NotBlank(message = "E-mail é obrigatório")
    @Email(message = "E-mail inválido")
    @Size(max = 256, message = "E-mail deve ter no máximo 256 caracteres")
    private String email;

    @NotBlank(message = "CPF é obrigatório")
    @Pattern(regexp = "\\d{11}", message = "CPF deve conter 11 dígitos")
    private String cpf;

    @NotBlank(message = "Telefone é obrigatório")
    @Pattern(regexp = "\\d{10,11}", message = "Telefone deve conter 10 ou 11 dígitos")
    private String phone;

    @NotNull(message = "Data de nascimento é obrigatória")
    @Past(message = "Data de nascimento inválida")
    private LocalDate birthDate;

    @NotBlank(message = "Senha é obrigatória")
    @Size(min = 8, max = 256, message = "Senha deve ter entre 8 e 256 caracteres")
    @Pattern(
        regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[!@#$%^&*(),.?\":{}|<>]).*$",
        message = "Senha deve conter pelo menos uma letra maiúscula, uma minúscula, um número e um caractere especial"
    )
    private String password;

    @NotNull(message = "Tipo de usuário é obrigatório")
    private User.UserType userType;

    @Valid
    @NotNull(message = "Endereço é obrigatório")
    private AddressRequest address;

    @AssertTrue(message = "Você deve ter pelo menos 18 anos para se cadastrar")
    public boolean isMinimumAgeValid() {
        return birthDate != null && !birthDate.isAfter(LocalDate.now().minusYears(18));
    }

    @AssertTrue(message = "Você deve aceitar os termos e condições")
    private Boolean acceptTerms;
}
