package com.servija.servija.dto.chat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SendMessageRequest(
    @NotBlank String content,
    @Pattern(regexp = "text|image|file|audio") String type,
    String attachmentUrl,
    String attachmentName
) {}
