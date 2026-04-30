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
public class ClientHomeResponse {

    private RegionInfo region;
    private List<String> interestCategories;
    private List<ServiceRequestResponse> recentRequests;
    private List<ServiceRequestResponse> activeProjects;
    private FavoritesInfo favorites;
    private List<NearbyFreelancerResponse> featuredProfessionals;
    private List<ServiceResponse> featuredServices;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RegionInfo {
        private String city;
        private String state;
        private String displayName;
        private Boolean localized;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FavoritesInfo {
        private Integer serviceCount;
        private Integer professionalCount;
        private List<ServiceResponse> services;
        private List<NearbyFreelancerResponse> professionals;
    }
}
