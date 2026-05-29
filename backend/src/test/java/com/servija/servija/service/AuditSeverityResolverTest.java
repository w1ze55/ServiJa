package com.servija.servija.service;

import com.servija.servija.dto.AuditSeverity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AuditSeverityResolverTest {

    @Test
    void resolvesKnownInfoAction() {
        assertEquals(AuditSeverity.INFO, AuditSeverityResolver.resolve("BANK_ACCOUNT_ADDED"));
    }

    @Test
    void resolvesNewSemanticInfoAction() {
        assertEquals(AuditSeverity.INFO, AuditSeverityResolver.resolve("SERVICE_DEACTIVATED"));
    }

    @Test
    void resolvesKnownWarningAction() {
        assertEquals(AuditSeverity.WARNING, AuditSeverityResolver.resolve("WITHDRAWAL_REQUESTED"));
    }

    @Test
    void resolvesKnownCriticalAction() {
        assertEquals(AuditSeverity.CRITICAL, AuditSeverityResolver.resolve("ACCOUNT_DELETED"));
    }

    @Test
    void resolvesNewSemanticCriticalAction() {
        assertEquals(AuditSeverity.CRITICAL, AuditSeverityResolver.resolve("USER_SUSPENDED"));
    }

    @Test
    void fallsBackToInfoForUnknownAction() {
        assertEquals(AuditSeverity.INFO, AuditSeverityResolver.resolve("SOME_NEW_ACTION"));
    }
}
