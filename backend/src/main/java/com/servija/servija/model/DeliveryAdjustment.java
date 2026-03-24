package com.servija.servija.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Locale;

@Entity
@Table(name = "delivery_adjustments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeliveryAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_request_id", nullable = false)
    private ServiceRequest serviceRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requested_by_user_id", nullable = false)
    private User requestedBy;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private Status status = Status.PENDING;

    @Column(name = "freelancer_response", columnDefinition = "TEXT")
    private String freelancerResponse;

    @Column(name = "responded_at")
    private LocalDateTime respondedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Getter
    @RequiredArgsConstructor
    public enum Status {
        PENDING("Pendente"),
        ACKNOWLEDGED("Recebido"),
        IN_PROGRESS("Em andamento"),
        COMPLETED("Ajustes finalizados"),
        REJECTED("Recusado");

        private final String label;

        public static String getLabel(String status) {
            if (status == null || status.isBlank()) {
                return status;
            }

            try {
                return valueOf(status.trim().toUpperCase(Locale.ROOT)).getLabel();
            } catch (IllegalArgumentException ex) {
                return status;
            }
        }
    }
}
