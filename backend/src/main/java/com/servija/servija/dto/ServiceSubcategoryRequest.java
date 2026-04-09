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
public class ServiceSubcategoryRequest {

    @NotNull(message = "A categoria pai e obrigatoria")
    private Long categoryId;

    @NotBlank(message = "O nome da subcategoria e obrigatorio")
    @Size(max = 160, message = "O nome da subcategoria deve ter no maximo 160 caracteres")
    private String name;

    @NotNull(message = "A ordem da subcategoria e obrigatoria")
    private Integer sortOrder;
}
