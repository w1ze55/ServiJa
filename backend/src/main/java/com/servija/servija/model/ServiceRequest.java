package com.servija.servija.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "service_requests")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contractor_id", nullable = false)
    private User contractor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "freelancer_id")
    private User freelancer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id")
    private Service service;

    @Column(name = "service_name", nullable = false)
    private String serviceName;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 50)
    private String deadline;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_model", nullable = false, length = 20)
    @Builder.Default
    private WorkModel workModel = WorkModel.REMOTO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.PENDING;

    @Column(name = "chat_enabled")
    @Builder.Default
    private Boolean chatEnabled = false;

    @Column(length = 500)
    private String address;

    @Column(name = "desired_date")
    private LocalDate desiredDate;

    @Column(columnDefinition = "TEXT")
    private String attachments;

    @Column(name = "freelancer_attachments", columnDefinition = "TEXT")
    private String freelancerAttachments;

    @Column(name = "response_attachments", columnDefinition = "TEXT")
    private String responseAttachments;

    @DecimalMin(value = "5.00", message = "O valor minimo e R$ 5,00")
    @DecimalMax(value = "1000000.00", message = "O valor maximo e R$ 1.000.000,00")
    @Column(name = "response_value", precision = 10, scale = 2)
    private BigDecimal responseValue;

    @Min(value = 1, message = "O prazo minimo e de 1 dia")
    @Max(value = 365, message = "O prazo maximo e de 365 dias")
    @Column(name = "response_days")
    private Integer responseDays;

    @Column(name = "response_scope", columnDefinition = "TEXT")
    private String responseScope;

    @Column(name = "response_notes", columnDefinition = "TEXT")
    private String responseNotes;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @Column(name = "counter_value", precision = 10, scale = 2)
    private BigDecimal counterValue;

    @Column(name = "counter_days")
    private Integer counterDays;

    @Column(name = "counter_notes", columnDefinition = "TEXT")
    private String counterNotes;

    @Column(name = "legacy_low_value_allowed", nullable = false)
    @Builder.Default
    private Boolean legacyLowValueAllowed = false;

    @Column(name = "cancel_reason", columnDefinition = "TEXT")
    private String cancelReason;

    @Column(name = "reject_reason", columnDefinition = "TEXT")
    private String rejectReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum WorkModel {
        PRESENCIAL, REMOTO, HIBRIDO
    }

    public enum Status {
        PENDING, SENT, NEGOTIATING, ACCEPTED, REJECTED, IN_PROGRESS, DELIVERED, COMPLETED, CANCELLED
    }
}
