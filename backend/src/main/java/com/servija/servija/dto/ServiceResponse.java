package com.servija.servija.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceResponse {
    private Long id;
    private Long freelancerId;
    private String freelancerName;
    private String title;
    private String category;
    private String description;
    private BigDecimal serviceRating;
    private Integer serviceReviewCount;
    private BigDecimal price;
    private Long completedServicesCount;
    private BigDecimal averageCompletedPrice;
    private BigDecimal averagePriceMin;
    private BigDecimal averagePriceMax;
    private String priceType;
    private Integer deliveryTime;
    private String deliveryUnit;
    private String workModel;
    private List<String> tags;
    private List<String> images;
    private Boolean isActive;
    private String freelancerProfilePhoto;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
