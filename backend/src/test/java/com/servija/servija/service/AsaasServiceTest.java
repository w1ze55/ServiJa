package com.servija.servija.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.servija.servija.exception.AsaasIntegrationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsaasServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private AsaasService asaasService;

    @BeforeEach
    void setUp() {
        asaasService = new AsaasService(restTemplate, new ObjectMapper());
        ReflectionTestUtils.setField(asaasService, "asaasApiUrl", "https://api-sandbox.asaas.com");
        ReflectionTestUtils.setField(asaasService, "asaasApiKey", "$$aact_hmlg_test_key");
    }

    @Test
    @DisplayName("Deve criar cliente com notificationDisabled=true e desativar notificacoes em lote")
    @SuppressWarnings({"unchecked", "rawtypes"})
    void getOrCreateCustomer_createsCustomerWithNotificationsDisabled() {
        ArgumentCaptor<HttpEntity> createCustomerCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        ArgumentCaptor<HttpEntity> batchUpdateCaptor = ArgumentCaptor.forClass(HttpEntity.class);

        when(restTemplate.exchange(
                contains("/v3/customers?email="),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"data\":[]}"));

        when(restTemplate.exchange(
                eq("https://api-sandbox.asaas.com/v3/customers"),
                eq(HttpMethod.POST),
                createCustomerCaptor.capture(),
                eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"id\":\"cus_123\"}"));

        when(restTemplate.exchange(
                eq("https://api-sandbox.asaas.com/v3/customers/cus_123"),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                eq(String.class)))
                .thenReturn(ResponseEntity.ok("{}"));

        when(restTemplate.exchange(
                eq("https://api-sandbox.asaas.com/v3/customers/cus_123/notifications"),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"data\":[{\"id\":\"not_1\"}]}"));

        when(restTemplate.exchange(
                eq("https://api-sandbox.asaas.com/v3/notifications/batch"),
                eq(HttpMethod.PUT),
                batchUpdateCaptor.capture(),
                eq(String.class)))
                .thenReturn(ResponseEntity.ok("{}"));

        String customerId = asaasService.getOrCreateCustomer("cliente@servija.com", "Cliente", "12345678901");

        assertThat(customerId).isEqualTo("cus_123");

        Map<String, Object> createPayload = (Map<String, Object>) createCustomerCaptor.getValue().getBody();
        assertThat(createPayload.get("notificationDisabled")).isEqualTo(true);
        assertThat(createPayload.get("cpfCnpj")).isEqualTo("12345678901");
        assertThat(createPayload.get("email")).isEqualTo("cliente@servija.com");
        assertThat(createCustomerCaptor.getValue().getHeaders().getFirst("access_token"))
                .isEqualTo("$aact_hmlg_test_key");
        assertThat(createCustomerCaptor.getValue().getHeaders().getFirst(HttpHeaders.USER_AGENT))
                .isEqualTo("ServiJa");

        Map<String, Object> batchPayload = (Map<String, Object>) batchUpdateCaptor.getValue().getBody();
        assertThat(batchPayload.get("customer")).isEqualTo("cus_123");
        assertThat(batchPayload.get("notifications")).isInstanceOf(List.class);

        Map<String, Object> notificationPayload = (Map<String, Object>) ((List<?>) batchPayload.get("notifications")).get(0);
        assertThat(notificationPayload.get("id")).isEqualTo("not_1");
        assertThat(notificationPayload.get("enabled")).isEqualTo(false);
        assertThat(notificationPayload.get("emailEnabledForProvider")).isEqualTo(false);
        assertThat(notificationPayload.get("smsEnabledForProvider")).isEqualTo(false);
        assertThat(notificationPayload.get("emailEnabledForCustomer")).isEqualTo(false);
        assertThat(notificationPayload.get("smsEnabledForCustomer")).isEqualTo(false);
        assertThat(notificationPayload.get("phoneCallEnabledForCustomer")).isEqualTo(false);
        assertThat(notificationPayload.get("whatsappEnabledForCustomer")).isEqualTo(false);

        verify(restTemplate).exchange(
                eq("https://api-sandbox.asaas.com/v3/customers/cus_123"),
                eq(HttpMethod.PUT),
                any(HttpEntity.class),
                eq(String.class));
    }

    @Test
    @DisplayName("Deve rejeitar chave de sandbox configurada com URL de producao")
    void getOrCreateCustomer_rejectsSandboxKeyOnProductionUrl() {
        ReflectionTestUtils.setField(asaasService, "asaasApiUrl", "https://api.asaas.com");
        ReflectionTestUtils.setField(asaasService, "asaasApiKey", "$aact_hmlg_test_key");

        assertThatThrownBy(() -> asaasService.getOrCreateCustomer("cliente@servija.com", "Cliente", "12345678901"))
                .isInstanceOf(AsaasIntegrationException.class)
                .hasMessageContaining("sandbox")
                .hasMessageContaining("producao");

        verifyNoInteractions(restTemplate);
    }
}
