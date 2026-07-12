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
public class ServiceCardResponse {
    private Long id;
    private Long freelancerId;
    private String freelancerName;
    private String freelancerProfilePhoto;
    private String title;
    private String category;
    private String description;
    private Integer deliveryTime;
    private String deliveryUnit;
    private String workModel;
    private List<String> images;
    private BigDecimal price;
    private String priceType;
    private Long completedServicesCount;
    private BigDecimal averagePriceMin;
    private BigDecimal averagePriceMax;
    private BigDecimal serviceRating;
    private Integer serviceReviewCount;
}
