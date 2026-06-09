package com.servija.servija.model;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"roles", "contractorProfile", "freelancerProfile"})
@ToString(exclude = {"roles", "contractorProfile", "freelancerProfile"})
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

    @Enumerated(EnumType.STRING)
    @Column(name = "active_role", nullable = false, length = 20)
    private UserType activeRole;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_roles", joinColumns = @JoinColumn(name = "user_id"))
    @Column(name = "role", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Set<UserType> roles = new LinkedHashSet<>();

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

    // ---- 2FA (TOTP) ----
    @Column(name = "totp_secret_encrypted")
    private byte[] totpSecretEncrypted;

    @Column(name = "pending_totp_secret_encrypted")
    private byte[] pendingTotpSecretEncrypted;

    @Column(name = "pending_totp_created_at")
    private LocalDateTime pendingTotpCreatedAt;

    @Column(name = "totp_enabled", nullable = false)
    @lombok.Builder.Default
    private Boolean totpEnabled = false;

    @Column(name = "totp_enabled_at")
    private LocalDateTime totpEnabledAt;

    @Column(name = "totp_failed_attempts", nullable = false)
    @lombok.Builder.Default
    private Integer totpFailedAttempts = 0;

    @Column(name = "totp_locked_until")
    private LocalDateTime totpLockedUntil;

    // H6: versão de credenciais. Incrementada em troca/reset de senha para invalidar
    // todos os JWTs emitidos antes (revogação de tokens stateless).
    @Column(name = "token_version", nullable = false)
    @lombok.Builder.Default
    private Integer tokenVersion = 0;

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
    private Contractor contractorProfile;

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
    private Freelancer freelancerProfile;

    public enum UserType {
        CONTRACTOR,
        FREELANCER,
        ADMIN
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        Set<UserType> effectiveRoles = getRoles();

        if (effectiveRoles == null || effectiveRoles.isEmpty()) {
            return Collections.emptySet();
        }

        return effectiveRoles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name()))
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    public UserType getUserType() {
        return resolveEffectiveActiveRole();
    }

    public UserType getActiveRole() {
        return resolveEffectiveActiveRole();
    }

    public Set<UserType> getRoles() {
        Set<UserType> effectiveRoles = resolveEffectiveRoles();
        return effectiveRoles.isEmpty()
                ? Collections.emptySet()
                : Collections.unmodifiableSet(effectiveRoles);
    }

    public UserType getStoredUserType() {
        return userType;
    }

    public UserType getStoredActiveRole() {
        return activeRole;
    }

    public Set<UserType> getStoredRoles() {
        return roles;
    }

    public void setUserType(UserType userType) {
        this.userType = userType;
        this.activeRole = userType;
        addRole(userType);
    }

    public void setActiveRole(UserType activeRole) {
        this.activeRole = activeRole;
        this.userType = activeRole;
        addRole(activeRole);
    }

    public void addRole(UserType role) {
        if (role == null) {
            return;
        }

        if (roles == null) {
            roles = new LinkedHashSet<>();
        }

        roles.add(role);
    }

    public boolean hasRole(UserType role) {
        return role != null && getRoles().contains(role);
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

    public UserType resolveEffectiveActiveRole() {
        if (activeRole == UserType.ADMIN && userType != null && userType != UserType.ADMIN) {
            return userType;
        }

        return activeRole != null ? activeRole : userType;
    }

    public boolean hasLegacyInvalidActiveRole() {
        return activeRole == UserType.ADMIN && userType != null && userType != UserType.ADMIN;
    }

    public Set<UserType> resolveEffectiveRoles() {
        LinkedHashSet<UserType> effectiveRoles = new LinkedHashSet<>();

        if (roles != null && !roles.isEmpty()) {
            effectiveRoles.addAll(roles);
        }

        if (hasLegacyInvalidActiveRole()) {
            effectiveRoles.remove(UserType.ADMIN);
        }

        UserType effectiveActiveRole = resolveEffectiveActiveRole();
        if (effectiveActiveRole != null) {
            effectiveRoles.add(effectiveActiveRole);
        }

        if (userType != null && userType != UserType.ADMIN) {
            effectiveRoles.add(userType);
        }

        return effectiveRoles.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }
}
