package com.servija.servija.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ImpersonateResponse {
    private String token;
    private LocalDateTime expiresAt;
}
