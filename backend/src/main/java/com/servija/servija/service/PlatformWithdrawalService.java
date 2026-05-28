package com.servija.servija.service;

import com.servija.servija.exception.ResourceNotFoundException;
import com.servija.servija.model.PlatformBankAccount;
import com.servija.servija.model.PlatformWallet;
import com.servija.servija.model.PlatformWithdrawalRequest;
import com.servija.servija.repository.PlatformWithdrawalRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlatformWithdrawalService {

    private static final BigDecimal MIN_WITHDRAWAL_AMOUNT = new BigDecimal("10.00");
    private static final BigDecimal MAX_WITHDRAWAL_AMOUNT = new BigDecimal("50000.00");

    private final PlatformWithdrawalRequestRepository platformWithdrawalRequestRepository;
    private final PlatformWalletService platformWalletService;
    private final PlatformBankAccountService platformBankAccountService;
    private final AsaasService asaasService;
    private final AuditLogger auditLogger;

    @Transactional
    public PlatformWithdrawalRequest requestWithdrawal(BigDecimal amount, Long bankAccountId) {
        if (amount == null) {
            throw new IllegalArgumentException("O valor do saque é obrigatório");
        }

        if (amount.compareTo(MIN_WITHDRAWAL_AMOUNT) < 0) {
            throw new IllegalArgumentException("O valor mínimo para saque é R$ " + MIN_WITHDRAWAL_AMOUNT);
        }

        if (amount.compareTo(MAX_WITHDRAWAL_AMOUNT) > 0) {
            throw new IllegalArgumentException("O valor máximo para saque é R$ " + MAX_WITHDRAWAL_AMOUNT);
        }

        PlatformBankAccount account = platformBankAccountService.getRequiredAccount(bankAccountId);
        validateAccount(account);

        PlatformWallet wallet = platformWalletService.lockWallet();
        if (wallet.getAvailableBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Saldo insuficiente");
        }

        PlatformWithdrawalRequest.WithdrawalMethod method =
                account.getAccountType() == PlatformBankAccount.AccountType.PIX_KEY
                        ? PlatformWithdrawalRequest.WithdrawalMethod.PIX
                        : PlatformWithdrawalRequest.WithdrawalMethod.TED;

        wallet.reserveWithdrawal(amount);

        PlatformWithdrawalRequest withdrawal = PlatformWithdrawalRequest.builder()
                .amount(amount)
                .status(PlatformWithdrawalRequest.WithdrawalStatus.PENDING)
                .withdrawalMethod(method)
                .bankAccountId(account.getId())
                .bankAccountType(account.getAccountType())
                .bankName(account.getBankName())
                .bankCode(account.getBankCode())
                .agency(account.getAgency())
                .agencyDigit(account.getAgencyDigit())
                .accountNumber(account.getAccountNumber())
                .accountDigit(account.getAccountDigit())
                .accountBankType(account.getAccountBankType())
                .pixKey(account.getPixKey())
                .pixKeyType(account.getPixKeyType())
                .holderName(account.getHolderName())
                .holderCpfCnpj(account.getHolderCpfCnpj())
                .destinationLabel(platformBankAccountService.buildDestinationLabel(account))
                .transactionId(UUID.randomUUID().toString())
                .build();

        withdrawal = platformWithdrawalRequestRepository.save(withdrawal);
        withdrawal.setStatus(PlatformWithdrawalRequest.WithdrawalStatus.PROCESSING);
        platformWithdrawalRequestRepository.save(withdrawal);

        try {
            Map<String, Object> transfer = createTransfer(withdrawal);
            withdrawal.setAsaasTransferId((String) transfer.get("id"));
            withdrawal.setStatus(PlatformWithdrawalRequest.WithdrawalStatus.COMPLETED);
            withdrawal.setProcessedAt(LocalDateTime.now());
            wallet.completeWithdrawal(amount);
        } catch (Exception e) {
            withdrawal.setStatus(PlatformWithdrawalRequest.WithdrawalStatus.FAILED);
            withdrawal.setRejectionReason(e.getMessage());
            wallet.releaseReservedWithdrawal(amount);
            log.error("Erro ao processar saque da plataforma {}", withdrawal.getId(), e);
        }

        platformWithdrawalRequestRepository.save(withdrawal);

        Map<String, Object> auditPayload = new LinkedHashMap<>();
        auditPayload.put("amount", withdrawal.getAmount());
        auditPayload.put("withdrawalMethod", withdrawal.getWithdrawalMethod().name());
        auditPayload.put("destination", withdrawal.getDestinationLabel());
        auditLogger.recordSemantic(
                "PLATFORM_WITHDRAWAL_REQUESTED",
                "PLATFORM_WITHDRAWAL",
                String.valueOf(withdrawal.getId()),
                withdrawal.getDestinationLabel(),
                withdrawal.getStatus() == PlatformWithdrawalRequest.WithdrawalStatus.FAILED
                        ? com.servija.servija.model.AuditLog.Result.FAILURE
                        : com.servija.servija.model.AuditLog.Result.SUCCESS,
                auditPayload,
                buildAuditSnapshot(withdrawal),
                null,
                null);

        return withdrawal;
    }

    @Transactional(readOnly = true)
    public List<PlatformWithdrawalRequest> getWithdrawals() {
        return platformWithdrawalRequestRepository.findAllByOrderByCreatedAtDesc();
    }

    @Transactional(readOnly = true)
    public PlatformWithdrawalRequest getById(Long id) {
        return platformWithdrawalRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação não encontrada"));
    }

    private void validateAccount(PlatformBankAccount account) {
        if (account.getAccountType() == PlatformBankAccount.AccountType.PIX_KEY) {
            if (account.getPixKey() == null || account.getPixKey().isBlank()) {
                throw new IllegalStateException("A conta selecionada não possui uma chave PIX válida");
            }
            return;
        }

        if (isBlank(account.getBankCode())
                || isBlank(account.getAgency())
                || isBlank(account.getAccountNumber())
                || account.getAccountBankType() == null
                || isBlank(account.getHolderName())
                || isBlank(account.getHolderCpfCnpj())) {
            throw new IllegalStateException("A conta bancária selecionada está incompleta para saque via TED");
        }
    }

    private Map<String, Object> createTransfer(PlatformWithdrawalRequest withdrawal) {
        String description = "Saque plataforma Servi Já - " + withdrawal.getTransactionId();

        if (withdrawal.getWithdrawalMethod() == PlatformWithdrawalRequest.WithdrawalMethod.TED) {
            return asaasService.createTedTransfer(
                    withdrawal.getAmount(),
                    withdrawal.getBankCode(),
                    withdrawal.getAgency(),
                    withdrawal.getAccountNumber(),
                    withdrawal.getAccountDigit(),
                    withdrawal.getAccountBankType() != null
                            ? withdrawal.getAccountBankType().name()
                            : PlatformBankAccount.BankAccountType.CONTA_CORRENTE.name(),
                    withdrawal.getHolderName(),
                    withdrawal.getHolderCpfCnpj(),
                    description);
        }

        return asaasService.createPixTransfer(
                withdrawal.getAmount(),
                withdrawal.getPixKey(),
                withdrawal.getPixKeyType(),
                description);
    }

    private Map<String, Object> buildAuditSnapshot(PlatformWithdrawalRequest withdrawal) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("id", withdrawal.getId());
        snapshot.put("amount", withdrawal.getAmount());
        snapshot.put("status", withdrawal.getStatus() != null ? withdrawal.getStatus().name() : null);
        snapshot.put("withdrawalMethod", withdrawal.getWithdrawalMethod() != null ? withdrawal.getWithdrawalMethod().name() : null);
        snapshot.put("bankAccountId", withdrawal.getBankAccountId());
        snapshot.put("destinationLabel", withdrawal.getDestinationLabel());
        snapshot.put("transactionId", withdrawal.getTransactionId());
        snapshot.put("rejectionReason", withdrawal.getRejectionReason());
        snapshot.put("processedAt", withdrawal.getProcessedAt());
        snapshot.put("createdAt", withdrawal.getCreatedAt());
        return snapshot;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
