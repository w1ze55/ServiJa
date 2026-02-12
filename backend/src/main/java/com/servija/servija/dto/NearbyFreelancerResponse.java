package com.servija.servija.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

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
    private BigDecimal hourlyRate;
    private String category;
}
