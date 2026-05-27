import type { AuditLogEntry, AuditSeverity } from "@/api/services/auditLogService";

export const SEVERITY_LABELS: Record<AuditSeverity, string> = {
  INFO: "Info",
  WARNING: "Atenção",
  CRITICAL: "Crítico",
};

export const SEVERITY_BADGE_STYLES: Record<
  AuditSeverity,
  { badge: string; dot: string }
> = {
  INFO: {
    badge: "bg-blue-50 text-blue-700",
    dot: "bg-blue-600",
  },
  WARNING: {
    badge: "bg-amber-50 text-amber-700",
    dot: "bg-amber-500",
  },
  CRITICAL: {
    badge: "bg-red-50 text-red-700",
    dot: "bg-red-600",
  },
};

export const getLogSeverity = (
  log: Pick<AuditLogEntry, "severity">,
): AuditSeverity => log.severity ?? "INFO";
