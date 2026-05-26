package com.servija.servija.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
public class AuditLogContentService {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private static final Set<String> REDACTED_KEYS = Set.of(
            "password",
            "currentpassword",
            "newpassword",
            "confirmpassword",
            "hash",
            "token",
            "adminresetpasswordtokenhash",
            "verificationcode",
            "resetpasswordtoken",
            "pixkeyvalue");

    private static final Set<String> DOCUMENT_KEYS = Set.of(
            "cpf",
            "cnpj",
            "holdercpfcnpj");

    private static final Set<String> PIX_KEYS = Set.of(
            "pixkey");

    private static final Set<String> ACCOUNT_KEYS = Set.of(
            "accountnumber",
            "accountdigit");

    private static final Set<String> AGENCY_KEYS = Set.of(
            "agency",
            "agencydigit");

    private static final Set<String> BANK_KEYS = Set.of(
            "bankcode");

    private final ObjectMapper objectMapper;

    public AuditLogContentService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String serializePayload(String action, Object rawPayload) {
        Object payload = buildPayload(action, rawPayload);
        return serializeSanitized(payload);
    }

    public String serializeState(Object snapshot) {
        return serializeSanitized(snapshot);
    }

    public Object sanitizeValue(Object value) {
        if (value == null) {
            return null;
        }
        Object normalized = objectMapper.convertValue(value, Object.class);
        return sanitizeRecursive(normalized, null);
    }

    private Object buildPayload(String action, Object rawPayload) {
        if (action == null || action.isBlank() || rawPayload == null) {
            return null;
        }

        Map<String, Object> data = asMap(rawPayload);
        if (data.isEmpty()) {
            return null;
        }

        return switch (action) {
            case "USER_REGISTERED" -> mapOf("role", firstPresent(data, "role", "userType"));
            case "LOGIN" -> mapOf("method", firstPresent(data, "method"));
            case "LOGIN_FAILED" -> mapOf("reason", normalizeLoginFailureReason(firstPresent(data, "reason")));
            case "EMAIL_VERIFIED" -> mapOf("attempts", firstPresent(data, "attempts"));
            case "BANK_ACCOUNT_ADDED" -> mapOf(
                    "type", normalizeBankAccountType(firstPresent(data, "type", "accountType")),
                    "keyType", firstPresent(data, "keyType", "pixKeyType"));
            case "WITHDRAWAL_REQUESTED" -> mapOf(
                    "amount", firstPresent(data, "amount"),
                    "method", firstPresent(data, "method", "withdrawalMethod"));
            case "PROFILE_UPDATED", "FREELANCER_PROFILE_UPDATED", "ADMIN_USER_UPDATED", "SERVICE_UPDATED" -> mapOf(
                    "fieldsChanged", normalizeFieldsChanged(data.get("fieldsChanged")));
            default -> data.containsKey("fieldsChanged")
                    ? mapOf("fieldsChanged", normalizeFieldsChanged(data.get("fieldsChanged")))
                    : null;
        };
    }

    private String serializeSanitized(Object value) {
        Object sanitized = sanitizeValue(value);
        if (!hasMeaningfulContent(sanitized)) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(sanitized);
        } catch (JsonProcessingException exception) {
            return null;
        }
    }

    private boolean hasMeaningfulContent(Object value) {
        if (value == null) {
            return false;
        }
        if (value instanceof Map<?, ?> map) {
            return !map.isEmpty();
        }
        if (value instanceof Collection<?> collection) {
            return !collection.isEmpty();
        }
        return true;
    }

    private Object sanitizeRecursive(Object value, String parentKey) {
        if (value == null) {
            return null;
        }

        if (value instanceof Map<?, ?> map) {
            Map<String, Object> sanitized = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                String key = String.valueOf(entry.getKey());
                String normalizedKey = normalizeKey(key);
                Object sanitizedValue = sanitizeRecursive(entry.getValue(), normalizedKey);
                if (sanitizedValue == null && shouldDropEmptyKey(normalizedKey)) {
                    continue;
                }
                sanitized.put(key, sanitizedValue);
            }
            return sanitized;
        }

        if (value instanceof Collection<?> collection) {
            List<Object> sanitized = new ArrayList<>();
            for (Object item : collection) {
                sanitized.add(sanitizeRecursive(item, parentKey));
            }
            return sanitized;
        }

        if (value.getClass().isArray()) {
            List<Object> sanitized = new ArrayList<>();
            Object[] array = (Object[]) value;
            for (Object item : array) {
                sanitized.add(sanitizeRecursive(item, parentKey));
            }
            return sanitized;
        }

        if (value instanceof CharSequence sequence) {
            return sanitizeScalar(sequence.toString(), parentKey);
        }

        if (parentKey != null && BANK_KEYS.contains(parentKey)) {
            return "[REDACTED]";
        }

        return value;
    }

    private boolean shouldDropEmptyKey(String normalizedKey) {
        return normalizedKey != null
                && (REDACTED_KEYS.contains(normalizedKey)
                || DOCUMENT_KEYS.contains(normalizedKey)
                || PIX_KEYS.contains(normalizedKey)
                || ACCOUNT_KEYS.contains(normalizedKey)
                || AGENCY_KEYS.contains(normalizedKey)
                || BANK_KEYS.contains(normalizedKey));
    }

    private Object sanitizeScalar(String value, String normalizedKey) {
        if (normalizedKey == null) {
            return value;
        }
        if (REDACTED_KEYS.contains(normalizedKey)) {
            return "[REDACTED]";
        }
        if (DOCUMENT_KEYS.contains(normalizedKey)) {
            return maskDocument(value);
        }
        if (PIX_KEYS.contains(normalizedKey)) {
            return maskPixKey(value);
        }
        if (ACCOUNT_KEYS.contains(normalizedKey)) {
            return maskAccountNumber(value);
        }
        if (AGENCY_KEYS.contains(normalizedKey)) {
            return maskAgency(value);
        }
        if (BANK_KEYS.contains(normalizedKey)) {
            return "[REDACTED]";
        }
        return value;
    }

    private Map<String, Object> asMap(Object value) {
        if (value == null) {
            return Map.of();
        }
        Object normalized = objectMapper.convertValue(value, Object.class);
        if (normalized instanceof Map<?, ?> map) {
            return objectMapper.convertValue(map, MAP_TYPE);
        }
        return Map.of();
    }

    private Map<String, Object> mapOf(Object... keyValues) {
        Map<String, Object> values = new LinkedHashMap<>();
        for (int index = 0; index + 1 < keyValues.length; index += 2) {
            Object value = keyValues[index + 1];
            if (value == null) {
                continue;
            }
            if (value instanceof Collection<?> collection && collection.isEmpty()) {
                continue;
            }
            values.put(String.valueOf(keyValues[index]), value);
        }
        return values.isEmpty() ? null : values;
    }

    private Object firstPresent(Map<String, Object> data, String... keys) {
        for (String key : keys) {
            if (data.containsKey(key) && data.get(key) != null) {
                return data.get(key);
            }
        }
        return null;
    }

    private List<String> normalizeFieldsChanged(Object value) {
        if (value == null) {
            return List.of();
        }
        if (value instanceof Collection<?> collection) {
            return collection.stream()
                    .filter(Objects::nonNull)
                    .map(String::valueOf)
                    .filter(item -> !item.isBlank())
                    .distinct()
                    .toList();
        }
        return List.of(String.valueOf(value));
    }

    private String normalizeLoginFailureReason(Object rawReason) {
        String normalized = normalizeKey(String.valueOf(rawReason));
        return switch (normalized) {
            case "usernotfound",
                    "resourcenotfoundexception" -> "user_not_found";
            case "accountsuspended",
                    "contasuspensa" -> "account_suspended";
            case "toomanyattempts",
                    "toomanyattemptsexception" -> "too_many_attempts";
            case "invalidcredentialsexception",
                    "badcredentialsexception",
                    "invalidcredentials",
                    "credentialsinvalidas" -> "invalid_credentials";
            default -> "invalid_credentials";
        };
    }

    private String normalizeBankAccountType(Object rawType) {
        if (rawType == null) {
            return null;
        }
        String value = String.valueOf(rawType).trim().toUpperCase(Locale.ROOT);
        if ("PIX_KEY".equals(value)) {
            return "PIX";
        }
        if ("BANK_ACCOUNT".equals(value)) {
            return "TED";
        }
        return value;
    }

    private String normalizeKey(String value) {
        if (value == null) {
            return null;
        }
        return value.replaceAll("[^A-Za-z0-9]", "").toLowerCase(Locale.ROOT);
    }

    private String maskDocument(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String digits = value.replaceAll("\\D", "");
        if (digits.length() < 3) {
            return "***";
        }
        return "**" + digits.substring(digits.length() - 3);
    }

    private String maskPixKey(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        if (value.length() <= 3) {
            return "***";
        }
        return "**" + value.substring(value.length() - 3);
    }

    private String maskAccountNumber(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        String digits = value.replaceAll("\\D", "");
        if (digits.length() <= 4) {
            return "****";
        }
        return "**" + digits.substring(digits.length() - 4);
    }

    private String maskAgency(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return "*";
    }
}
