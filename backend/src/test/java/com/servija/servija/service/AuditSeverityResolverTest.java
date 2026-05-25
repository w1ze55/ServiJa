package com.servija.servija.service;

import com.servija.servija.dto.AuditSeverity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuditSeverityResolverTest {

    @Test
    void resolvesKnownWarningAction() {
        assertEquals(AuditSeverity.WARNING, AuditSeverityResolver.resolve("BANK_ACCOUNT_ADDED"));
    }

    @Test
    void resolvesKnownCriticalAction() {
        assertEquals(AuditSeverity.CRITICAL, AuditSeverityResolver.resolve("ACCOUNT_DELETED"));
    }

    @Test
    void fallsBackToInfoForUnknownAction() {
        assertEquals(AuditSeverity.INFO, AuditSeverityResolver.resolve("SOME_NEW_ACTION"));
    }
}
