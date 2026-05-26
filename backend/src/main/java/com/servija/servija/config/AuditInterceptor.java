package com.servija.servija.config;

import com.servija.servija.model.AuditLog;
import com.servija.servija.model.User;
import com.servija.servija.service.AuditLogger;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Captures every successful mutation performed by non-admin authenticated users
 * and writes an audit log entry. Admin-area paths, public auth flows (which are
 * logged manually with success/failure detail), and webhooks are excluded.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditInterceptor implements HandlerInterceptor {

    private static final List<String> SKIP_PATH_PREFIXES = List.of(
            "/api/admin/",
            "/api/auth/",
            "/api/chat/",
            "/api/payments/webhook",
            "/api/webhooks/",
            "/swagger",
            "/v3/api-docs",
            "/actuator"
    );

    private static final List<String> MUTATING_METHODS = List.of("POST", "PUT", "PATCH", "DELETE");

    /**
     * Ordered URL-pattern → semantic mapping. First match wins. {id} captures
     * any path segment and is stored as the entity id.
     */
    private static final List<Mapping> MAPPINGS = List.of(
            new Mapping("PUT",    "/api/users/me/password",                                "PASSWORD_CHANGED",           "USER"),
            new Mapping("PUT",    "/api/users/me/avatar/upload",                           "AVATAR_UPDATED",             "USER"),
            new Mapping("PUT",    "/api/users/me/avatar",                                  "AVATAR_UPDATED",             "USER"),
            new Mapping("PUT",    "/api/users/me",                                         "PROFILE_UPDATED",            "USER"),
            new Mapping("DELETE", "/api/users/me",                                         "ACCOUNT_DELETED",            "USER"),

            new Mapping("POST",   "/api/addresses",                                        "ADDRESS_CREATED",            "ADDRESS"),
            new Mapping("PUT",    "/api/addresses/{id}",                                   "ADDRESS_UPDATED",            "ADDRESS"),
            new Mapping("DELETE", "/api/addresses/{id}",                                   "ADDRESS_DELETED",            "ADDRESS"),
            new Mapping("PUT",    "/api/addresses/{id}/default",                           "ADDRESS_DEFAULT_SET",        "ADDRESS"),

            new Mapping("POST",   "/api/services",                                         "SERVICE_CREATED",            "SERVICE"),
            new Mapping("PUT",    "/api/services/{id}",                                    "SERVICE_UPDATED",            "SERVICE"),
            new Mapping("DELETE", "/api/services/{id}",                                    "SERVICE_DELETED",            "SERVICE"),
            new Mapping("PATCH",  "/api/services/{id}/toggle",                             "SERVICE_TOGGLED",            "SERVICE"),

            new Mapping("POST",   "/api/service-requests",                                 "SERVICE_REQUEST_CREATED",    "SERVICE_REQUEST"),
            new Mapping("PATCH",  "/api/service-requests/{id}/status",                     "SERVICE_REQUEST_STATUS",     "SERVICE_REQUEST"),
            new Mapping("PATCH",  "/api/service-requests/{id}/respond",                    "SERVICE_REQUEST_RESPONDED",  "SERVICE_REQUEST"),
            new Mapping("PATCH",  "/api/service-requests/{id}/milestones/{milestoneId}",   "MILESTONE_UPDATED",          "SERVICE_REQUEST"),
            new Mapping("PATCH",  "/api/service-requests/{id}/counter-proposal",           "COUNTER_PROPOSAL_SENT",      "SERVICE_REQUEST"),
            new Mapping("PATCH",  "/api/service-requests/{id}/accept-counter",             "COUNTER_PROPOSAL_ACCEPTED",  "SERVICE_REQUEST"),
            new Mapping("PATCH",  "/api/service-requests/{id}/reject-counter",             "COUNTER_PROPOSAL_REJECTED",  "SERVICE_REQUEST"),
            new Mapping("POST",   "/api/service-requests/{id}/freelancer-attachments",     "FREELANCER_ATTACHMENTS",     "SERVICE_REQUEST"),
            new Mapping("POST",   "/api/service-requests/{id}/contractor-attachments",     "CONTRACTOR_ATTACHMENTS",     "SERVICE_REQUEST"),
            new Mapping("PATCH",  "/api/service-requests/{id}/complete",                   "SERVICE_REQUEST_COMPLETED",  "SERVICE_REQUEST"),
            new Mapping("DELETE", "/api/service-requests/{id}",                            "SERVICE_REQUEST_DELETED",    "SERVICE_REQUEST"),

            new Mapping("POST",   "/api/reviews/{id}",                                     "REVIEW_CREATED",             "REVIEW"),

            new Mapping("POST",   "/api/payments",                                         "PAYMENT_CREATED",            "PAYMENT"),
            new Mapping("POST",   "/api/payments/{id}/simulate-confirmation",              "PAYMENT_SIMULATED",          "PAYMENT"),

            new Mapping("POST",   "/api/wallet/withdraw",                                  "WITHDRAWAL_REQUESTED",       "WITHDRAWAL"),
            new Mapping("DELETE", "/api/wallet/withdrawals/{id}",                          "WITHDRAWAL_CANCELLED",       "WITHDRAWAL"),
            new Mapping("POST",   "/api/wallet/bank-accounts",                             "BANK_ACCOUNT_ADDED",         "BANK_ACCOUNT"),
            new Mapping("DELETE", "/api/wallet/bank-accounts/{id}",                        "BANK_ACCOUNT_DELETED",       "BANK_ACCOUNT"),
            new Mapping("PUT",    "/api/wallet/bank-accounts/{id}/default",                "BANK_ACCOUNT_DEFAULT",       "BANK_ACCOUNT"),

            new Mapping("POST",   "/api/freelancer/portfolio",                             "PORTFOLIO_CREATED",          "PORTFOLIO"),
            new Mapping("PUT",    "/api/freelancer/portfolio/{id}",                        "PORTFOLIO_UPDATED",          "PORTFOLIO"),
            new Mapping("DELETE", "/api/freelancer/portfolio/{id}",                        "PORTFOLIO_DELETED",          "PORTFOLIO"),

            new Mapping("PUT",    "/api/freelancer/profile",                               "FREELANCER_PROFILE_UPDATED", "USER"),
            new Mapping("PUT",    "/api/freelancer/location",                              "FREELANCER_LOCATION_UPDATED","USER"),

            new Mapping("POST",   "/api/favorites",                                        "FAVORITE_ADDED",             "FAVORITE"),
            new Mapping("DELETE", "/api/favorites/{id}",                                   "FAVORITE_REMOVED",           "FAVORITE"),

            new Mapping("POST",   "/api/support-requests",                                 "SUPPORT_REQUEST_CREATED",    "SUPPORT_TICKET"),

            new Mapping("POST",   "/api/delivery-adjustments/service-request/{id}",        "ADJUSTMENT_REQUESTED",       "ADJUSTMENT"),
            new Mapping("PATCH",  "/api/delivery-adjustments/{id}/status",                 "ADJUSTMENT_STATUS_CHANGED",  "ADJUSTMENT"),
            new Mapping("DELETE", "/api/delivery-adjustments/{id}",                        "ADJUSTMENT_DELETED",         "ADJUSTMENT")
    );

    private final AuditLogger auditLogger;

    @Override
    public void afterCompletion(HttpServletRequest request,
                                HttpServletResponse response,
                                Object handler,
                                Exception ex) {
        try {
            if (!shouldLog(request)) return;
            if (auditLogger.isRequestAlreadyAudited(request)) return;

            User actor = auditLogger.currentActor();
            // Skip admins (they get tracked separately if at all)
            if (actor != null && actor.getUserType() == User.UserType.ADMIN) return;

            int status = response.getStatus();
            AuditLog.Result result = (status >= 200 && status < 400) ? AuditLog.Result.SUCCESS : AuditLog.Result.FAILURE;

            Mapping mapping = matchMapping(request.getMethod(), request.getRequestURI());

            String action = mapping != null ? mapping.action() : (request.getMethod() + "_" + bestEffortResource(request.getRequestURI()));
            String entityType = mapping != null ? mapping.entityType() : null;
            String entityId = mapping != null ? mapping.extractEntityId(request.getRequestURI()) : null;

            String ip = auditLogger.extractIp(request);
            String ua = auditLogger.truncate(request.getHeader("User-Agent"), 512);

            AuditLog logEntry = AuditLog.builder()
                    .actorId(actor != null ? actor.getId() : null)
                    .actorName(actor != null ? actor.getName() : null)
                    .actorEmail(actor != null ? actor.getEmail() : null)
                    .actorType(actor != null ? auditLogger.mapActorType(actor) : AuditLog.ActorType.GUEST)
                    .action(auditLogger.truncate(action, 96))
                    .entityType(auditLogger.truncate(entityType, 64))
                    .entityId(auditLogger.truncate(entityId, 128))
                    .httpMethod(request.getMethod())
                    .httpPath(auditLogger.truncate(request.getRequestURI(), 255))
                    .httpStatus(status)
                    .result(result)
                    .ipAddress(auditLogger.truncate(ip, 64))
                    .userAgent(ua)
                    .payload(ex != null ? auditLogger.serializePayload(Map.of("error", ex.getClass().getSimpleName(), "message", ex.getMessage() == null ? "" : ex.getMessage())) : null)
                    .build();

            auditLogger.recordRaw(logEntry);
        } catch (Exception e) {
            log.warn("AuditInterceptor falhou: {}", e.getMessage());
        }
    }

    private boolean shouldLog(HttpServletRequest request) {
        String method = request.getMethod();
        if (!MUTATING_METHODS.contains(method)) return false;

        String uri = request.getRequestURI();
        if (uri == null) return false;
        for (String prefix : SKIP_PATH_PREFIXES) {
            if (uri.startsWith(prefix)) return false;
        }
        return uri.startsWith("/api/");
    }

    private Mapping matchMapping(String method, String uri) {
        for (Mapping m : MAPPINGS) {
            if (m.matches(method, uri)) return m;
        }
        return null;
    }

    private String bestEffortResource(String uri) {
        if (uri == null) return "REQUEST";
        String[] parts = uri.replaceFirst("^/api/", "").split("/");
        for (String part : parts) {
            if (!part.isBlank() && !part.matches("\\d+")) {
                return part.toUpperCase().replace('-', '_');
            }
        }
        return "REQUEST";
    }

    /** URL pattern with {placeholders} for path variables. */
    private record Mapping(String method, String pattern, String action, String entityType) {
        private static final Pattern PLACEHOLDER = Pattern.compile("\\{[^/]+?\\}");

        Pattern regex() {
            String escaped = pattern.replace("/", "\\/");
            String regex = PLACEHOLDER.matcher(escaped).replaceAll("([^/]+)");
            return Pattern.compile("^" + regex + "/?$");
        }

        boolean matches(String httpMethod, String uri) {
            if (!method.equals(httpMethod)) return false;
            return regex().matcher(uri).matches();
        }

        String extractEntityId(String uri) {
            Matcher m = regex().matcher(uri);
            if (m.matches() && m.groupCount() >= 1) {
                return m.group(1);
            }
            return null;
        }
    }
}
