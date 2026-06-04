package com.servija.servija.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "contractors")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = "user")
@ToString(exclude = "user")
public class Contractor {

    @Id
    @Column(name = "user_id")
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column
    private String preferredLocation;

    @Column
    @lombok.Builder.Default
    private Integer totalContracts = 0;

    @Column
    @lombok.Builder.Default
    private Integer activeContracts = 0;

    @Column
    @lombok.Builder.Default
    private Integer completedContracts = 0;

    private User ensureUser() {
        if (user == null) {
            user = new User();
            user.setId(id);
        }
        return user;
    }

    public String getName() {
        return user != null ? user.getName() : null;
    }

    public void setName(String name) {
        ensureUser().setName(name);
    }

    public String getEmail() {
        return user != null ? user.getEmail() : null;
    }

    public void setEmail(String email) {
        ensureUser().setEmail(email);
    }

    public String getCpf() {
        return user != null ? user.getCpf() : null;
    }

    public void setCpf(String cpf) {
        ensureUser().setCpf(cpf);
    }

    public String getPhone() {
        return user != null ? user.getPhone() : null;
    }

    public void setPhone(String phone) {
        ensureUser().setPhone(phone);
    }

    public java.time.LocalDate getBirthDate() {
        return user != null ? user.getBirthDate() : null;
    }

    public void setBirthDate(java.time.LocalDate birthDate) {
        ensureUser().setBirthDate(birthDate);
    }

    public String getPassword() {
        return user != null ? user.getPassword() : null;
    }

    public void setPassword(String password) {
        ensureUser().setPassword(password);
    }

    public User.UserType getUserType() {
        return user != null ? user.getUserType() : null;
    }

    public void setUserType(User.UserType userType) {
        ensureUser().setUserType(userType);
    }

    public Boolean getActive() {
        return user != null ? user.getActive() : null;
    }

    public void setActive(Boolean active) {
        ensureUser().setActive(active);
    }

    public Boolean getEmailVerified() {
        return user != null ? user.getEmailVerified() : null;
    }

    public void setEmailVerified(Boolean emailVerified) {
        ensureUser().setEmailVerified(emailVerified);
    }

    public String getAvatarUrl() {
        return user != null ? user.getAvatarUrl() : null;
    }

    public void setAvatarUrl(String avatarUrl) {
        ensureUser().setAvatarUrl(avatarUrl);
    }

    public String getVerificationCode() {
        return user != null ? user.getVerificationCode() : null;
    }

    public void setVerificationCode(String verificationCode) {
        ensureUser().setVerificationCode(verificationCode);
    }

    public java.time.LocalDateTime getVerificationCodeExpiry() {
        return user != null ? user.getVerificationCodeExpiry() : null;
    }

    public void setVerificationCodeExpiry(java.time.LocalDateTime verificationCodeExpiry) {
        ensureUser().setVerificationCodeExpiry(verificationCodeExpiry);
    }

    public Integer getVerificationAttempts() {
        return user != null ? user.getVerificationAttempts() : null;
    }

    public void setVerificationAttempts(Integer verificationAttempts) {
        ensureUser().setVerificationAttempts(verificationAttempts);
    }

    public java.time.LocalDateTime getVerificationLockUntil() {
        return user != null ? user.getVerificationLockUntil() : null;
    }

    public void setVerificationLockUntil(java.time.LocalDateTime verificationLockUntil) {
        ensureUser().setVerificationLockUntil(verificationLockUntil);
    }

    public String getResetPasswordToken() {
        return user != null ? user.getResetPasswordToken() : null;
    }

    public void setResetPasswordToken(String resetPasswordToken) {
        ensureUser().setResetPasswordToken(resetPasswordToken);
    }

    public java.time.LocalDateTime getResetPasswordTokenExpiry() {
        return user != null ? user.getResetPasswordTokenExpiry() : null;
    }

    public void setResetPasswordTokenExpiry(java.time.LocalDateTime resetPasswordTokenExpiry) {
        ensureUser().setResetPasswordTokenExpiry(resetPasswordTokenExpiry);
    }

    public Integer getResetPasswordAttempts() {
        return user != null ? user.getResetPasswordAttempts() : null;
    }

    public void setResetPasswordAttempts(Integer resetPasswordAttempts) {
        ensureUser().setResetPasswordAttempts(resetPasswordAttempts);
    }

    public java.time.LocalDateTime getResetPasswordLockUntil() {
        return user != null ? user.getResetPasswordLockUntil() : null;
    }

    public void setResetPasswordLockUntil(java.time.LocalDateTime resetPasswordLockUntil) {
        ensureUser().setResetPasswordLockUntil(resetPasswordLockUntil);
    }

    public String getAdminResetPasswordTokenHash() {
        return user != null ? user.getAdminResetPasswordTokenHash() : null;
    }

    public void setAdminResetPasswordTokenHash(String adminResetPasswordTokenHash) {
        ensureUser().setAdminResetPasswordTokenHash(adminResetPasswordTokenHash);
    }

    public java.time.LocalDateTime getAdminResetPasswordTokenExpiry() {
        return user != null ? user.getAdminResetPasswordTokenExpiry() : null;
    }

    public void setAdminResetPasswordTokenExpiry(java.time.LocalDateTime adminResetPasswordTokenExpiry) {
        ensureUser().setAdminResetPasswordTokenExpiry(adminResetPasswordTokenExpiry);
    }
}
