package com.servija.servija.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioProjectResponse {
    private Long id;
    private String title;
    private String description;
    private String category;
    private List<String> tags;
    private String videoUrl;
    private String projectUrl;
    private LocalDate completedDate;
    private String client;
    private Boolean featured;
    private List<ProjectImageResponse> images;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectImageResponse {
        private Long id;
        private String imageUrl;
        private String caption;
        private Integer displayOrder;
    }
}
