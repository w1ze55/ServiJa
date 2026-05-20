package com.servija.servija.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.servija.servija.model.AuditLog;
import com.servija.servija.model.User;
import com.servija.servija.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Façade for writing audit log entries. Captures actor (current user) automatically
 * from SecurityContextHolder and IP/UserAgent/HTTP info from the current request.
 *
 * The dedicated AuditInterceptor records most user mutations automatically; this
 * helper exists for flows that bypass it (login/register before security context
 * is set, or where extra semantic data is needed).
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLogger {

    private final AuditLogService auditLogService;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public void record(
            String action,
            String entityType,
            String entityId,
            String entityLabel,
            AuditLog.Result result,
            Object payload) {
        persist(currentActor(), null, action, entityType, entityId, entityLabel, result, payload);
    }

    public void recordFor(
            User actor,
            String action,
            String entityType,
            String entityId,
            String entityLabel,
            AuditLog.Result result,
            Object payload) {
        persist(actor, null, action, entityType, entityId, entityLabel, result, payload);
    }

    public void recordGuest(
            String attemptedEmail,
            String action,
            String entityType,
            String entityId,
            String entityLabel,
            AuditLog.Result result,
            Object payload) {
        persist(null, attemptedEmail, action, entityType, entityId, entityLabel, result, payload);
    }

    public void recordRaw(AuditLog log) {
        try {
            auditLogService.save(log);
        } catch (Exception e) {
            log("save", e);
        }
    }

    private void persist(
            User actor,
            String fallbackEmail,
            String action,
            String entityType,
            String entityId,
            String entityLabel,
            AuditLog.Result result,
            Object payload) {
        try {
            HttpServletRequest request = currentRequest();
            String ip = request != null ? extractIp(request) : null;
            String ua = request != null ? truncate(request.getHeader("User-Agent"), 512) : null;
            String httpMethod = request != null ? request.getMethod() : null;
            String httpPath = request != null ? truncate(request.getRequestURI(), 255) : null;
            String payloadJson = serializePayload(payload);

            AuditLog logEntry = AuditLog.builder()
                    .actorId(actor != null ? actor.getId() : null)
                    .actorName(actor != null ? actor.getName() : null)
                    .actorEmail(actor != null ? actor.getEmail() : fallbackEmail)
                    .actorType(actor != null ? mapActorType(actor) : AuditLog.ActorType.GUEST)
                    .action(action)
                    .entityType(entityType)
                    .entityId(truncate(entityId, 128))
                    .entityLabel(truncate(entityLabel, 255))
                    .httpMethod(httpMethod)
                    .httpPath(httpPath)
                    .result(result)
                    .ipAddress(truncate(ip, 64))
                    .userAgent(ua)
                    .payload(payloadJson)
                    .build();

            auditLogService.save(logEntry);
        } catch (Exception e) {
            log("write", e);
        }
    }

    public AuditLog.ActorType mapActorType(User user) {
        if (user == null || user.getUserType() == null) return AuditLog.ActorType.GUEST;
        return switch (user.getUserType()) {
            case CONTRACTOR -> AuditLog.ActorType.CONTRACTOR;
            case FREELANCER -> AuditLog.ActorType.FREELANCER;
            case ADMIN -> AuditLog.ActorType.ADMIN;
        };
    }

    public User currentActor() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
                return null;
            }
            return userRepository.findByEmail(auth.getName()).orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    public HttpServletRequest currentRequest() {
        try {
            Object attrs = RequestContextHolder.getRequestAttributes();
            if (attrs instanceof ServletRequestAttributes sra) {
                return sra.getRequest();
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public String extractIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    public String serializePayload(Object payload) {
        if (payload == null) return null;
        if (payload instanceof String s) return s;
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            return null;
        }
    }

    public String truncate(String value, int max) {
        if (value == null) return null;
        return value.length() <= max ? value : value.substring(0, max);
    }

    private void log(String op, Exception e) {
        log.warn("Falha ao gravar audit log ({}): {}", op, e.getMessage());
    }
}
