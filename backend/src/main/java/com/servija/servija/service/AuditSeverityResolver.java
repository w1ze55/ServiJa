package com.servija.servija.service;

import com.servija.servija.dto.AuditSeverity;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public final class AuditSeverityResolver {

    private static final Map<String, AuditSeverity> ACTION_SEVERITY = Map.ofEntries(
            Map.entry("LOGIN", AuditSeverity.INFO),
            Map.entry("LOGOUT", AuditSeverity.INFO),
            Map.entry("USER_REGISTERED", AuditSeverity.INFO),
            Map.entry("CADASTRO", AuditSeverity.INFO),
            Map.entry("EMAIL_VERIFIED", AuditSeverity.INFO),
            Map.entry("VERIFICACAO_EMAIL", AuditSeverity.INFO),
            Map.entry("CONSULTA", AuditSeverity.INFO),
            Map.entry("DOWNLOAD_RELATORIO", AuditSeverity.INFO),
            Map.entry("DOWNLOAD_REPORT", AuditSeverity.INFO),

            Map.entry("BANK_ACCOUNT_ADDED", AuditSeverity.WARNING),
            Map.entry("CONTA_BANCARIA_CRIADA", AuditSeverity.WARNING),
            Map.entry("BANK_ACCOUNT_DELETED", AuditSeverity.WARNING),
            Map.entry("BANK_ACCOUNT_DEFAULT", AuditSeverity.WARNING),
            Map.entry("CONTA_BANCARIA_EXCLUIDA", AuditSeverity.WARNING),
            Map.entry("ADDRESS_CREATED", AuditSeverity.WARNING),
            Map.entry("ADDRESS_UPDATED", AuditSeverity.WARNING),
            Map.entry("ADDRESS_DELETED", AuditSeverity.WARNING),
            Map.entry("ADDRESS_DEFAULT_SET", AuditSeverity.WARNING),
            Map.entry("WITHDRAWAL_REQUESTED", AuditSeverity.WARNING),
            Map.entry("SAQUE_SOLICITADO", AuditSeverity.WARNING),
            Map.entry("SERVICE_TOGGLED", AuditSeverity.WARNING),
            Map.entry("SERVICE_ACTIVATED", AuditSeverity.WARNING),
            Map.entry("SERVICE_DEACTIVATED", AuditSeverity.WARNING),
            Map.entry("SERVICO_DESATIVADO", AuditSeverity.WARNING),
            Map.entry("PROFILE_UPDATED", AuditSeverity.WARNING),
            Map.entry("FREELANCER_PROFILE_UPDATED", AuditSeverity.WARNING),
            Map.entry("FREELANCER_LOCATION_UPDATED", AuditSeverity.WARNING),
            Map.entry("ADMIN_USER_UPDATED", AuditSeverity.WARNING),
            Map.entry("PERFIL_EDITADO", AuditSeverity.WARNING),
            Map.entry("PASSWORD_CHANGED", AuditSeverity.WARNING),
            Map.entry("AVATAR_UPDATED", AuditSeverity.WARNING),
            Map.entry("PORTFOLIO_UPDATED", AuditSeverity.WARNING),
            Map.entry("CATEGORY_UPDATED", AuditSeverity.WARNING),
            Map.entry("SUBCATEGORY_UPDATED", AuditSeverity.WARNING),
            Map.entry("SENHA_ALTERADA", AuditSeverity.WARNING),
            Map.entry("WITHDRAWAL_CANCELLED", AuditSeverity.WARNING),
            Map.entry("SERVICE_REQUEST_STATUS", AuditSeverity.WARNING),
            Map.entry("SERVICE_REQUEST_DELETED", AuditSeverity.WARNING),
            Map.entry("SOLICITACAO_CANCELADA", AuditSeverity.WARNING),
            Map.entry("CATEGORY_ARCHIVED", AuditSeverity.WARNING),
            Map.entry("CATEGORY_RESTORED", AuditSeverity.WARNING),
            Map.entry("SUBCATEGORY_ARCHIVED", AuditSeverity.WARNING),
            Map.entry("SUBCATEGORY_RESTORED", AuditSeverity.WARNING),

            Map.entry("USER_SUSPENDED", AuditSeverity.CRITICAL),
            Map.entry("USER_REACTIVATED", AuditSeverity.CRITICAL),
            Map.entry("USUARIO_SUSPENSO", AuditSeverity.CRITICAL),
            Map.entry("USER_BANNED", AuditSeverity.CRITICAL),
            Map.entry("USUARIO_BANIDO", AuditSeverity.CRITICAL),
            Map.entry("USER_DELETED", AuditSeverity.CRITICAL),
            Map.entry("USUARIO_EXCLUIDO", AuditSeverity.CRITICAL),
            Map.entry("REVIEW_REMOVED", AuditSeverity.CRITICAL),
            Map.entry("AVALIACAO_REMOVIDA", AuditSeverity.CRITICAL),
            Map.entry("ACCOUNT_DELETED", AuditSeverity.CRITICAL),
            Map.entry("CONTA_EXCLUIDA", AuditSeverity.CRITICAL),
            Map.entry("ADMIN_FORCED_ACTION", AuditSeverity.CRITICAL),
            Map.entry("ADMIN_ACAO_FORCADA", AuditSeverity.CRITICAL)
    );

    private static final Set<String> WARNING_ACTIONS = actionsForLevel(AuditSeverity.WARNING);
    private static final Set<String> CRITICAL_ACTIONS = actionsForLevel(AuditSeverity.CRITICAL);
    private static final Set<String> NON_INFO_ACTIONS = ACTION_SEVERITY.entrySet().stream()
            .filter(entry -> entry.getValue() != AuditSeverity.INFO)
            .map(Map.Entry::getKey)
            .collect(Collectors.toUnmodifiableSet());

    private AuditSeverityResolver() {
    }

    public static AuditSeverity resolve(String action) {
        if (action == null || action.isBlank()) {
            return AuditSeverity.INFO;
        }
        return ACTION_SEVERITY.getOrDefault(action, AuditSeverity.INFO);
    }

    public static Set<String> actionsFor(AuditSeverity severity) {
        return switch (severity) {
            case INFO -> Set.of();
            case WARNING -> WARNING_ACTIONS;
            case CRITICAL -> CRITICAL_ACTIONS;
        };
    }

    public static Set<String> nonInfoActions() {
        return NON_INFO_ACTIONS;
    }

    private static Set<String> actionsForLevel(AuditSeverity severity) {
        return ACTION_SEVERITY.entrySet().stream()
                .filter(entry -> entry.getValue() == severity)
                .map(Map.Entry::getKey)
                .collect(Collectors.toUnmodifiableSet());
    }
}
