package com.servija.servija.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "professional_links")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProfessionalLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "freelancer_id", nullable = false)
    private Freelancer freelancer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LinkType type;

    @Column(nullable = false, length = 500)
    private String url;

    @Column(length = 100)
    private String label;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public enum LinkType {
        WEBSITE,
        LINKEDIN,
        INSTAGRAM,
        GITHUB,
        PORTFOLIO,
        OTHER
    }
}
