package com.servija.servija.service;

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


@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLogger {

    public static final String REQUEST_AUDITED_ATTRIBUTE = AuditLogger.class.getName() + ".AUDITED";

    private final AuditLogService auditLogService;
    private final UserRepository userRepository;
    private final AuditLogContentService auditLogContentService;

    public void record(
            String action,
            String entityType,
            String entityId,
            String entityLabel,
            AuditLog.Result result,
            Object payload) {
        persist(currentActor(), null, action, entityType, entityId, entityLabel, result, payload, null, null, null);
    }

    public void record(
            String action,
            String entityType,
            String entityId,
            String entityLabel,
            AuditLog.Result result,
            Object payload,
            Object response) {
        persist(currentActor(), null, action, entityType, entityId, entityLabel, result, payload, response, null, null);
    }

    public void recordFor(
            User actor,
            String action,
            String entityType,
            String entityId,
            String entityLabel,
            AuditLog.Result result,
            Object payload) {
        persist(actor, null, action, entityType, entityId, entityLabel, result, payload, null, null, null);
    }

    public void recordFor(
            User actor,
            String action,
            String entityType,
            String entityId,
            String entityLabel,
            AuditLog.Result result,
            Object payload,
            Object response) {
        persist(actor, null, action, entityType, entityId, entityLabel, result, payload, response, null, null);
    }

    public void recordGuest(
            String attemptedEmail,
            String action,
            String entityType,
            String entityId,
            String entityLabel,
            AuditLog.Result result,
            Object payload) {
        persist(null, attemptedEmail, action, entityType, entityId, entityLabel, result, payload, null, null, null);
    }

    public void recordSemantic(
            String action,
            String entityType,
            String entityId,
            String entityLabel,
            AuditLog.Result result,
            Object payload,
            Object oldValue,
            Object newValue) {
        persist(currentActor(), null, action, entityType, entityId, entityLabel, result, payload, null, oldValue, newValue);
    }

    public void recordSemantic(
            String action,
            String entityType,
            String entityId,
            String entityLabel,
            AuditLog.Result result,
            Object payload,
            Object response,
            Object oldValue,
            Object newValue) {
        persist(currentActor(), null, action, entityType, entityId, entityLabel, result, payload, response, oldValue, newValue);
    }

    public void recordSemanticFor(
            User actor,
            String action,
            String entityType,
            String entityId,
            String entityLabel,
            AuditLog.Result result,
            Object payload,
            Object oldValue,
            Object newValue) {
        persist(actor, null, action, entityType, entityId, entityLabel, result, payload, null, oldValue, newValue);
    }

    public void recordSemanticFor(
            User actor,
            String action,
            String entityType,
            String entityId,
            String entityLabel,
            AuditLog.Result result,
            Object payload,
            Object response,
            Object oldValue,
            Object newValue) {
        persist(actor, null, action, entityType, entityId, entityLabel, result, payload, response, oldValue, newValue);
    }

    public void recordSemanticGuest(
            String attemptedEmail,
            String action,
            String entityType,
            String entityId,
            String entityLabel,
            AuditLog.Result result,
            Object payload,
            Object oldValue,
            Object newValue) {
        persist(null, attemptedEmail, action, entityType, entityId, entityLabel, result, payload, null, oldValue, newValue);
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
            Object payload,
            Object response,
            Object oldValue,
            Object newValue) {
        try {
            HttpServletRequest request = currentRequest();
            String ip = request != null ? extractIp(request) : null;
            String ua = request != null ? truncate(request.getHeader("User-Agent"), 512) : null;
            String httpMethod = request != null ? request.getMethod() : null;
            String httpPath = request != null ? truncate(request.getRequestURI(), 255) : null;
            String payloadJson = auditLogContentService.serializePayload(action, payload);
            String responseJson = auditLogContentService.serializeResponse(response);
            String oldValueJson = auditLogContentService.serializeState(oldValue);
            String newValueJson = auditLogContentService.serializeState(newValue);

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
                    .response(responseJson)
                    .oldValue(oldValueJson)
                    .newValue(newValueJson)
                    .build();

            markCurrentRequestAsAudited(request);
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
        return auditLogContentService.serializeState(payload);
    }

    public String serializePayload(String action, Object payload) {
        return auditLogContentService.serializePayload(action, payload);
    }

    public String serializeResponse(Object response) {
        return auditLogContentService.serializeResponse(response);
    }

    public void markCurrentRequestAsAudited() {
        markCurrentRequestAsAudited(currentRequest());
    }

    public boolean isRequestAlreadyAudited(HttpServletRequest request) {
        return request != null && Boolean.TRUE.equals(request.getAttribute(REQUEST_AUDITED_ATTRIBUTE));
    }

    private void markCurrentRequestAsAudited(HttpServletRequest request) {
        if (request != null) {
            request.setAttribute(REQUEST_AUDITED_ATTRIBUTE, Boolean.TRUE);
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
