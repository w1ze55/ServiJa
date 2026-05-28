package com.servija.servija.scheduler;

import com.servija.servija.model.Payment;
import com.servija.servija.model.ServiceRequest;
import com.servija.servija.repository.PaymentRepository;
import com.servija.servija.service.FreelancerWalletService;
import com.servija.servija.service.PlatformWalletService;
import com.servija.servija.service.ServiceRequestHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Scheduler para liberar automaticamente o saldo pendente quando o cliente
 * nao aprova a entrega dentro de 7 dias.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WalletScheduler {

    private final PaymentRepository paymentRepository;
    private final FreelancerWalletService walletService;
    private final PlatformWalletService platformWalletService;
    private final ServiceRequestHistoryService historyService;

    private static final int AUTO_RELEASE_DAYS = 7;

    /**
     * Executa a cada hora para verificar pagamentos pendentes de liberacao.
     * Regra: Se o servico foi entregue (DELIVERED) ha mais de 7 dias
     * e o cliente nao aprovou, o saldo e liberado automaticamente.
     */
    @Scheduled(fixedRate = 3600000)
    @Transactional
    public void autoReleaseExpiredPendingBalances() {
        log.info("Executando auto-release de saldos pendentes...");

        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(AUTO_RELEASE_DAYS);

        List<Payment> payments = paymentRepository.findAll();
        int released = 0;

        for (Payment payment : payments) {
            if (payment.getStatus() != Payment.PaymentStatus.COMPLETED) {
                continue;
            }
            if (payment.getServiceRequest() == null) {
                continue;
            }

            ServiceRequest serviceRequest = payment.getServiceRequest();

            if (serviceRequest.getStatus() == ServiceRequest.Status.DELIVERED
                    && serviceRequest.getUpdatedAt() != null
                    && serviceRequest.getUpdatedAt().isBefore(cutoffDate)) {

                try {
                    serviceRequest.setStatus(ServiceRequest.Status.COMPLETED);

                    walletService.releasePendingToAvailable(
                            payment.getFreelancer().getEmail(),
                            payment.getAmount());
                    platformWalletService.releasePlatformFeeIfNeeded(payment);
                    historyService.record(
                            serviceRequest,
                            payment.getFreelancer(),
                            com.servija.servija.model.ServiceRequestHistory.OperationType.SERVICE_COMPLETED,
                            "Servico concluido automaticamente apos 7 dias sem aprovacao do cliente");

                    released++;
                    log.info(
                            "Saldo auto-liberado para freelancer {} - Valor: {} - ServiceRequest: {}",
                            payment.getFreelancer().getEmail(),
                            payment.getAmount(),
                            serviceRequest.getId());
                } catch (Exception e) {
                    log.error("Erro ao auto-liberar saldo para payment {}: {}", payment.getId(), e.getMessage());
                    throw new RuntimeException("Falha ao executar auto-release da carteira", e);
                }
            }
        }

        if (released > 0) {
            log.info("Auto-release concluido: {} pagamentos liberados", released);
        }
    }
}
