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
public class ServiceCategoryRequest {

    @NotBlank(message = "O nome da categoria e obrigatorio")
    @Size(max = 150, message = "O nome da categoria deve ter no maximo 150 caracteres")
    private String name;

    @NotNull(message = "A ordem da categoria e obrigatoria")
    private Integer sortOrder;
}
