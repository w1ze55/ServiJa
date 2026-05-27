package com.servija.servija.scheduler;

import com.servija.servija.service.AuditLogRetentionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditLogRetentionSchedulerTest {

    @Mock
    private AuditLogRetentionService auditLogRetentionService;

    @InjectMocks
    private AuditLogRetentionScheduler auditLogRetentionScheduler;

    @Test
    void runsRetentionCleanupOncePerExecution() {
        auditLogRetentionScheduler.purgeExpiredAuditLogs();

        verify(auditLogRetentionService).purgeExpiredLogs();
    }
}
