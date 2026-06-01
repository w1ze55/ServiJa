package com.servija.servija.service;

import com.servija.servija.dto.WalletTransactionResponse;
import com.servija.servija.model.User;
import com.servija.servija.repository.UserRepository;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletReportServiceTest {

    @Mock
    private WalletTransactionService transactionService;
    @Mock
    private FreelancerWalletService walletService;
    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private WalletReportService service;

    @Test
    @DisplayName("generateReport gera planilha Excel com resumo e transacoes")
    void generateReportGeneratesExcel() throws Exception {
        User user = User.builder().id(1L).name("Freela").email("freela@test.com").build();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(transactionService.getTransactions(user.getEmail(), "ALL")).thenReturn(List.of(
                WalletTransactionResponse.builder()
                        .type("EARNING")
                        .description("Logo")
                        .amount(new BigDecimal("120.00"))
                        .status("COMPLETED")
                        .paymentMethod("PIX")
                        .date(LocalDateTime.of(2026, 1, 10, 10, 0))
                        .build()));
        when(walletService.getWalletBalance(user.getEmail())).thenReturn(Map.of(
                "totalEarned", new BigDecimal("200.00"),
                "availableBalance", new BigDecimal("120.00"),
                "pendingBalance", new BigDecimal("80.00"),
                "totalWithdrawn", new BigDecimal("50.00")));

        WalletReportService.ReportFile report = service.generateReport(user.getEmail(), WalletReportService.ReportFormat.EXCEL);

        assertThat(report.filename()).isEqualTo("relatorio-carteira.xlsx");
        assertThat(report.contentType()).contains("spreadsheetml");
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(report.content()))) {
            assertThat(workbook.getSheet("Resumo")).isNotNull();
            assertThat(workbook.getSheet("Transacoes")).isNotNull();
            assertThat(workbook.getSheet("Transacoes").getRow(1).getCell(0).getStringCellValue()).isEqualTo("Recebimento");
        }
    }

    @Test
    @DisplayName("generateReport gera PDF com header valido")
    void generateReportGeneratesPdf() {
        User user = User.builder().id(1L).name("Freela").email("freela@test.com").build();
        when(userRepository.findByEmail(user.getEmail())).thenReturn(Optional.of(user));
        when(transactionService.getTransactions(user.getEmail(), "ALL")).thenReturn(List.of());
        when(walletService.getWalletBalance(user.getEmail())).thenReturn(Map.of(
                "totalEarned", BigDecimal.ZERO,
                "availableBalance", BigDecimal.ZERO,
                "pendingBalance", BigDecimal.ZERO,
                "totalWithdrawn", BigDecimal.ZERO));

        WalletReportService.ReportFile report = service.generateReport(user.getEmail(), WalletReportService.ReportFormat.PDF);

        assertThat(report.filename()).isEqualTo("relatorio-carteira.pdf");
        assertThat(new String(report.content(), 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
    }
}
