package com.servija.servija.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "review_ai_summaries")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewSummary {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "freelancer_id", nullable = false, unique = true)
    private Freelancer freelancer;

    @Column(name = "summary_json", nullable = false, columnDefinition = "TEXT")
    private String summaryJson;

    @Column(name = "review_count", nullable = false)
    private Integer reviewCount;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;
}
