package com.servija.servija.service;

import com.servija.servija.exception.ResourceNotFoundException;
import com.servija.servija.model.Payment;
import com.servija.servija.model.PlatformWallet;
import com.servija.servija.repository.PaymentRepository;
import com.servija.servija.repository.PlatformWalletRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlatformWalletService {

    private final PlatformWalletRepository platformWalletRepository;
    private final PaymentRepository paymentRepository;

    @Transactional
    public PlatformWallet getOrCreateWallet() {
        return platformWalletRepository.findFirstByOrderByIdAsc()
                .orElseGet(this::createWallet);
    }

    @Transactional
    public PlatformWallet lockWallet() {
        return platformWalletRepository.lockAllOrderByIdAsc().stream()
                .findFirst()
                .orElseGet(this::createWallet);
    }

    @Transactional
    public boolean releasePlatformFeeIfNeeded(Payment payment) {
        if (payment == null || payment.getId() == null) {
            return false;
        }

        Payment lockedPayment = paymentRepository.lockById(payment.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Pagamento não encontrado"));

        if (lockedPayment.getStatus() != Payment.PaymentStatus.COMPLETED) {
            return false;
        }

        if (lockedPayment.getPlatformFeeReleasedAt() != null) {
            return false;
        }

        BigDecimal netPlatformFee = resolveNetPlatformFee(lockedPayment);
        if (netPlatformFee.compareTo(BigDecimal.ZERO) <= 0) {
            lockedPayment.setPlatformFeeReleasedAt(LocalDateTime.now());
            paymentRepository.save(lockedPayment);
            log.warn("Pagamento {} não possui receita líquida para liberar na carteira da plataforma", lockedPayment.getId());
            return false;
        }

        PlatformWallet wallet = lockWallet();
        wallet.creditPlatformFee(netPlatformFee);
        lockedPayment.setPlatformFeeReleasedAt(LocalDateTime.now());

        platformWalletRepository.save(wallet);
        paymentRepository.save(lockedPayment);

        log.info("Receita líquida da plataforma liberada para pagamento {}: R$ {}", lockedPayment.getId(), netPlatformFee);
        return true;
    }

    private BigDecimal resolveNetPlatformFee(Payment payment) {
        BigDecimal platformFee = payment.getPlatformFee() != null ? payment.getPlatformFee() : BigDecimal.ZERO;
        BigDecimal installmentFee = payment.getInstallmentFee() != null ? payment.getInstallmentFee() : BigDecimal.ZERO;
        BigDecimal gatewayFee = payment.getGatewayFee() != null ? payment.getGatewayFee() : BigDecimal.ZERO;
        return platformFee.add(installmentFee).subtract(gatewayFee);
    }

    private PlatformWallet createWallet() {
        PlatformWallet wallet = PlatformWallet.builder()
                .availableBalance(BigDecimal.ZERO)
                .reservedBalance(BigDecimal.ZERO)
                .totalEarned(BigDecimal.ZERO)
                .totalWithdrawn(BigDecimal.ZERO)
                .build();
        return platformWalletRepository.save(wallet);
    }
}
