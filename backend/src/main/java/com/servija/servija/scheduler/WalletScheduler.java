package com.servija.servija.scheduler;

import com.servija.servija.model.Payment;
import com.servija.servija.model.ServiceRequest;
import com.servija.servija.repository.PaymentRepository;
import com.servija.servija.service.FreelancerWalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduler para liberar automaticamente o saldo pendente quando o cliente
 * não aprova a entrega dentro de 7 dias.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WalletScheduler {

    private final PaymentRepository paymentRepository;
    private final FreelancerWalletService walletService;

    private static final int AUTO_RELEASE_DAYS = 7;

    /**
     * Executa a cada hora para verificar pagamentos pendentes de liberação.
     * Regra: Se o serviço foi entregue (DELIVERED) há mais de 7 dias
     * e o cliente não aprovou, o saldo é liberado automaticamente.
     */
    @Scheduled(fixedRate = 3600000) // 1 hora
    @Transactional
    public void autoReleaseExpiredPendingBalances() {
        log.info("⏰ Executando auto-release de saldos pendentes...");

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(AUTO_RELEASE_DAYS);

        List<Payment> payments = paymentRepository.findAll();
        int released = 0;

        for (Payment payment : payments) {
            if (payment.getStatus() != Payment.PaymentStatus.COMPLETED) continue;
            if (payment.getServiceRequest() == null) continue;

            ServiceRequest sr = payment.getServiceRequest();

            // Se o serviço foi entregue e está aguardando aprovação há mais de 7 dias
            if (sr.getStatus() == ServiceRequest.Status.DELIVERED
                    && sr.getUpdatedAt() != null
                    && sr.getUpdatedAt().isBefore(cutoffDate)) {

                try {
                    // Liberar o saldo pendente → disponível
                    walletService.releasePendingToAvailable(
                            payment.getFreelancer().getEmail(),
                            payment.getAmount());

                    // Marcar o service request como COMPLETED
                    sr.setStatus(ServiceRequest.Status.COMPLETED);

                    released++;
                    log.info("✅ Saldo auto-liberado para freelancer {} - Valor: {} - ServiceRequest: {}",
                            payment.getFreelancer().getEmail(),
                            payment.getAmount(),
                            sr.getId());
                } catch (Exception e) {
                    log.error("❌ Erro ao auto-liberar saldo para payment {}: {}",
                            payment.getId(), e.getMessage());
                }
            }
        }

        if (released > 0) {
            log.info("⏰ Auto-release concluído: {} pagamentos liberados", released);
        }
    }
}
