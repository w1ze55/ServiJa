package com.servija.servija.dto;

import com.servija.servija.model.SupportRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SupportRequestCreateRequest {

    private Long userId;

    private String userName;

    private String userEmail;

    @NotNull(message = "A categoria de suporte e obrigatoria")
    private SupportRequest.Category supportCategory;

    @NotBlank(message = "A descricao e obrigatoria")
    @Size(max = 5000, message = "A descricao deve ter no maximo 5000 caracteres")
    private String description;

    private List<SupportAttachmentDto> attachments;
}
