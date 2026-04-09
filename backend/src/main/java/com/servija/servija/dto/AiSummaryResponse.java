package com.servija.servija.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiSummaryResponse {

    private boolean available;
    private List<String> strengths;
    private List<String> attentionPoints;
    private String overall;
    private String generatedAt;

    public static AiSummaryResponse unavailable() {
        return AiSummaryResponse.builder()
                .available(false)
                .strengths(List.of())
                .attentionPoints(List.of())
                .overall("")
                .build();
    }
}
