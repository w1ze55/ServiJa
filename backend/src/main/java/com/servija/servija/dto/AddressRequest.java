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
public class AddressRequest {

    @NotBlank(message = "Identificação é obrigatória")
    @Size(max = 50, message = "Identificação deve ter no máximo 50 caracteres")
    private String label;

    @NotBlank(message = "Rua é obrigatória")
    @Size(max = 255, message = "Rua deve ter no máximo 255 caracteres")
    private String street;

    @NotBlank(message = "Número é obrigatório")
    @Size(max = 20, message = "Número deve ter no máximo 20 caracteres")
    private String number;

    @Size(max = 100, message = "Complemento deve ter no máximo 100 caracteres")
    private String complement;

    @NotBlank(message = "Bairro é obrigatório")
    @Size(max = 100, message = "Bairro deve ter no máximo 100 caracteres")
    private String neighborhood;

    @NotBlank(message = "Cidade é obrigatória")
    @Size(max = 100, message = "Cidade deve ter no máximo 100 caracteres")
    private String city;

    @NotBlank(message = "Estado é obrigatório")
    @Size(min = 2, max = 2, message = "Estado deve ter 2 caracteres")
    private String state;

    @NotBlank(message = "CEP é obrigatório")
    @Size(max = 10, message = "CEP deve ter no máximo 10 caracteres")
    private String zipCode;

    private Boolean isDefault;
}
