package com.servija.servija.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {

    private Long id;
    private Long serviceRequestId;
    private String serviceName;
    private String reviewerName;
    private String reviewerAvatar;
    private String reviewerType;
    private Integer rating;
    private String comment;
    private List<String> tags;
    private LocalDateTime createdAt;
}
