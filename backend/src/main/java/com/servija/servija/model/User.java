package com.servija.servija.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;

@Entity
@Table(name = "users")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Inheritance(strategy = InheritanceType.JOINED)
public class User implements UserDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @Column(nullable = false, unique = true, length = 256)
    private String email;

    @Column(nullable = false, unique = true, length = 14)
    private String cpf;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column
    private LocalDate birthDate;

    @Column(nullable = false, length = 256)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserType userType;

    @Column(nullable = false)
    @lombok.Builder.Default
    private Boolean emailVerified = false;

    @Column(length = 6)
    private String verificationCode;

    @Column
    private LocalDateTime verificationCodeExpiry;

    @Column(nullable = false)
    @lombok.Builder.Default
    private Integer verificationAttempts = 0;

    @Column
    private LocalDateTime verificationLockUntil;

    @Column(length = 100)
    private String resetPasswordToken;

    @Column
    private LocalDateTime resetPasswordTokenExpiry;

    @Column(length = 64)
    private String adminResetPasswordTokenHash;

    @Column
    private LocalDateTime adminResetPasswordTokenExpiry;

    @Column(nullable = false)
    @lombok.Builder.Default
    private Integer resetPasswordAttempts = 0;

    @Column
    private LocalDateTime resetPasswordLockUntil;

    @Lob
    @Column(columnDefinition = "MEDIUMTEXT")
    private String avatarUrl; // Armazena base64 da imagem

    @Column(nullable = false)
    @lombok.Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime deletedAt;

    public enum UserType {
        CONTRACTOR,
        FREELANCER,
        ADMIN
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(
                new SimpleGrantedAuthority("ROLE_" + userType.name()));
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return active;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
