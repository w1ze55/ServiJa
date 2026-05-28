package com.servija.servija.admin.wallet;

import com.servija.servija.dto.PlatformWalletTransactionResponse;
import com.servija.servija.model.PlatformWallet;
import com.servija.servija.service.PlatformWalletReportService;
import com.servija.servija.service.PlatformWalletService;
import com.servija.servija.service.PlatformWalletTransactionService;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlatformWalletReportServiceTest {

    @Mock
    private PlatformWalletTransactionService platformWalletTransactionService;

    @Mock
    private PlatformWalletService platformWalletService;

    @InjectMocks
    private PlatformWalletReportService platformWalletReportService;

    private PlatformWallet wallet;
    private List<PlatformWalletTransactionResponse> transactions;

    @BeforeEach
    void setUp() {
        wallet = PlatformWallet.builder()
                .id(1L)
                .availableBalance(new BigDecimal("300.00"))
                .reservedBalance(new BigDecimal("120.00"))
                .totalEarned(new BigDecimal("503.01"))
                .totalWithdrawn(new BigDecimal("80.00"))
                .build();

        transactions = List.of(
                PlatformWalletTransactionResponse.builder()
                        .id(1L)
                        .type("FEE")
                        .amount(new BigDecimal("3.01"))
                        .status("COMPLETED")
                        .description("Receita liquida da plataforma liberada")
                        .paymentMethod("PIX")
                        .serviceName("Instalacao")
                        .referenceLabel("Servico #10")
                        .date(LocalDateTime.of(2026, 5, 10, 10, 0))
                        .build(),
                PlatformWalletTransactionResponse.builder()
                        .id(2L)
                        .type("WITHDRAWAL")
                        .amount(new BigDecimal("-80.00"))
                        .status("COMPLETED")
                        .description("Saque via PIX")
                        .paymentMethod("PIX")
                        .referenceLabel("PIX - CPF - **900")
                        .date(LocalDateTime.of(2026, 5, 11, 11, 0))
                        .build()
        );
    }

    @Test
    @DisplayName("deve gerar relatório Excel com resumo e transações")
    void shouldGenerateExcelReport() throws Exception {
        when(platformWalletService.getOrCreateWallet()).thenReturn(wallet);
        when(platformWalletTransactionService.getTransactions("ALL", null, null)).thenReturn(transactions);

        PlatformWalletReportService.ReportFile report =
                platformWalletReportService.generateReport(
                        PlatformWalletReportService.ReportFormat.EXCEL,
                        "ALL",
                        null,
                        null
                );

        assertThat(report.filename()).isEqualTo("relatorio-carteira-plataforma.xlsx");
        assertThat(report.contentType()).isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        assertThat(report.content()).isNotEmpty();

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(report.content()))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(2);
            assertThat(workbook.getSheet("Resumo")).isNotNull();
            assertThat(workbook.getSheetAt(0).getRow(0).getCell(1).getStringCellValue())
                    .isEqualTo("Carteira da plataforma");
            assertThat(workbook.getSheetAt(0).getRow(6).getCell(0).getStringCellValue())
                    .isEqualTo("Receita liquida acumulada");
            assertThat(workbook.getSheetAt(0).getRow(6).getCell(1).getStringCellValue())
                    .isEqualTo("R$ 503.01");
            assertThat(workbook.getSheetAt(0).getRow(9).getCell(1).getStringCellValue())
                    .isEqualTo("R$ 80.00");
            assertThat(workbook.getSheetAt(1).getRow(1).getCell(0).getStringCellValue())
                    .isEqualTo("Receita liquida");
            assertThat(workbook.getSheetAt(1).getRow(1).getCell(4).getStringCellValue())
                    .isEqualTo("R$ 3.01");
        }
    }

    @Test
    @DisplayName("deve gerar relatório PDF com header válido")
    void shouldGeneratePdfReport() {
        LocalDate dateFrom = LocalDate.of(2026, 5, 1);
        LocalDate dateTo = LocalDate.of(2026, 5, 31);

        when(platformWalletService.getOrCreateWallet()).thenReturn(wallet);
        when(platformWalletTransactionService.getTransactions("WITHDRAWAL", dateFrom, dateTo))
                .thenReturn(transactions);

        PlatformWalletReportService.ReportFile report =
                platformWalletReportService.generateReport(
                        PlatformWalletReportService.ReportFormat.PDF,
                        "WITHDRAWAL",
                        dateFrom,
                        dateTo
                );

        assertThat(report.filename()).isEqualTo("relatorio-carteira-plataforma.pdf");
        assertThat(report.contentType()).isEqualTo("application/pdf");
        assertThat(report.content()).isNotEmpty();
        assertThat(new String(report.content(), 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
    }
}
