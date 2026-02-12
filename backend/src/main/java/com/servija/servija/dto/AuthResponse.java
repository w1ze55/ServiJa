package com.servija.servija.dto;

import com.servija.servija.model.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;
    
    @Builder.Default
    private String tokenType = "Bearer";
    
    private UserDto user;

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
        private User.UserType userType;
        private Boolean emailVerified;
        private String avatarUrl;
        private Boolean active;
        private LocalDateTime createdAt;
    }
}

