package com.servija.servija.controller;

import com.servija.servija.service.AsaasWebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/webhooks/asaas")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Webhooks Asaas", description = "Endpoints para receber notificações do Asaas")
public class AsaasWebhookController {

    private final AsaasWebhookService webhookService;

    @Value("${asaas.webhook.token:}")
    private String webhookToken;

    /**
     * Endpoint principal para receber webhooks do Asaas (cobranças e transferências)
     * URL a configurar no Asaas: https://seu-dominio.com/api/webhooks/asaas
     */
    @PostMapping
    @Operation(summary = "Receber webhook do Asaas", description = "Endpoint para receber notificações de eventos do Asaas (cobranças e transferências)")
    public ResponseEntity<Map<String, String>> handleWebhook(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "asaas-access-token", required = false) String accessToken
    ) {
        try {
            // Validar token de autenticação
            if (webhookToken != null && !webhookToken.isEmpty()) {
                if (accessToken == null || !accessToken.equals(webhookToken)) {
                    log.warn("Webhook recebido com token inválido");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(Map.of("error", "Token inválido"));
                }
            }

            log.info("Webhook recebido do Asaas: {}", payload);

            // Processar webhook
            webhookService.processWebhook(payload);

            return ResponseEntity.ok(Map.of("status", "received"));

        } catch (Exception e) {
            log.error("Erro ao processar webhook do Asaas", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Endpoint para receber webhooks de transações PIX (recebimentos via QR Code estático ou chave PIX)
     * URL a configurar no Asaas: https://seu-dominio.com/api/webhooks/asaas/pix-transaction
     * 
     * Este endpoint processa notificações de transações PIX que incluem:
     * - Pagamentos via QR Code estático
     * - Pagamentos via chave PIX
     * - Atualizações de status (DONE, CANCELLED, AWAITING_CRITICAL_ACTION_AUTHORIZATION, etc.)
     */
    @PostMapping("/pix-transaction")
    @Operation(summary = "Receber webhook de transação PIX", 
               description = "Endpoint para receber notificações de transações PIX recebidas via QR Code estático ou chave PIX")
    public ResponseEntity<Map<String, String>> handlePixTransactionWebhook(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "asaas-access-token", required = false) String accessToken
    ) {
        try {
            // Validar token de autenticação
            if (webhookToken != null && !webhookToken.isEmpty()) {
                if (accessToken == null || !accessToken.equals(webhookToken)) {
                    log.warn("Webhook PIX Transaction recebido com token inválido");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                            .body(Map.of("error", "Token inválido"));
                }
            }

            log.info("🔔 Webhook PIX Transaction recebido: {}", payload);

            // Processar webhook de transação PIX
            webhookService.processPixTransactionWebhook(payload);

            return ResponseEntity.ok(Map.of("status", "received"));

        } catch (Exception e) {
            log.error("Erro ao processar webhook de transação PIX", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Endpoint de validação (para testes do Asaas)
     */
    @GetMapping
    @Operation(summary = "Validar endpoint de webhook", description = "Endpoint para validação da URL pelo Asaas")
    public ResponseEntity<Map<String, String>> validateWebhook() {
        return ResponseEntity.ok(Map.of(
                "status", "active",
                "message", "Webhook endpoint está funcionando"
        ));
    }

    /**
     * Endpoint de validação para webhook de PIX Transaction
     */
    @GetMapping("/pix-transaction")
    @Operation(summary = "Validar endpoint de webhook PIX", description = "Endpoint para validação da URL de webhook de transações PIX")
    public ResponseEntity<Map<String, String>> validatePixTransactionWebhook() {
        return ResponseEntity.ok(Map.of(
                "status", "active",
                "message", "Webhook PIX Transaction endpoint está funcionando"
        ));
    }
}
