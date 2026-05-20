package com.servija.servija.dto;

import com.servija.servija.model.User;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminUserSummaryDto {
    private Long id;
    private String name;
    private String email;
    private User.UserType userType;
    private Boolean active;
    private LocalDateTime createdAt;
    private String avatarUrl;
    private LocalDateTime deletedAt;
}
