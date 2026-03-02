package com.servija.servija.service;

import com.servija.servija.exception.ResourceNotFoundException;
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

    private static final BigDecimal MIN_WITHDRAWAL_AMOUNT = new BigDecimal("10.00");
    private static final BigDecimal MAX_WITHDRAWAL_AMOUNT = new BigDecimal("50000.00");

    /**
     * Solicita saque
     */
    @Transactional
    public WithdrawalRequest requestWithdrawal(
            String email,
            BigDecimal amount,
            String pixKey,
            String pixKeyType
    ) {
        User freelancer = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        // Validações
        if (amount.compareTo(MIN_WITHDRAWAL_AMOUNT) < 0) {
            throw new IllegalArgumentException(
                "Valor mínimo para saque é R$ " + MIN_WITHDRAWAL_AMOUNT
            );
        }

        if (amount.compareTo(MAX_WITHDRAWAL_AMOUNT) > 0) {
            throw new IllegalArgumentException(
                "Valor máximo para saque é R$ " + MAX_WITHDRAWAL_AMOUNT
            );
        }

        if (!walletService.hasSufficientBalance(freelancer, amount)) {
            throw new IllegalStateException("Saldo insuficiente");
        }

        // Criar solicitação
        WithdrawalRequest withdrawal = WithdrawalRequest.builder()
                .freelancer(freelancer)
                .amount(amount)
                .pixKey(pixKey)
                .pixKeyType(pixKeyType)
                .transactionId(UUID.randomUUID().toString())
                .status(WithdrawalRequest.WithdrawalStatus.PENDING)
                .build();

        withdrawal = withdrawalRepository.save(withdrawal);

        // Deduzir do saldo disponível imediatamente
        walletService.deductAvailableBalance(freelancer, amount);

        log.info("Solicitação de saque criada: {} - R$ {}", withdrawal.getId(), amount);

        // Processar saque automaticamente (ou pode ser manual via admin)
        processWithdrawal(withdrawal.getId());

        return withdrawal;
    }

    /**
     * Processa saque (transfere via PIX do Asaas)
     */
    @Transactional
    public void processWithdrawal(Long withdrawalId) {
        WithdrawalRequest withdrawal = withdrawalRepository.findById(withdrawalId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação não encontrada"));

        if (withdrawal.getStatus() != WithdrawalRequest.WithdrawalStatus.PENDING) {
            throw new IllegalStateException("Solicitação já foi processada");
        }

        withdrawal.setStatus(WithdrawalRequest.WithdrawalStatus.PROCESSING);
        withdrawalRepository.save(withdrawal);

        try {
            // Realizar transferência PIX via Asaas
            Map<String, Object> transfer = asaasService.createPixTransfer(
                withdrawal.getAmount(),
                withdrawal.getPixKey(),
                withdrawal.getPixKeyType(),
                "Saque ServiJá - " + withdrawal.getTransactionId()
            );

            withdrawal.setAsaasTransferId((String) transfer.get("id"));
            withdrawal.setStatus(WithdrawalRequest.WithdrawalStatus.COMPLETED);
            withdrawal.setProcessedAt(LocalDateTime.now());
            withdrawalRepository.save(withdrawal);

            log.info("Saque processado com sucesso: {}", withdrawal.getId());

        } catch (Exception e) {
            log.error("Erro ao processar saque: {}", withdrawal.getId(), e);
            
            // Reverter saldo em caso de erro
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
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

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
     * Cancela solicitação de saque
     */
    @Transactional
    public void cancelWithdrawal(Long withdrawalId, String email) {
        WithdrawalRequest withdrawal = withdrawalRepository.findById(withdrawalId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação não encontrada"));

        User freelancer = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        if (!withdrawal.getFreelancer().getId().equals(freelancer.getId())) {
            throw new IllegalStateException("Você não tem permissão para cancelar esta solicitação");
        }

        if (withdrawal.getStatus() != WithdrawalRequest.WithdrawalStatus.PENDING) {
            throw new IllegalStateException("Apenas solicitações pendentes podem ser canceladas");
        }

        withdrawal.setStatus(WithdrawalRequest.WithdrawalStatus.CANCELLED);
        withdrawalRepository.save(withdrawal);

        // Devolver saldo
        walletService.addPendingBalance(freelancer, withdrawal.getAmount());

        log.info("Solicitação de saque cancelada: {}", withdrawalId);
    }
}
