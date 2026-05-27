package com.servija.servija.service;

import com.servija.servija.dto.AuditSeverity;
import com.servija.servija.repository.AuditLogRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Set;

@Service
@Slf4j
public class AuditLogRetentionService {

    private final AuditLogRepository auditLogRepository;
    private final Period infoRetention;
    private final Period warningRetention;
    private final Period criticalRetention;
    private final Clock clock;

    public AuditLogRetentionService(
            AuditLogRepository auditLogRepository,
            @Value("${audit.retention.info:P1M}") String infoRetention,
            @Value("${audit.retention.warning:P6M}") String warningRetention,
            @Value("${audit.retention.critical:P1Y}") String criticalRetention) {
        this(
                auditLogRepository,
                parsePeriod("audit.retention.info", infoRetention),
                parsePeriod("audit.retention.warning", warningRetention),
                parsePeriod("audit.retention.critical", criticalRetention),
                Clock.systemDefaultZone());
    }

    AuditLogRetentionService(
            AuditLogRepository auditLogRepository,
            Period infoRetention,
            Period warningRetention,
            Period criticalRetention,
            Clock clock) {
        this.auditLogRepository = auditLogRepository;
        this.infoRetention = infoRetention;
        this.warningRetention = warningRetention;
        this.criticalRetention = criticalRetention;
        this.clock = clock;
    }

    @Transactional
    public RetentionSummary purgeExpiredLogs() {
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime infoCutoff = now.minus(infoRetention);
        LocalDateTime warningCutoff = now.minus(warningRetention);
        LocalDateTime criticalCutoff = now.minus(criticalRetention);

        Set<String> nonInfoActions = AuditSeverityResolver.nonInfoActions();
        Set<String> warningActions = AuditSeverityResolver.actionsFor(AuditSeverity.WARNING);
        Set<String> criticalActions = AuditSeverityResolver.actionsFor(AuditSeverity.CRITICAL);

        int infoDeleted = auditLogRepository.deleteInfoLogsOlderThan(infoCutoff, nonInfoActions);
        int warningDeleted = auditLogRepository.deleteLogsOlderThanAndActionIn(warningCutoff, warningActions);
        int criticalDeleted = auditLogRepository.deleteLogsOlderThanAndActionIn(criticalCutoff, criticalActions);

        RetentionSummary summary = new RetentionSummary(infoDeleted, warningDeleted, criticalDeleted);
        log.info(
                "Audit log retention concluida: infoRemovidos={}, warningRemovidos={}, criticalRemovidos={}, totalRemovidos={}",
                summary.infoDeleted(),
                summary.warningDeleted(),
                summary.criticalDeleted(),
                summary.totalDeleted());
        return summary;
    }

    private static Period parsePeriod(String propertyName, String rawValue) {
        try {
            return Period.parse(rawValue);
        } catch (Exception e) {
            throw new IllegalArgumentException("Valor invalido para " + propertyName + ": " + rawValue, e);
        }
    }

    public record RetentionSummary(int infoDeleted, int warningDeleted, int criticalDeleted) {
        public int totalDeleted() {
            return infoDeleted + warningDeleted + criticalDeleted;
        }
    }
}
