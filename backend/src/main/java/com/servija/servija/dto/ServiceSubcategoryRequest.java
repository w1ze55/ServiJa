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

    @NotNull(message = "A categoria pai é obrigatória")
    private Long categoryId;

    @NotBlank(message = "O nome da subcategoria é obrigatório")
    @Size(max = 160, message = "O nome da subcategoria deve ter no máximo 160 caracteres")
    private String name;

    @NotNull(message = "A ordem da subcategoria é obrigatória")
    private Integer sortOrder;
}
