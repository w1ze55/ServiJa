package com.servija.servija.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRequestResponse {

    private Long id;
    private Long contractorId;
    private String contractorName;
    private String contractorAvatar;
    private Long freelancerId;
    private String freelancerName;
    private String freelancerAvatar;
    private String freelancerLocation;
    private String serviceName;
    private Long categoryId;
    private String categorySlug;
    private String categoryLabel;
    private String category;
    private Long subcategoryId;
    private String subcategorySlug;
    private String subcategoryLabel;
    private String subcategory;
    private String description;
    private BigDecimal budget;
    private String deadline;
    private String workModel;
    private String status;
    private String address;
    private LocalDate desiredDate;
    private List<String> attachments;
    private List<String> freelancerAttachments;
    private List<FreelancerAttachmentDTO> freelancerAttachmentDetails;
    private List<String> responseAttachments;

    private BigDecimal responseValue;
    private Integer responseDays;
    private String responseScope;
    private String responseNotes;
    private LocalDateTime respondedAt;

    private BigDecimal counterValue;
    private Integer counterDays;
    private String counterNotes;

    private String cancelReason;
    private String rejectReason;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
