package com.servija.servija.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRequestDTO {

    private String serviceName;
    private String category;
    private String description;
    private BigDecimal budget;
    private String deadline;
    private String priority; // LOW, MEDIUM, HIGH
    private String address;
    private String desiredDate;
    private List<String> attachments;
    private Long freelancerId;
}
