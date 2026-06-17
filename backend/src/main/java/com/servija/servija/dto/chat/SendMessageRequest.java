package com.servija.servija.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SendMessageRequest(
    // M13: limita tamanho do conteúdo e dos metadados de anexo (storage/broadcast).
    @NotBlank @Size(max = 4000) String content,
    @Pattern(regexp = "text|image|file|audio") String type,
    @Size(max = 1024) String attachmentUrl,
    @Size(max = 255) String attachmentName
) {}
