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
public class ServiceRequest {
    private String title;
    private Long categoryId;
    private Long subcategoryId;
    private String category;
    private String subcategory;
    private String description;
    private BigDecimal price;
    private String priceType; // FIXED, HOURLY, DAILY
    private Integer deliveryTime;
    private String deliveryUnit; // DAYS, WEEKS
    private String workModel; // PRESENCIAL, REMOTO, HIBRIDO
    private List<String> tags;
    private List<String> images;
    private Boolean isActive;
}
