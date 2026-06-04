package com.servija.servija.dto;

import com.servija.servija.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;

    @Builder.Default
    private String tokenType = "Bearer";

    private UserDto user;

    /** When true, the JWT in {@code token} is a short-lived challenge token; client must call /api/auth/2fa/verify. */
    private Boolean requires2fa;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserDto {
        private Long id;
        private String name;
        private String email;
        private String cpf;
        private String phone;
        private LocalDate birthDate;
        private User.UserType userType;
        private User.UserType activeRole;
        private Set<User.UserType> roles;
        private Boolean emailVerified;
        private String avatarUrl;
        private Boolean active;
        private LocalDateTime createdAt;
    }
}

