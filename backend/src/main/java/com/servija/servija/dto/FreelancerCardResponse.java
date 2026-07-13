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
public class FreelancerCardResponse {
    private Long id;
    private String name;
    private String avatar;
    private String location;
    private List<String> categories;
    private Integer servicesCount;
    private BigDecimal rating;
    private Integer reviewCount;
}
