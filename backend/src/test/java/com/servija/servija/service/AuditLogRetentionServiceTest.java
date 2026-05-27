package com.servija.servija.service;

import com.servija.servija.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuditLogRetentionServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    private AuditLogRetentionService auditLogRetentionService;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-05-27T12:00:00Z"), ZoneOffset.UTC);
        auditLogRetentionService = new AuditLogRetentionService(
                auditLogRepository,
                Period.ofMonths(1),
                Period.ofMonths(6),
                Period.ofYears(1),
                fixedClock);
    }

    @Test
    @SuppressWarnings("unchecked")
    void removesLogsUsingSeveritySpecificCutoffs() {
        when(auditLogRepository.deleteInfoLogsOlderThan(any(), any())).thenReturn(4);
        when(auditLogRepository.deleteLogsOlderThanAndActionIn(any(), any())).thenReturn(3, 2);

        AuditLogRetentionService.RetentionSummary summary = auditLogRetentionService.purgeExpiredLogs();

        ArgumentCaptor<LocalDateTime> infoCutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<Collection<String>> nonInfoActionsCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(auditLogRepository).deleteInfoLogsOlderThan(infoCutoffCaptor.capture(), nonInfoActionsCaptor.capture());

        ArgumentCaptor<LocalDateTime> severityCutoffCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<Collection<String>> actionsCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(auditLogRepository, times(2))
                .deleteLogsOlderThanAndActionIn(severityCutoffCaptor.capture(), actionsCaptor.capture());

        assertEquals(LocalDateTime.of(2026, 4, 27, 12, 0), infoCutoffCaptor.getValue());
        assertEquals(
                List.of(
                        LocalDateTime.of(2025, 11, 27, 12, 0),
                        LocalDateTime.of(2025, 5, 27, 12, 0)),
                severityCutoffCaptor.getAllValues());
        assertEquals(AuditSeverityResolver.nonInfoActions(), Set.copyOf(nonInfoActionsCaptor.getValue()));
        assertEquals(AuditSeverityResolver.actionsFor(com.servija.servija.dto.AuditSeverity.WARNING), Set.copyOf(actionsCaptor.getAllValues().get(0)));
        assertEquals(AuditSeverityResolver.actionsFor(com.servija.servija.dto.AuditSeverity.CRITICAL), Set.copyOf(actionsCaptor.getAllValues().get(1)));
        assertEquals(4, summary.infoDeleted());
        assertEquals(3, summary.warningDeleted());
        assertEquals(2, summary.criticalDeleted());
        assertEquals(9, summary.totalDeleted());
    }

    @Test
    @SuppressWarnings("unchecked")
    void treatsUnknownActionsAsInfoByExcludingOnlyKnownNonInfoActions() {
        when(auditLogRepository.deleteInfoLogsOlderThan(any(), any())).thenReturn(0);
        when(auditLogRepository.deleteLogsOlderThanAndActionIn(any(), any())).thenReturn(0, 0);

        auditLogRetentionService.purgeExpiredLogs();

        ArgumentCaptor<Collection<String>> nonInfoActionsCaptor = ArgumentCaptor.forClass(Collection.class);
        verify(auditLogRepository).deleteInfoLogsOlderThan(any(), nonInfoActionsCaptor.capture());

        Set<String> nonInfoActions = Set.copyOf(nonInfoActionsCaptor.getValue());
        assertFalse(nonInfoActions.contains("SOME_NEW_ACTION"));
        assertTrue(nonInfoActions.contains("WITHDRAWAL_REQUESTED"));
        assertTrue(nonInfoActions.contains("ACCOUNT_DELETED"));
    }
}
