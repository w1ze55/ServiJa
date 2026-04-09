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
    private Long categoryId;
    private String categorySlug;
    private String categoryLabel;
    private String category;
    private Long subcategoryId;
    private String subcategorySlug;
    private String subcategoryLabel;
    private String subcategory;
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
