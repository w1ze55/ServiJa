package com.servija.servija.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "service_search_weekly_metrics",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_service_search_weekly_metrics_service_week",
                        columnNames = {"service_id", "week_start_date"})
        },
        indexes = {
                @Index(name = "idx_service_search_weekly_metrics_week_count", columnList = "week_start_date, search_count"),
                @Index(name = "idx_service_search_weekly_metrics_service", columnList = "service_id")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceSearchWeeklyMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Service service;

    @Column(name = "week_start_date", nullable = false)
    private LocalDate weekStartDate;

    @Column(name = "search_count", nullable = false)
    @Builder.Default
    private Long searchCount = 0L;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
