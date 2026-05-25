package com.servija.servija.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {

    public enum ActorType {
        CONTRACTOR, FREELANCER, ADMIN, GUEST
    }

    public enum Result {
        SUCCESS, FAILURE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    @CreationTimestamp
    private LocalDateTime timestamp;

    @Column
    private Long actorId;

    @Column(length = 255)
    private String actorName;

    @Column(length = 255)
    private String actorEmail;

    @Enumerated(EnumType.STRING)
    @Column(length = 32)
    private ActorType actorType;

    @Column(nullable = false, length = 96)
    private String action;

    @Column(length = 64)
    private String entityType;

    @Column(length = 128)
    private String entityId;

    @Column(length = 255)
    private String entityLabel;

    @Column(length = 8)
    private String httpMethod;

    @Column(length = 255)
    private String httpPath;

    @Column
    private Integer httpStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private Result result;

    @Column(length = 64)
    private String ipAddress;

    @Column(length = 512)
    private String userAgent;

    @Column(columnDefinition = "TEXT")
    private String payload;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;
}
