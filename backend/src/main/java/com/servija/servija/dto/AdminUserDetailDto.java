package com.servija.servija.dto;

import com.servija.servija.model.User;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;


@Data
@Builder
public class AdminUserDetailDto {
    // Common fields
    private Long id;
    private String name;
    private String email;
    private String phone;
    private String cpf;
    private User.UserType userType;
    private Boolean active;
    private Boolean emailVerified;
    private String avatarUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    // FREELANCER fields
    private String bio;
    private String professionalTitle;
    private BigDecimal rating;
    private Integer totalReviews;
    private Integer completedJobs;
    private Integer activeJobs;
    private Boolean kycCompleted;
    private Boolean profileCompleted;
    private String category;
    private Boolean available;

    // CONTRACTOR fields
    private Integer totalContracts;
    private Integer activeContracts;
    private Integer completedContracts;

    // Enriched data: freelancer services + contractor rating (calculated from reviews table)
    private List<AdminServiceSummaryDto> services;
    private BigDecimal contractorRating;
    private Long contractorTotalReviews;
}
