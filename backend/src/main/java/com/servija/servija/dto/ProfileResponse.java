package com.servija.servija.dto;

import com.servija.servija.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfileResponse {
    
    private Long id;
    private String name;
    private String email;
    private String cpf;
    private String phone;
    private LocalDate birthDate;
    private User.UserType userType;
    private Boolean emailVerified;
    private String avatarUrl;
    private Boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    private ContractorProfile contractorProfile;
    private FreelancerProfile freelancerProfile;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContractorProfile {
        private String preferredLocation;
        private Integer totalContracts;
        private Integer activeContracts;
        private Integer completedContracts;
        private BigDecimal contractorRating;
        private Long contractorTotalReviews;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FreelancerProfile {
        private String bio;
        private String professionalTitle;
        private String location;
        private BigDecimal rating;
        private Integer totalReviews;
        private Integer completedJobs;
        private Integer activeJobs;
        private Boolean profileCompleted;
        private Boolean available;
    }
}
