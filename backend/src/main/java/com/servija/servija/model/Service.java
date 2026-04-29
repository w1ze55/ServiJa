package com.servija.servija.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "services")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Service {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "freelancer_id", nullable = false)
    private Freelancer freelancer;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, length = 100)
    private String category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ServiceCategory serviceCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subcategory_id")
    private ServiceSubcategory serviceSubcategory;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "price_type", nullable = false)
    @Builder.Default
    private PriceType priceType = PriceType.FIXED;

    @Column(name = "delivery_time", nullable = false)
    private Integer deliveryTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_unit", nullable = false)
    @Builder.Default
    private DeliveryUnit deliveryUnit = DeliveryUnit.DAYS;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_model", nullable = false, length = 20)
    @Builder.Default
    private WorkModel workModel = WorkModel.REMOTO;

    @Column(columnDefinition = "TEXT")
    private String tags;

    @Column(columnDefinition = "TEXT")
    private String images;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum PriceType {
        FIXED, HOURLY, DAILY
    }

    public enum DeliveryUnit {
        DAYS, WEEKS, MONTHS
    }

    public enum WorkModel {
        PRESENCIAL, REMOTO, HIBRIDO
    }
}
