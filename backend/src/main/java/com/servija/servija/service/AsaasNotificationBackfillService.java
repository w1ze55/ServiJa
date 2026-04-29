package com.servija.servija.service;

import com.servija.servija.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AsaasNotificationBackfillService {

    @Value("${asaas.notifications.backfill-on-startup:false}")
    private boolean backfillOnStartup;

    private final PaymentRepository paymentRepository;
    private final AsaasService asaasService;

    @EventListener(ApplicationReadyEvent.class)
    public void runBackfillOnStartup() {
        if (!backfillOnStartup) {
            log.info("Backfill de notificacoes do Asaas desativado no startup.");
            return;
        }

        List<String> customerIds = paymentRepository.findDistinctAsaasCustomerIds();
        if (customerIds.isEmpty()) {
            log.info("Nenhum cliente Asaas encontrado para backfill de notificacoes.");
            return;
        }

        int successCount = 0;
        int failureCount = 0;

        for (String customerId : customerIds) {
            try {
                asaasService.disableCustomerNotifications(customerId);
                successCount++;
            } catch (Exception e) {
                failureCount++;
                log.error("Falha ao aplicar backfill de notificacoes para o cliente Asaas {}", customerId, e);
            }
        }

        log.info(
                "Backfill de notificacoes do Asaas finalizado. Clientes processados: {}, sucessos: {}, falhas: {}",
                customerIds.size(),
                successCount,
                failureCount);
    }
}
