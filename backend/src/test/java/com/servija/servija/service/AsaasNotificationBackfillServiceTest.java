package com.servija.servija.service;

import com.servija.servija.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AsaasNotificationBackfillServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private AsaasService asaasService;

    private AsaasNotificationBackfillService backfillService;

    @BeforeEach
    void setUp() {
        backfillService = new AsaasNotificationBackfillService(paymentRepository, asaasService);
    }

    @Test
    @DisplayName("Deve continuar o backfill mesmo quando um cliente falha")
    void runBackfillOnStartup_continuesAfterFailure() {
        ReflectionTestUtils.setField(backfillService, "backfillOnStartup", true);

        when(paymentRepository.findDistinctAsaasCustomerIds()).thenReturn(List.of("cus_1", "cus_2"));
        doNothing().when(asaasService).disableCustomerNotifications("cus_1");
        doThrow(new RuntimeException("boom")).when(asaasService).disableCustomerNotifications("cus_2");

        backfillService.runBackfillOnStartup();

        verify(asaasService).disableCustomerNotifications("cus_1");
        verify(asaasService).disableCustomerNotifications("cus_2");
    }

    @Test
    @DisplayName("Não deve executar backfill quando a flag estiver desligada")
    void runBackfillOnStartup_skipsWhenDisabled() {
        ReflectionTestUtils.setField(backfillService, "backfillOnStartup", false);

        backfillService.runBackfillOnStartup();

        verify(paymentRepository, never()).findDistinctAsaasCustomerIds();
    }
}
