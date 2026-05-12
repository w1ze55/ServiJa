package com.servija.servija.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NearbyFreelancerResponse {
    private Long id;
    private String name;
    private String avatar;
    private String professionalTitle;
    private BigDecimal rating;
    private Integer totalReviews;
    private String location;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private Double distance; // distância em km
    private Boolean available;
    private String category;
    private Integer completedJobs;
    private Long categoryId;
    private String categorySlug;
    private String categoryLabel;
    private Long subcategoryId;
    private String subcategorySlug;
    private String subcategoryLabel;
    private List<Long> subcategoryIds;
    private List<String> subcategoryLabels;
}
