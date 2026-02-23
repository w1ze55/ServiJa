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
public class FreelancerResponseDTO {

    private BigDecimal value;
    private Integer executionDays;
    private String scope;
    private String notes;
}
