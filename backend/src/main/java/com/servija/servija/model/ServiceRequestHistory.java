package com.servija.servija.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "service_request_history")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceRequestHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_request_id", nullable = false)
    private ServiceRequest serviceRequest;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type", nullable = false, length = 30)
    private OperationType operationType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "details", columnDefinition = "TEXT")
    private String details;

    public enum OperationType {
        REQUEST_OPENED("Solicitação aberta"),
        REQUEST_REJECTED("Solicitação recusada"),
        BUDGET_SENT("Orçamento enviado"),
        COUNTER_PROPOSAL_SENT("Contra proposta enviada"),
        COUNTER_PROPOSAL_ACCEPTED("Contra proposta aceita"),
        COUNTER_PROPOSAL_REJECTED("Contra proposta rejeitada"),
        BUDGET_ACCEPTED("Orçamento aceito"),
        CANCELLED("Solicitação cancelada");

        private final String label;

        OperationType(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }
}
