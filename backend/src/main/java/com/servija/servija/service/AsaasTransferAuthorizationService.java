package com.servija.servija.service;

import com.servija.servija.model.WithdrawalRequest;
import com.servija.servija.repository.WithdrawalRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsaasTransferAuthorizationService {

    private final WithdrawalRequestRepository withdrawalRepository;

    /**
     * Valida a transferência enviada pelo mecanismo de segurança do Asaas.
     * Se reconhecida, responde APPROVED para substituir a confirmação manual via SMS/app.
     */
    public Map<String, String> authorizeTransfer(Map<String, Object> payload) {
        String type = asString(payload.get("type"));
        if (!"TRANSFER".equalsIgnoreCase(type)) {
            log.warn("Tipo de autorização do Asaas não suportado: {}", type);
            return refused("Tipo de autorização não suportado");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> transfer = (Map<String, Object>) payload.get("transfer");

        if (transfer == null || transfer.isEmpty()) {
            return refused("Transferência ausente no payload");
        }

        String transferId = asString(transfer.get("id"));
        if (!StringUtils.hasText(transferId)) {
            return refused("Transferência sem ID");
        }

        WithdrawalRequest withdrawal = withdrawalRepository.findFirstByAsaasTransferId(transferId)
                .orElse(null);

        if (withdrawal == null) {
            log.warn("Transferência Asaas não encontrada para autorização: {}", transferId);
            return refused("Transferência não encontrada no ServiJá");
        }

        String validationError = validateTransfer(withdrawal, transfer);
        if (validationError != null) {
            log.warn("Transferência Asaas recusada: {} - {}", transferId, validationError);
            return refused(validationError);
        }

        log.info("Transferência Asaas autorizada via webhook: {} (saque {})",
                transferId, withdrawal.getId());
        return Map.of("status", "APPROVED");
    }

    private String validateTransfer(WithdrawalRequest withdrawal, Map<String, Object> transfer) {
        BigDecimal transferValue = toBigDecimal(transfer.get("value"));
        if (transferValue == null) {
            return "Valor da transferência ausente";
        }

        if (withdrawal.getAmount() == null || withdrawal.getAmount().compareTo(transferValue) != 0) {
            return "Valor da transferência divergente";
        }

        String operationType = asString(transfer.get("operationType"));
        if (withdrawal.getWithdrawalMethod() == WithdrawalRequest.WithdrawalMethod.PIX
                && StringUtils.hasText(operationType)
                && !"PIX".equalsIgnoreCase(operationType)) {
            return "Tipo de operação divergente para saque PIX";
        }

        if (withdrawal.getWithdrawalMethod() == WithdrawalRequest.WithdrawalMethod.TED
                && StringUtils.hasText(operationType)
                && !"TED".equalsIgnoreCase(operationType)) {
            return "Tipo de operação divergente para saque TED";
        }

        return null;
    }

    private Map<String, String> refused(String reason) {
        Map<String, String> response = new LinkedHashMap<>();
        response.put("status", "REFUSED");
        response.put("refuseReason", reason);
        return response;
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof BigDecimal decimal) {
            return decimal;
        }

        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }

        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
