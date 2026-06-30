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
public class ResponseMilestoneDTO {

    private String id;

    @NotBlank(message = "O título do marco é obrigatório")
    private String title;

    private String description;

    @Builder.Default
    private String status = "pending";
}
