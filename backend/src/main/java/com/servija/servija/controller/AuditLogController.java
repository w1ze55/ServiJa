package com.servija.servija.controller;

import com.servija.servija.dto.AuditLogPageResponse;
import com.servija.servija.dto.AuditSeverity;
import com.servija.servija.model.AuditLog;
import com.servija.servija.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/admin/audit-logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping
    public ResponseEntity<AuditLogPageResponse> listLogs(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) AuditLog.ActorType actorType,
            @RequestParam(required = false) AuditLog.Result result,
            @RequestParam(required = false) AuditSeverity severity,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        return ResponseEntity.ok(
                auditLogService.listLogs(search, action, entityType, actorType, result, severity, dateFrom, dateTo, page, size));
    }
}
