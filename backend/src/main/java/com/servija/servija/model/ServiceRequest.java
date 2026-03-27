package com.servija.servija.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;
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

    @DecimalMin(value = "5.00", message = "O orçamento mínimo é R$ 5,00")
    @DecimalMax(value = "1000000.00", message = "O orçamento máximo é R$ 1.000.000,00")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal budget;

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

    // Freelancer response fields
    @DecimalMin(value = "5.00", message = "O valor mínimo é R$ 5,00")
    @DecimalMax(value = "1000000.00", message = "O valor máximo é R$ 1.000.000,00")
    @Column(name = "response_value", precision = 10, scale = 2)
    private BigDecimal responseValue;

    @Min(value = 1, message = "O prazo mínimo é de 1 dia")
    @Max(value = 365, message = "O prazo máximo é de 365 dias")
    @Column(name = "response_days")
    private Integer responseDays;

    @Column(name = "response_scope", columnDefinition = "TEXT")
    private String responseScope;

    @Column(name = "response_notes", columnDefinition = "TEXT")
    private String responseNotes;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    // Counter-proposal fields
    @Column(name = "counter_value", precision = 10, scale = 2)
    private BigDecimal counterValue;

    @Column(name = "counter_days")
    private Integer counterDays;

    @Column(name = "counter_notes", columnDefinition = "TEXT")
    private String counterNotes;

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
