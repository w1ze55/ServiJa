package com.servija.servija.dto;

import com.servija.servija.model.AuditLog;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AuditLogDto {
    private Long id;
    private LocalDateTime timestamp;
    private Long actorId;
    private String actorName;
    private String actorEmail;
    private AuditLog.ActorType actorType;
    private String action;
    private String entityType;
    private String entityId;
    private String entityLabel;
    private String httpMethod;
    private String httpPath;
    private Integer httpStatus;
    private AuditLog.Result result;
    private AuditSeverity severity;
    private String ipAddress;
    private String userAgent;
    private String payload;
    private String response;
    private String oldValue;
    private String newValue;
}
