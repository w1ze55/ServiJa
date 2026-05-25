package com.servija.servija.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class AuditLogContentServiceTest {

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AuditLogContentService auditLogContentService = new AuditLogContentService(objectMapper);

    @Test
    void serializesRegistrationPayloadUsingSemanticRoleKey() throws Exception {
        String payloadJson = auditLogContentService.serializePayload(
                "USER_REGISTERED",
                Map.of(
                        "userType", "FREELANCER",
                        "cpf", "12345678901"));

        assertEquals(
                Map.of("role", "FREELANCER"),
                readMap(payloadJson));
    }

    @Test
    void normalizesLoginFailureReason() throws Exception {
        String payloadJson = auditLogContentService.serializePayload(
                "LOGIN_FAILED",
                Map.of("reason", "Conta suspensa"));

        assertEquals(
                Map.of("reason", "account_suspended"),
                readMap(payloadJson));
    }

    @Test
    void omitsPayloadForActionsWithoutSemanticPayload() {
        String payloadJson = auditLogContentService.serializePayload(
                "BANK_ACCOUNT_DELETED",
                Map.of("accountType", "PIX_KEY"));

        assertNull(payloadJson);
    }

    @Test
    void normalizesBankAccountPayloadWithoutLeakingSensitiveFields() throws Exception {
        String payloadJson = auditLogContentService.serializePayload(
                "BANK_ACCOUNT_ADDED",
                Map.of(
                        "accountType", "BANK_ACCOUNT",
                        "pixKeyType", "CPF",
                        "pixKeyValue", "12345678901",
                        "holderCpfCnpj", "12345678901"));

        assertEquals(
                Map.of(
                        "type", "TED",
                        "keyType", "CPF"),
                readMap(payloadJson));
    }

    @Test
    void serializesProfilePayloadWithFieldNamesOnly() throws Exception {
        String payloadJson = auditLogContentService.serializePayload(
                "PROFILE_UPDATED",
                Map.of(
                        "fieldsChanged", List.of("bio", "phone"),
                        "bio", "novo texto",
                        "phone", "47999999999"));

        assertEquals(
                Map.of("fieldsChanged", List.of("bio", "phone")),
                readMap(payloadJson));
    }

    @Test
    void serializesServiceUpdatePayloadWithChangedFieldNamesOnly() throws Exception {
        String payloadJson = auditLogContentService.serializePayload(
                "SERVICE_UPDATED",
                Map.of(
                        "fieldsChanged", List.of("title"),
                        "title", "Nome novo"));

        assertEquals(
                Map.of("fieldsChanged", List.of("title")),
                readMap(payloadJson));
    }

    @Test
    void sanitizesSensitiveDataFromStateSnapshots() throws Exception {
        String stateJson = auditLogContentService.serializeState(Map.of(
                "cpf", "12345678901",
                "password", "secret",
                "pixKey", "user@example.com",
                "accountNumber", "12345678",
                "agency", "1234",
                "bankCode", "001",
                "holderCpfCnpj", "10987654321"));

        Map<String, Object> sanitized = readMap(stateJson);
        assertEquals("**901", sanitized.get("cpf"));
        assertEquals("[REDACTED]", sanitized.get("password"));
        assertEquals("**com", sanitized.get("pixKey"));
        assertEquals("**5678", sanitized.get("accountNumber"));
        assertEquals("*", sanitized.get("agency"));
        assertEquals("[REDACTED]", sanitized.get("bankCode"));
        assertEquals("**321", sanitized.get("holderCpfCnpj"));
    }

    private Map<String, Object> readMap(String json) throws Exception {
        return objectMapper.readValue(json, MAP_TYPE);
    }
}
