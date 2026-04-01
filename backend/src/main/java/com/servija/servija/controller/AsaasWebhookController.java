package com.servija.servija.controller;

import com.servija.servija.service.AsaasTransferAuthorizationService;
import com.servija.servija.service.AsaasWebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/webhooks/asaas")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Webhooks Asaas", description = "Endpoints para receber notificacoes do Asaas")
public class AsaasWebhookController {

    private final AsaasWebhookService webhookService;
    private final AsaasTransferAuthorizationService transferAuthorizationService;

    @Value("${asaas.webhook.token:}")
    private String webhookToken;

    /**
     * Endpoint principal para receber eventos comuns do Asaas.
     */
    @PostMapping
    @Operation(summary = "Receber webhook do Asaas", description = "Endpoint para receber notificacoes de eventos do Asaas")
    public ResponseEntity<Map<String, String>> handleWebhook(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "asaas-access-token", required = false) String accessToken
    ) {
        try {
            if (!isValidToken(accessToken)) {
                log.warn("Webhook recebido com token invalido");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Token invalido"));
            }

            log.info("Webhook recebido do Asaas: {}", payload);
            webhookService.processWebhook(payload);
            return ResponseEntity.ok(Map.of("status", "received"));

        } catch (Exception e) {
            log.error("Erro ao processar webhook do Asaas", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Endpoint para o mecanismo de seguranca do Asaas que substitui a
     * confirmacao manual via SMS/app por uma aprovacao sistêmica.
     */
    @PostMapping("/transfer-authorization")
    @Operation(
            summary = "Autorizar saque do Asaas",
            description = "Endpoint para o mecanismo de seguranca do Asaas que responde APPROVED ou REFUSED para transferencias"
    )
    public ResponseEntity<Map<String, String>> handleTransferAuthorization(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "asaas-access-token", required = false) String accessToken
    ) {
        try {
            if (!isValidToken(accessToken)) {
                log.warn("Webhook de autorizacao de saque recebido com token invalido");
                return ResponseEntity.ok(Map.of(
                        "status", "REFUSED",
                        "refuseReason", "Token invalido"
                ));
            }

            log.info("Webhook de autorizacao de saque recebido do Asaas: {}", payload);
            return ResponseEntity.ok(transferAuthorizationService.authorizeTransfer(payload));

        } catch (Exception e) {
            log.error("Erro ao autorizar saque do Asaas", e);
            return ResponseEntity.ok(Map.of(
                    "status", "REFUSED",
                    "refuseReason", "Erro interno ao validar transferencia"
            ));
        }
    }

    /**
     * Endpoint para receber webhooks de transacoes PIX.
     */
    @PostMapping("/pix-transaction")
    @Operation(
            summary = "Receber webhook de transacao PIX",
            description = "Endpoint para receber notificacoes de transacoes PIX recebidas via QR Code estatico ou chave PIX"
    )
    public ResponseEntity<Map<String, String>> handlePixTransactionWebhook(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "asaas-access-token", required = false) String accessToken
    ) {
        try {
            if (!isValidToken(accessToken)) {
                log.warn("Webhook PIX Transaction recebido com token invalido");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("error", "Token invalido"));
            }

            log.info("Webhook PIX Transaction recebido: {}", payload);
            webhookService.processPixTransactionWebhook(payload);
            return ResponseEntity.ok(Map.of("status", "received"));

        } catch (Exception e) {
            log.error("Erro ao processar webhook de transacao PIX", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    @Operation(summary = "Validar endpoint de webhook", description = "Endpoint para validacao da URL pelo Asaas")
    public ResponseEntity<Map<String, String>> validateWebhook() {
        return ResponseEntity.ok(Map.of(
                "status", "active",
                "message", "Webhook endpoint esta funcionando"
        ));
    }

    @GetMapping("/pix-transaction")
    @Operation(summary = "Validar endpoint de webhook PIX", description = "Endpoint para validacao da URL de webhook de transacoes PIX")
    public ResponseEntity<Map<String, String>> validatePixTransactionWebhook() {
        return ResponseEntity.ok(Map.of(
                "status", "active",
                "message", "Webhook PIX Transaction endpoint esta funcionando"
        ));
    }

    @GetMapping("/transfer-authorization")
    @Operation(summary = "Validar endpoint de autorizacao de saque", description = "Endpoint para validacao da URL do mecanismo de seguranca do Asaas")
    public ResponseEntity<Map<String, String>> validateTransferAuthorizationWebhook() {
        return ResponseEntity.ok(Map.of(
                "status", "active",
                "message", "Webhook de autorizacao de saque ativo"
        ));
    }

    private boolean isValidToken(String accessToken) {
        return webhookToken == null
                || webhookToken.isEmpty()
                || (accessToken != null && accessToken.equals(webhookToken));
    }
}
