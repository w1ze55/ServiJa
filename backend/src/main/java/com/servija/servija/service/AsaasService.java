package com.servija.servija.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsaasService {

    @Value("${asaas.api.url}")
    private String asaasApiUrl;

    @Value("${asaas.api.key}")
    private String asaasApiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    public void init() {
        boolean isSandbox = asaasApiUrl != null && asaasApiUrl.contains("sandbox");
        log.info("🏦 Asaas configurado - Ambiente: {} | URL: {}",
                isSandbox ? "SANDBOX" : "PRODUÇÃO", asaasApiUrl);

        if (!isSandbox) {
            log.warn("⚠️ATENÇÃO⚠️: Asaas está configurado para PRODUÇÃO! Cobranças serão REAIS.");
        }
    }

    /**
     * Cria ou recupera um cliente no Asaas
     */
    public String getOrCreateCustomer(String email, String name, String cpfCnpj) {
        try {
            // Primeiro tenta buscar o cliente existente
            String existingCustomerId = findCustomerByEmail(email);
            if (existingCustomerId != null) {
                log.info("Cliente já existe no Asaas: {}", existingCustomerId);

                // Se o CPF foi fornecido, atualizar o cliente para garantir que tem CPF
                if (cpfCnpj != null && !cpfCnpj.isEmpty()) {
                    updateCustomerCpf(existingCustomerId, cpfCnpj);
                }

                return existingCustomerId;
            }

            // Se não existe, cria um novo
            HttpHeaders headers = createHeaders();

            Map<String, Object> customerData = new HashMap<>();
            customerData.put("name", name);
            customerData.put("email", email);
            if (cpfCnpj != null && !cpfCnpj.isEmpty()) {
                customerData.put("cpfCnpj", cpfCnpj);
            }

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(customerData, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    asaasApiUrl + "/v3/customers",
                    HttpMethod.POST,
                    request,
                    String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                String customerId = jsonNode.get("id").asText();
                log.info("Cliente criado no Asaas: {}", customerId);
                return customerId;
            }

            throw new RuntimeException("Erro ao criar cliente no Asaas");
        } catch (Exception e) {
            log.error("Erro ao criar/buscar cliente no Asaas", e);
            throw new RuntimeException("Erro ao processar cliente no Asaas: " + e.getMessage());
        }
    }

    /**
     * Atualiza o CPF de um cliente existente no Asaas
     */
    private void updateCustomerCpf(String customerId, String cpfCnpj) {
        try {
            HttpHeaders headers = createHeaders();

            Map<String, Object> customerData = new HashMap<>();
            customerData.put("cpfCnpj", cpfCnpj);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(customerData, headers);

            restTemplate.exchange(
                    asaasApiUrl + "/v3/customers/" + customerId,
                    HttpMethod.PUT,
                    request,
                    String.class);

            log.info("CPF atualizado para cliente no Asaas: {}", customerId);
        } catch (Exception e) {
            log.warn("Erro ao atualizar CPF do cliente no Asaas: {}", e.getMessage());
            // Não lança exceção aqui para não quebrar o fluxo
        }
    }

    /**
     * Busca cliente por email
     */
    private String findCustomerByEmail(String email) {
        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<?> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    asaasApiUrl + "/v3/customers?email=" + email,
                    HttpMethod.GET,
                    request,
                    String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                JsonNode data = jsonNode.get("data");

                if (data != null && data.isArray() && data.size() > 0) {
                    return data.get(0).get("id").asText();
                }
            }

            return null;
        } catch (Exception e) {
            log.error("Erro ao buscar cliente no Asaas", e);
            return null;
        }
    }

    /**
     * Cria uma cobrança no Asaas
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

            // Desabilitar notificações automáticas para evitar taxa de R$ 0,99
            paymentData.put("postalService", false); // Desabilita notificação por carta

            if (description != null && !description.isEmpty()) {
                paymentData.put("description", description);
            }

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(paymentData, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    asaasApiUrl + "/v3/payments",
                    HttpMethod.POST,
                    request,
                    String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
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

                log.info("Cobrança criada no Asaas: {}", result.get("id"));
                return result;
            }

            throw new RuntimeException("Erro ao criar cobrança no Asaas");
        } catch (Exception e) {
            log.error("Erro ao criar cobrança no Asaas", e);
            throw new RuntimeException("Erro ao processar pagamento no Asaas: " + e.getMessage());
        }
    }

    /**
     * Obtém QR Code PIX para uma cobrança
     */
    public Map<String, String> getPixQrCode(String paymentId) {
        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<?> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    asaasApiUrl + "/v3/payments/" + paymentId + "/pixQrCode",
                    HttpMethod.GET,
                    request,
                    String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
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
            throw new RuntimeException("Erro ao obter QR Code PIX: " + e.getMessage());
        }
    }

    /**
     * Consulta status de um pagamento
     */
    public String getPaymentStatus(String paymentId) {
        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<?> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    asaasApiUrl + "/v3/payments/" + paymentId,
                    HttpMethod.GET,
                    request,
                    String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                return jsonNode.get("status").asText();
            }

            throw new RuntimeException("Erro ao consultar status do pagamento");
        } catch (Exception e) {
            log.error("Erro ao consultar status do pagamento", e);
            throw new RuntimeException("Erro ao consultar pagamento: " + e.getMessage());
        }
    }

    /**
     * Cria headers para requisição ao Asaas
     */
    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("access_token", asaasApiKey);
        return headers;
    }

    /**
     * Cria transferência PIX para conta externa
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

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());

                Map<String, Object> result = new HashMap<>();
                result.put("id", jsonNode.get("id").asText());
                result.put("status", jsonNode.get("status").asText());
                result.put("value", jsonNode.get("value").asDouble());

                if (jsonNode.has("transactionId")) {
                    result.put("transactionId", jsonNode.get("transactionId").asText());
                }

                log.info("Transferência PIX criada no Asaas: {}", result.get("id"));
                return result;
            }

            throw new RuntimeException("Erro ao criar transferência PIX");
        } catch (Exception e) {
            log.error("Erro ao criar transferência PIX no Asaas", e);
            throw new RuntimeException("Erro ao processar transferência PIX: " + e.getMessage());
        }
    }

    /**
     * Consulta status de uma transferência
     */
    public String getTransferStatus(String transferId) {
        try {
            HttpHeaders headers = createHeaders();
            HttpEntity<?> request = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    asaasApiUrl + "/v3/transfers/" + transferId,
                    HttpMethod.GET,
                    request,
                    String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode jsonNode = objectMapper.readTree(response.getBody());
                return jsonNode.get("status").asText();
            }

            throw new RuntimeException("Erro ao consultar status da transferência");
        } catch (Exception e) {
            log.error("Erro ao consultar status da transferência", e);
            throw new RuntimeException("Erro ao consultar transferência: " + e.getMessage());
        }
    }
}
