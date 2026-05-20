package com.servija.servija.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "freelancers")
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@PrimaryKeyJoinColumn(name = "user_id")
public class Freelancer extends User {

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
}

