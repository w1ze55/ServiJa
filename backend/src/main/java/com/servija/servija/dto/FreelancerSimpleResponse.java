package com.servija.servija.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FreelancerSimpleResponse {
    private Long userId;
    private String name;
    private String professionalTitle;
    private String category;
    private String avatarUrl;
}
