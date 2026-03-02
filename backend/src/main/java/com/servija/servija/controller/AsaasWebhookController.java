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
     * Endpoint principal para receber webhooks do Asaas
     * URL a configurar no Asaas: https://seu-dominio.com/api/webhooks/asaas
     */
    @PostMapping
    @Operation(summary = "Receber webhook do Asaas", description = "Endpoint para receber notificações de eventos do Asaas")
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
}
