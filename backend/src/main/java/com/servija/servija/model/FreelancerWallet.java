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
@Table(name = "freelancer_wallets")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FreelancerWallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "available_balance", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal availableBalance = BigDecimal.ZERO;

    @Column(name = "pending_balance", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal pendingBalance = BigDecimal.ZERO;

    @Column(name = "total_earned", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal totalEarned = BigDecimal.ZERO;

    @Column(name = "total_withdrawn", nullable = false, precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal totalWithdrawn = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public void addPendingBalance(BigDecimal amount) {
        this.pendingBalance = this.pendingBalance.add(amount);
    }

    public void movePendingToAvailable(BigDecimal amount) {
        this.pendingBalance = this.pendingBalance.subtract(amount);
        this.availableBalance = this.availableBalance.add(amount);
        this.totalEarned = this.totalEarned.add(amount);
    }

    public void deductAvailableBalance(BigDecimal amount) {
        this.availableBalance = this.availableBalance.subtract(amount);
        this.totalWithdrawn = this.totalWithdrawn.add(amount);
    }
}
