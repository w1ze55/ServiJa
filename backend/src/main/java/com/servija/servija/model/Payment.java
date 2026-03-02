package com.servija.servija.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_request_id", nullable = false)
    private ServiceRequest serviceRequest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contractor_id", nullable = false)
    private User contractor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "freelancer_id", nullable = false)
    private User freelancer;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "platform_fee", nullable = false, precision = 10, scale = 2)
    private BigDecimal platformFee;

    @Column(name = "gateway_fee", precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal gatewayFee = BigDecimal.ZERO;

    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "payment_method", nullable = false, length = 50)
    private String paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(name = "transaction_id", length = 255)
    private String transactionId;

    @Column(name = "asaas_payment_id", length = 255)
    private String asaasPaymentId;

    @Column(name = "asaas_customer_id", length = 255)
    private String asaasCustomerId;

    @Column(name = "pix_qr_code", columnDefinition = "TEXT")
    private String pixQrCode;

    @Column(name = "pix_copy_paste", columnDefinition = "TEXT")
    private String pixCopyPaste;

    @Column(name = "card_last_digits", length = 4)
    private String cardLastDigits;

    @Column(name = "refund_reason", columnDefinition = "TEXT")
    private String refundReason;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum PaymentStatus {
        PENDING,        // Aguardando pagamento
        PROCESSING,     // Processando
        COMPLETED,      // Pagamento confirmado
        FAILED,         // Falhou (captura recusada, reprovado por risco, etc)
        REFUNDED,       // Estornado
        CANCELLED,      // Cancelado/Deletado
        OVERDUE,        // Vencido
        CHARGEBACK      // Chargeback solicitado
    }
}
