package com.servija.servija.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsaasService {

    @Value("${asaas.api.url}")
    private String asaasApiUrl;

    @Value("${asaas.api.key}")
    private String asaasApiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @PostConstruct
    public void init() {
        boolean isSandbox = asaasApiUrl != null && asaasApiUrl.contains("sandbox");
        log.info("Asaas configurado - Ambiente: {} | URL: {}",
                isSandbox ? "SANDBOX" : "PRODUCAO", asaasApiUrl);

        if (!isSandbox) {
            log.warn("ATENCAO: Asaas esta configurado para PRODUCAO. Cobrancas serao reais.");
        }
    }

    /**
     * Cria ou recupera um cliente no Asaas garantindo que as notificacoes pagas
     * fiquem desativadas.
     */
    public String getOrCreateCustomer(String email, String name, String cpfCnpj) {
        try {
            String existingCustomerId = findCustomerByEmail(email);
            if (existingCustomerId != null) {
                log.info("Cliente ja existe no Asaas: {}", existingCustomerId);
                updateCustomerProfile(existingCustomerId, name, email, cpfCnpj);
                disableCustomerNotifications(existingCustomerId);
                return existingCustomerId;
            }

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(
                    buildCustomerPayload(name, email, cpfCnpj),
                    createHeaders());

            ResponseEntity<String> response = restTemplate.exchange(
                    asaasApiUrl + "/v3/customers",
                    HttpMethod.POST,
                    request,
                    String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                String customerId = jsonNode.path("id").asText(null);
                if (!StringUtils.hasText(customerId)) {
                    throw new RuntimeException("Resposta invalida ao criar cliente no Asaas");
                }

                disableCustomerNotifications(customerId);
                log.info("Cliente criado no Asaas com notificacoes desativadas: {}", customerId);
                return customerId;
            }

            throw new RuntimeException("Erro ao criar cliente no Asaas");
        } catch (Exception e) {
            log.error("Erro ao criar/buscar cliente no Asaas", e);
            throw new RuntimeException("Erro ao processar cliente no Asaas: " + e.getMessage(), e);
        }
    }

    /**
     * Desativa notificacoes automáticas do Asaas para evitar taxa de mensageria.
     * O metodo e idempotente e pode ser reutilizado pelo backfill operacional.
     */
    public void disableCustomerNotifications(String customerId) {
        try {
            updateCustomerNotificationPreference(customerId);

            List<JsonNode> notifications = listCustomerNotifications(customerId);
            if (notifications.isEmpty()) {
                log.info("Cliente {} nao possui notificacoes para atualizar no Asaas", customerId);
                return;
            }

            Map<String, Object> batchPayload = new HashMap<>();
            batchPayload.put("customer", customerId);
            batchPayload.put("notifications", notifications.stream()
                    .map(this::buildDisabledNotificationPayload)
                    .toList());

            executeNotificationBatchUpdate(batchPayload);
            log.info("Notificacoes do cliente {} desativadas no Asaas: {}", customerId, notifications.size());
        } catch (Exception e) {
            log.error("Erro ao desativar notificacoes do cliente {} no Asaas", customerId, e);
            throw new RuntimeException("Erro ao desativar notificacoes do cliente no Asaas: " + e.getMessage(), e);
        }
    }

    /**
     * Busca cliente por email
     */
    private String findCustomerByEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return null;
        }

        try {
            String url = UriComponentsBuilder.fromUriString(asaasApiUrl + "/v3/customers")
                    .queryParam("email", email)
                    .toUriString();

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(createHeaders()),
                    String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                JsonNode data = jsonNode.path("data");

                if (data.isArray() && data.size() > 0) {
                    return data.get(0).path("id").asText(null);
                }
            }

            return null;
        } catch (Exception e) {
            log.error("Erro ao buscar cliente no Asaas", e);
            return null;
        }
    }

    private void updateCustomerProfile(String customerId, String name, String email, String cpfCnpj) {
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(
                buildCustomerPayload(name, email, cpfCnpj),
                createHeaders());

        ResponseEntity<String> response = restTemplate.exchange(
                asaasApiUrl + "/v3/customers/" + customerId,
                HttpMethod.PUT,
                request,
                String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Erro ao atualizar cliente no Asaas");
        }

        log.info("Perfil do cliente atualizado no Asaas: {}", customerId);
    }

    private void updateCustomerNotificationPreference(String customerId) {
        Map<String, Object> customerData = new HashMap<>();
        customerData.put("notificationDisabled", true);

        ResponseEntity<String> response = restTemplate.exchange(
                asaasApiUrl + "/v3/customers/" + customerId,
                HttpMethod.PUT,
                new HttpEntity<>(customerData, createHeaders()),
                String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Erro ao desativar notificacoes padrao do cliente no Asaas");
        }
    }

    private Map<String, Object> buildCustomerPayload(String name, String email, String cpfCnpj) {
        Map<String, Object> customerData = new HashMap<>();
        if (StringUtils.hasText(name)) {
            customerData.put("name", name);
        }
        if (StringUtils.hasText(email)) {
            customerData.put("email", email);
        }
        if (StringUtils.hasText(cpfCnpj)) {
            customerData.put("cpfCnpj", cpfCnpj);
        }
        customerData.put("notificationDisabled", true);
        return customerData;
    }

    private List<JsonNode> listCustomerNotifications(String customerId) throws Exception {
        String customerEndpoint = asaasApiUrl + "/v3/customers/" + customerId + "/notifications";
        try {
            return extractNotificationData(fetchJson(customerEndpoint));
        } catch (Exception firstError) {
            log.warn("Falha ao buscar notificacoes pelo endpoint do cliente {}. Tentando endpoint alternativo.",
                    customerId);
            String fallbackEndpoint = UriComponentsBuilder.fromUriString(asaasApiUrl + "/v3/notifications")
                    .queryParam("customer", customerId)
                    .toUriString();
            return extractNotificationData(fetchJson(fallbackEndpoint));
        }
    }

    private JsonNode fetchJson(String url) throws Exception {
        ResponseEntity<String> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                new HttpEntity<>(createHeaders()),
                String.class);

        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new RuntimeException("Resposta invalida do Asaas para " + url);
        }

        return objectMapper.readTree(response.getBody());
    }

    private List<JsonNode> extractNotificationData(JsonNode jsonNode) {
        JsonNode data = jsonNode.path("data");
        JsonNode notificationsNode = data.isMissingNode() || data.isNull() ? jsonNode : data;

        List<JsonNode> notifications = new ArrayList<>();
        if (notificationsNode.isArray()) {
            notificationsNode.forEach(notifications::add);
        }
        return notifications;
    }

    private Map<String, Object> buildDisabledNotificationPayload(JsonNode notification) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", notification.path("id").asText());
        payload.put("enabled", false);
        payload.put("emailEnabledForProvider", false);
        payload.put("smsEnabledForProvider", false);
        payload.put("emailEnabledForCustomer", false);
        payload.put("smsEnabledForCustomer", false);
        payload.put("phoneCallEnabledForCustomer", false);
        payload.put("whatsappEnabledForCustomer", false);
        return payload;
    }

    private void executeNotificationBatchUpdate(Map<String, Object> payload) {
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, createHeaders());

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    asaasApiUrl + "/v3/notifications/batch",
                    HttpMethod.PUT,
                    request,
                    String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                return;
            }
        } catch (Exception putError) {
            log.warn("Falha ao atualizar notificacoes do Asaas via PUT. Tentando fallback via POST.");
        }

        ResponseEntity<String> fallbackResponse = restTemplate.exchange(
                asaasApiUrl + "/v3/notifications/batch",
                HttpMethod.POST,
                request,
                String.class);

        HttpStatusCode status = fallbackResponse.getStatusCode();
        if (!status.is2xxSuccessful()) {
            throw new RuntimeException("Erro ao atualizar notificacoes em lote no Asaas");
        }
    }

    /**
     * Cria uma cobranca no Asaas
     */
    public Map<String, Object> createPayment(
            String customerId,
            BigDecimal value,
            LocalDate dueDate,
            String billingType,
            String description) {
        try {
            HttpHeaders headers = createHeaders();

            Map<String, Object> paymentData = new HashMap<>();
            paymentData.put("customer", customerId);
            paymentData.put("billingType", billingType); // BOLETO, CREDIT_CARD, PIX
            paymentData.put("value", value);
            paymentData.put("dueDate", dueDate.format(DateTimeFormatter.ISO_DATE));
            paymentData.put("postalService", false);

            if (description != null && !description.isEmpty()) {
                paymentData.put("description", description);
            }

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(paymentData, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    asaasApiUrl + "/v3/payments",
                    HttpMethod.POST,
                    request,
                    String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());

                Map<String, Object> result = new HashMap<>();
                result.put("id", jsonNode.get("id").asText());
                result.put("invoiceUrl", jsonNode.get("invoiceUrl").asText());
                result.put("status", jsonNode.get("status").asText());

                if (jsonNode.has("bankSlipUrl")) {
                    result.put("bankSlipUrl", jsonNode.get("bankSlipUrl").asText());
                }

                if (jsonNode.has("nossoNumero")) {
                    result.put("nossoNumero", jsonNode.get("nossoNumero").asText());
                }

                log.info("Cobranca criada no Asaas: {}", result.get("id"));
                return result;
            }

            throw new RuntimeException("Erro ao criar cobranca no Asaas");
        } catch (Exception e) {
            log.error("Erro ao criar cobranca no Asaas", e);
            throw new RuntimeException("Erro ao processar pagamento no Asaas: " + e.getMessage(), e);
        }
    }

    /**
     * Obtem QR Code PIX para uma cobranca
     */
    public Map<String, String> getPixQrCode(String paymentId) {
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    asaasApiUrl + "/v3/payments/" + paymentId + "/pixQrCode",
                    HttpMethod.GET,
                    new HttpEntity<>(createHeaders()),
                    String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());

                Map<String, String> result = new HashMap<>();
                result.put("encodedImage", jsonNode.get("encodedImage").asText());
                result.put("payload", jsonNode.get("payload").asText());

                if (jsonNode.has("expirationDate")) {
                    result.put("expirationDate", jsonNode.get("expirationDate").asText());
                }

                log.info("QR Code PIX obtido para pagamento: {}", paymentId);
                return result;
            }

            throw new RuntimeException("Erro ao obter QR Code PIX");
        } catch (Exception e) {
            log.error("Erro ao obter QR Code PIX", e);
            throw new RuntimeException("Erro ao obter QR Code PIX: " + e.getMessage(), e);
        }
    }

    /**
     * Consulta status de um pagamento
     */
    public String getPaymentStatus(String paymentId) {
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    asaasApiUrl + "/v3/payments/" + paymentId,
                    HttpMethod.GET,
                    new HttpEntity<>(createHeaders()),
                    String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                return jsonNode.get("status").asText();
            }

            throw new RuntimeException("Erro ao consultar status do pagamento");
        } catch (Exception e) {
            log.error("Erro ao consultar status do pagamento", e);
            throw new RuntimeException("Erro ao consultar pagamento: " + e.getMessage(), e);
        }
    }

    /**
     * Obtem URLs de recibo/comprovante de um pagamento no Asaas
     * Consulta GET /v3/payments/{id} e extrai transactionReceiptUrl e invoiceUrl
     */
    public Map<String, String> getPaymentReceiptUrl(String asaasPaymentId) {
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    asaasApiUrl + "/v3/payments/" + asaasPaymentId,
                    HttpMethod.GET,
                    new HttpEntity<>(createHeaders()),
                    String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());

                Map<String, String> result = new HashMap<>();

                if (jsonNode.has("transactionReceiptUrl") && !jsonNode.get("transactionReceiptUrl").isNull()) {
                    result.put("transactionReceiptUrl", jsonNode.get("transactionReceiptUrl").asText());
                }

                if (jsonNode.has("invoiceUrl") && !jsonNode.get("invoiceUrl").isNull()) {
                    result.put("invoiceUrl", jsonNode.get("invoiceUrl").asText());
                }

                log.info("URLs de recibo obtidas para pagamento {}: receipt={}, invoice={}",
                        asaasPaymentId,
                        result.getOrDefault("transactionReceiptUrl", "N/A"),
                        result.getOrDefault("invoiceUrl", "N/A"));

                return result;
            }

            throw new RuntimeException("Erro ao consultar pagamento no Asaas");
        } catch (Exception e) {
            log.error("Erro ao obter URLs de recibo do pagamento: {}", asaasPaymentId, e);
            throw new RuntimeException("Erro ao obter recibo do pagamento: " + e.getMessage(), e);
        }
    }

    /**
     * Cria headers para requisicao ao Asaas
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("access_token", asaasApiKey);
        return headers;
    }

    /**
     * Cria transferencia PIX para conta externa
     */
    public Map<String, Object> createPixTransfer(
            BigDecimal value,
            String pixKey,
            String pixKeyType,
            String description) {
        try {
            HttpHeaders headers = createHeaders();

            Map<String, Object> transferData = new HashMap<>();
            transferData.put("value", value);
            transferData.put("pixAddressKey", pixKey);

            if (pixKeyType != null && !pixKeyType.isEmpty()) {
                transferData.put("pixAddressKeyType", pixKeyType.toUpperCase());
            }

            if (description != null && !description.isEmpty()) {
                transferData.put("description", description);
            }

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(transferData, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    asaasApiUrl + "/v3/transfers",
                    HttpMethod.POST,
                    request,
                    String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());

                Map<String, Object> result = new HashMap<>();
                result.put("id", jsonNode.get("id").asText());
                result.put("status", jsonNode.get("status").asText());
                result.put("value", jsonNode.get("value").asDouble());

                if (jsonNode.has("transactionId")) {
                    result.put("transactionId", jsonNode.get("transactionId").asText());
                }

                log.info("Transferencia PIX criada no Asaas: {}", result.get("id"));
                return result;
            }

            throw new RuntimeException("Erro ao criar transferencia PIX");
        } catch (Exception e) {
            log.error("Erro ao criar transferencia PIX no Asaas", e);
            throw new RuntimeException("Erro ao processar transferencia PIX: " + e.getMessage(), e);
        }
    }

    /**
     * Consulta status de uma transferencia
     */
    public String getTransferStatus(String transferId) {
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    asaasApiUrl + "/v3/transfers/" + transferId,
                    HttpMethod.GET,
                    new HttpEntity<>(createHeaders()),
                    String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                return jsonNode.get("status").asText();
            }

            throw new RuntimeException("Erro ao consultar status da transferencia");
        } catch (Exception e) {
            log.error("Erro ao consultar status da transferencia", e);
            throw new RuntimeException("Erro ao consultar transferencia: " + e.getMessage(), e);
        }
    }

    /**
     * Cria transferencia TED para conta bancaria
     */
    public Map<String, Object> createTedTransfer(
            BigDecimal value,
            String bankCode,
            String agency,
            String accountNumber,
            String accountDigit,
            String accountType,
            String holderName,
            String holderCpfCnpj,
            String description) {
        try {
            HttpHeaders headers = createHeaders();

            Map<String, Object> bankAccount = new HashMap<>();
            bankAccount.put("bank", Map.of("code", bankCode));
            bankAccount.put("accountName", holderName);
            bankAccount.put("ownerName", holderName);
            bankAccount.put("cpfCnpj", holderCpfCnpj);
            bankAccount.put("agency", agency);
            bankAccount.put("account", accountNumber);
            bankAccount.put("accountDigit", accountDigit);
            bankAccount.put("bankAccountType",
                    "CONTA_POUPANCA".equals(accountType) ? "CONTA_POUPANCA" : "CONTA_CORRENTE");

            Map<String, Object> transferData = new HashMap<>();
            transferData.put("value", value);
            transferData.put("bankAccount", bankAccount);
            transferData.put("operationType", "TED");

            if (description != null && !description.isEmpty()) {
                transferData.put("description", description);
            }

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(transferData, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    asaasApiUrl + "/v3/transfers",
                    HttpMethod.POST,
                    request,
                    String.class);

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());

                Map<String, Object> result = new HashMap<>();
                result.put("id", jsonNode.get("id").asText());
                result.put("status", jsonNode.get("status").asText());
                result.put("value", jsonNode.get("value").asDouble());

                log.info("Transferencia TED criada no Asaas: {}", result.get("id"));
                return result;
            }

            throw new RuntimeException("Erro ao criar transferencia TED");
        } catch (Exception e) {
            log.error("Erro ao criar transferencia TED no Asaas", e);
            throw new RuntimeException("Erro ao processar transferencia TED: " + e.getMessage(), e);
        }
    }
}
