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
public class FreelancerProfileResponse {
    private Long id;
    private String name;
    private String email;
    private String profession;
    private Long categoryId;
    private String categorySlug;
    private String categoryLabel;
    private String category;
    private Long subcategoryId;
    private List<Long> subcategoryIds;
    private String subcategorySlug;
    private String subcategoryLabel;
    private String subcategory;
    private List<String> subcategoryLabels;
    private String bio;
    private String profilePhoto;
    private String serviceType;
    private String location;
    private List<String> cities;
    private List<ServiceCityResponse> serviceCities;
    private SocialLinks socialLinks;
    private List<ProfessionalLinkResponse> professionalLinks;
    private BigDecimal rating;
    private Integer totalReviews;
    private Integer completedJobs;
    private Boolean available;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SocialLinks {
        private String website;
        private String linkedin;
        private String instagram;
        private String portfolio;
    }
}

