package com.servija.servija.service;

import com.servija.servija.exception.ResourceNotFoundException;
import com.servija.servija.model.FreelancerWallet;
import com.servija.servija.model.Payment;
import com.servija.servija.model.User;
import com.servija.servija.repository.FreelancerWalletRepository;
import com.servija.servija.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class FreelancerWalletService {

    private final FreelancerWalletRepository walletRepository;
    private final UserRepository userRepository;

    /**
     * Obtém ou cria carteira do freelancer
     */
    @Transactional
    public FreelancerWallet getOrCreateWallet(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        return walletRepository.findByUser(user)
                .orElseGet(() -> {
                    FreelancerWallet wallet = FreelancerWallet.builder()
                            .user(user)
                            .availableBalance(BigDecimal.ZERO)
                            .pendingBalance(BigDecimal.ZERO)
                            .totalEarned(BigDecimal.ZERO)
                            .totalWithdrawn(BigDecimal.ZERO)
                            .build();
                    
                    FreelancerWallet saved = walletRepository.save(wallet);
                    log.info("Carteira criada para freelancer: {}", user.getEmail());
                    return saved;
                });
    }

    /**
     * Obtém carteira por ID do usuário
     */
    public FreelancerWallet getWalletByUserId(Long userId) {
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Carteira não encontrada"));
    }

    /**
     * Adiciona valor pendente (quando pagamento é recebido)
     */
    @Transactional
    public void addPendingBalance(User freelancer, BigDecimal amount) {
        FreelancerWallet wallet = walletRepository.findByUser(freelancer)
                .orElseGet(() -> {
                    FreelancerWallet newWallet = FreelancerWallet.builder()
                            .user(freelancer)
                            .build();
                    return walletRepository.save(newWallet);
                });

        wallet.addPendingBalance(amount);
        walletRepository.save(wallet);
        
        log.info("Saldo pendente adicionado: R$ {} para {}", amount, freelancer.getEmail());
    }

    /**
     * Move saldo pendente para disponível (após período de segurança)
     */
    @Transactional
    public void releasePendingBalance(Payment payment) {
        User freelancer = payment.getFreelancer();
        BigDecimal amount = payment.getAmount(); // Sem taxa da plataforma

        FreelancerWallet wallet = walletRepository.findByUser(freelancer)
                .orElseThrow(() -> new ResourceNotFoundException("Carteira não encontrada"));

        wallet.movePendingToAvailable(amount);
        walletRepository.save(wallet);

        log.info("Saldo liberado: R$ {} para {}", amount, freelancer.getEmail());
    }

    /**
     * Deduz saldo disponível (quando saque é processado)
     */
    @Transactional
    public void deductAvailableBalance(User freelancer, BigDecimal amount) {
        // H10: lock pessimista para serializar saques concorrentes e evitar TOCTOU.
        // A verificação de saldo e a dedução ocorrem sob o lock, dentro da transação.
        FreelancerWallet wallet = walletRepository.findByUserIdForUpdate(freelancer.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Carteira não encontrada"));

        if (wallet.getAvailableBalance().compareTo(amount) < 0) {
            throw new IllegalStateException("Saldo insuficiente");
        }

        wallet.deductAvailableBalance(amount);
        walletRepository.save(wallet);

        log.info("Saldo deduzido: R$ {} de {}", amount, freelancer.getEmail());
    }

    /**
     * Verifica se freelancer tem saldo suficiente
     */
    public boolean hasSufficientBalance(User freelancer, BigDecimal amount) {
        FreelancerWallet wallet = walletRepository.findByUser(freelancer)
                .orElseThrow(() -> new ResourceNotFoundException("Carteira não encontrada"));

        return wallet.getAvailableBalance().compareTo(amount) >= 0;
    }

    /**
     * Retorna saldo da carteira como Map para uso em APIs e relatórios
     */
    public java.util.Map<String, BigDecimal> getWalletBalance(String email) {
        FreelancerWallet wallet = getOrCreateWallet(email);
        java.util.Map<String, BigDecimal> balance = new java.util.HashMap<>();
        balance.put("availableBalance", wallet.getAvailableBalance());
        balance.put("pendingBalance", wallet.getPendingBalance());
        balance.put("totalEarned", wallet.getTotalEarned());
        balance.put("totalWithdrawn", wallet.getTotalWithdrawn());
        return balance;
    }

    /**
     * Move saldo pendente para disponível por email (usado pelo scheduler)
     */
    @Transactional
    public void releasePendingToAvailable(String email, BigDecimal amount) {
        FreelancerWallet wallet = getOrCreateWallet(email);
        wallet.movePendingToAvailable(amount);
        walletRepository.save(wallet);
        log.info("Saldo liberado (auto-release): R$ {} para {}", amount, email);
    }
}
