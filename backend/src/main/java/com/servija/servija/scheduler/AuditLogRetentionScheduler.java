package com.servija.servija.scheduler;

import com.servija.servija.service.AuditLogRetentionService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuditLogRetentionScheduler {

    private final AuditLogRetentionService auditLogRetentionService;

    @Scheduled(cron = "${audit.retention.cron:0 0 3 * * *}")
    public void purgeExpiredAuditLogs() {
        auditLogRetentionService.purgeExpiredLogs();
    }
}
