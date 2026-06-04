package com.servija.servija.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "freelancers")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(exclude = {"user", "serviceSubcategories", "serviceCities", "professionalLinks", "portfolioProjects"})
@ToString(exclude = {"user", "serviceSubcategories", "serviceCities", "professionalLinks", "portfolioProjects"})
public class Freelancer {

    @Id
    @Column(name = "user_id")
    private Long id;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column
    private String professionalTitle;

    @Column
    private String location;

    @Column(precision = 3, scale = 2)
    private BigDecimal rating;

    @Column
    @lombok.Builder.Default
    private Integer totalReviews = 0;

    @Column
    @lombok.Builder.Default
    private Integer completedJobs = 0;

    @Column
    @lombok.Builder.Default
    private Integer activeJobs = 0;

    @Column(nullable = false)
    @lombok.Builder.Default
    private Boolean profileCompleted = false;

    @Column(nullable = false)
    @lombok.Builder.Default
    private Boolean available = true;

    @Column(length = 100)
    private String category;

    @Column(length = 50)
    private String serviceType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ServiceCategory serviceCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subcategory_id")
    private ServiceSubcategory serviceSubcategory;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "freelancer_subcategories",
            joinColumns = @JoinColumn(name = "freelancer_id"),
            inverseJoinColumns = @JoinColumn(name = "subcategory_id"))
    @OrderBy("sortOrder ASC, name ASC")
    @lombok.Builder.Default
    private List<ServiceSubcategory> serviceSubcategories = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String cities;

    @Column(columnDefinition = "TEXT")
    private String socialLinks;

    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;

    @OneToMany(mappedBy = "freelancer", cascade = CascadeType.ALL, orphanRemoval = true)
    @lombok.Builder.Default
    private List<ServiceCity> serviceCities = new ArrayList<>();

    @OneToMany(mappedBy = "freelancer", cascade = CascadeType.ALL, orphanRemoval = true)
    @lombok.Builder.Default
    private List<ProfessionalLink> professionalLinks = new ArrayList<>();

    @OneToMany(mappedBy = "freelancer", cascade = CascadeType.ALL, orphanRemoval = true)
    @lombok.Builder.Default
    private List<PortfolioProject> portfolioProjects = new ArrayList<>();

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

    public String getAvatarUrl() {
        return user != null ? user.getAvatarUrl() : null;
    }

    public void setAvatarUrl(String avatarUrl) {
        ensureUser().setAvatarUrl(avatarUrl);
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
}

