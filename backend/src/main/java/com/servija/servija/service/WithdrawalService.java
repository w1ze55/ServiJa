package com.servija.servija.service;

import com.servija.servija.exception.ResourceNotFoundException;
import com.servija.servija.model.FreelancerBankAccount;
import com.servija.servija.model.User;
import com.servija.servija.model.WithdrawalRequest;
import com.servija.servija.repository.UserRepository;
import com.servija.servija.repository.WithdrawalRequestRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class WithdrawalService {

    private final WithdrawalRequestRepository withdrawalRepository;
    private final UserRepository userRepository;
    private final FreelancerWalletService walletService;
    private final AsaasService asaasService;
    private final BankAccountService bankAccountService;

    private static final BigDecimal MIN_WITHDRAWAL_AMOUNT = new BigDecimal("10.00");
    private static final BigDecimal MAX_WITHDRAWAL_AMOUNT = new BigDecimal("50000.00");

    /**
     * Solicita saque
     */
    @Transactional
    public WithdrawalRequest requestWithdrawal(
            String email,
            BigDecimal amount,
            String withdrawalMethodValue,
            String pixKey,
            String pixKeyType,
            Long bankAccountId
    ) {
        User freelancer = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado"));

        WithdrawalRequest.WithdrawalMethod withdrawalMethod = parseWithdrawalMethod(withdrawalMethodValue);
        FreelancerBankAccount bankAccount = null;

        if (amount.compareTo(MIN_WITHDRAWAL_AMOUNT) < 0) {
            throw new IllegalArgumentException(
                    "Valor minimo para saque e R$ " + MIN_WITHDRAWAL_AMOUNT
            );
        }

        if (amount.compareTo(MAX_WITHDRAWAL_AMOUNT) > 0) {
            throw new IllegalArgumentException(
                    "Valor maximo para saque e R$ " + MAX_WITHDRAWAL_AMOUNT
            );
        }

        if (!walletService.hasSufficientBalance(freelancer, amount)) {
            throw new IllegalStateException("Saldo insuficiente");
        }

        if (withdrawalMethod == WithdrawalRequest.WithdrawalMethod.PIX) {
            if (pixKey == null || pixKey.isBlank()) {
                throw new IllegalArgumentException("Informe a chave PIX");
            }
        } else {
            if (bankAccountId == null) {
                throw new IllegalArgumentException("Selecione uma conta bancaria para TED");
            }

            bankAccount = bankAccountService.getOwnedAccount(email, bankAccountId);
            if (bankAccount.getAccountType() != FreelancerBankAccount.AccountType.BANK_ACCOUNT) {
                throw new IllegalArgumentException("A conta selecionada nao pode ser usada para saque TED");
            }

            validateTedBankAccount(bankAccount);
            pixKey = null;
            pixKeyType = null;
        }

        WithdrawalRequest withdrawal = WithdrawalRequest.builder()
                .freelancer(freelancer)
                .amount(amount)
                .withdrawalMethod(withdrawalMethod)
                .bankAccount(bankAccount)
                .pixKey(pixKey)
                .pixKeyType(pixKeyType)
                .transactionId(UUID.randomUUID().toString())
                .status(WithdrawalRequest.WithdrawalStatus.PENDING)
                .build();

        withdrawal = withdrawalRepository.save(withdrawal);

        walletService.deductAvailableBalance(freelancer, amount);

        log.info("Solicitacao de saque criada: {} - R$ {}", withdrawal.getId(), amount);

        processWithdrawal(withdrawal.getId());

        return withdrawal;
    }

    /**
     * Processa saque (transfere via PIX ou TED do Asaas)
     */
    @Transactional
    public void processWithdrawal(Long withdrawalId) {
        WithdrawalRequest withdrawal = withdrawalRepository.findById(withdrawalId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitacao nao encontrada"));

        if (withdrawal.getStatus() != WithdrawalRequest.WithdrawalStatus.PENDING) {
            throw new IllegalStateException("Solicitacao ja foi processada");
        }

        withdrawal.setStatus(WithdrawalRequest.WithdrawalStatus.PROCESSING);
        withdrawalRepository.save(withdrawal);

        try {
            Map<String, Object> transfer = createTransfer(withdrawal);

            withdrawal.setAsaasTransferId((String) transfer.get("id"));
            withdrawal.setStatus(WithdrawalRequest.WithdrawalStatus.COMPLETED);
            withdrawal.setProcessedAt(LocalDateTime.now());
            withdrawalRepository.save(withdrawal);

            log.info("Saque processado com sucesso: {}", withdrawal.getId());

        } catch (Exception e) {
            log.error("Erro ao processar saque: {}", withdrawal.getId(), e);

            walletService.addPendingBalance(
                    withdrawal.getFreelancer(),
                    withdrawal.getAmount()
            );

            withdrawal.setStatus(WithdrawalRequest.WithdrawalStatus.FAILED);
            withdrawal.setRejectionReason(e.getMessage());
            withdrawalRepository.save(withdrawal);

            throw new RuntimeException("Erro ao processar saque: " + e.getMessage());
        }
    }

    /**
     * Lista saques do freelancer
     */
    public List<WithdrawalRequest> getMyWithdrawals(String email) {
        User freelancer = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado"));

        return withdrawalRepository.findByFreelancerIdOrderByCreatedAtDesc(freelancer.getId());
    }

    /**
     * Lista saques pendentes (para admin)
     */
    public List<WithdrawalRequest> getPendingWithdrawals() {
        return withdrawalRepository.findByStatusOrderByCreatedAtDesc(
                WithdrawalRequest.WithdrawalStatus.PENDING
        );
    }

    /**
     * Cancela solicitacao de saque
     */
    @Transactional
    public void cancelWithdrawal(Long withdrawalId, String email) {
        WithdrawalRequest withdrawal = withdrawalRepository.findById(withdrawalId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitacao nao encontrada"));

        User freelancer = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado"));

        if (!withdrawal.getFreelancer().getId().equals(freelancer.getId())) {
            throw new IllegalStateException("Voce nao tem permissao para cancelar esta solicitacao");
        }

        if (withdrawal.getStatus() != WithdrawalRequest.WithdrawalStatus.PENDING) {
            throw new IllegalStateException("Apenas solicitacoes pendentes podem ser canceladas");
        }

        withdrawal.setStatus(WithdrawalRequest.WithdrawalStatus.CANCELLED);
        withdrawalRepository.save(withdrawal);

        walletService.addPendingBalance(freelancer, withdrawal.getAmount());

        log.info("Solicitacao de saque cancelada: {}", withdrawalId);
    }

    private WithdrawalRequest.WithdrawalMethod parseWithdrawalMethod(String withdrawalMethodValue) {
        if (withdrawalMethodValue == null || withdrawalMethodValue.isBlank()) {
            return WithdrawalRequest.WithdrawalMethod.PIX;
        }

        try {
            return WithdrawalRequest.WithdrawalMethod.valueOf(withdrawalMethodValue.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Metodo de saque invalido");
        }
    }

    private void validateTedBankAccount(FreelancerBankAccount bankAccount) {
        if (isBlank(bankAccount.getBankCode())
                || isBlank(bankAccount.getAgency())
                || isBlank(bankAccount.getAccountNumber())
                || bankAccount.getAccountBankType() == null
                || isBlank(bankAccount.getHolderName())
                || isBlank(bankAccount.getHolderCpfCnpj())) {
            throw new IllegalStateException("A conta bancaria selecionada esta incompleta para saque TED");
        }
    }

    private Map<String, Object> createTransfer(WithdrawalRequest withdrawal) {
        String description = "Saque ServiJa - " + withdrawal.getTransactionId();

        if (withdrawal.getWithdrawalMethod() == WithdrawalRequest.WithdrawalMethod.TED) {
            FreelancerBankAccount bankAccount = withdrawal.getBankAccount();
            if (bankAccount == null) {
                throw new IllegalStateException("Conta bancaria do saque TED nao encontrada");
            }

            return asaasService.createTedTransfer(
                    withdrawal.getAmount(),
                    bankAccount.getBankCode(),
                    bankAccount.getAgency(),
                    bankAccount.getAccountNumber(),
                    bankAccount.getAccountDigit(),
                    bankAccount.getAccountBankType().name(),
                    bankAccount.getHolderName(),
                    bankAccount.getHolderCpfCnpj(),
                    description
            );
        }

        return asaasService.createPixTransfer(
                withdrawal.getAmount(),
                withdrawal.getPixKey(),
                withdrawal.getPixKeyType(),
                description
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
