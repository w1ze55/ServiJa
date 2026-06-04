package com.servija.servija.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.servija.servija.dto.PlatformWalletTransactionResponse;
import com.servija.servija.model.PlatformWallet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlatformWalletReportService {

    public enum ReportFormat {
        PDF,
        EXCEL
    }

    public record ReportFile(byte[] content, String contentType, String filename) {
    }

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DecimalFormatSymbols PT_BR_DECIMAL_SYMBOLS = DecimalFormatSymbols.getInstance(Locale.forLanguageTag("pt-BR"));

    private final PlatformWalletTransactionService transactionService;
    private final PlatformWalletService platformWalletService;

    public ReportFile generateReport(
            ReportFormat format,
            String type,
            LocalDate dateFrom,
            LocalDate dateTo
    ) {
        PlatformWallet wallet = platformWalletService.getOrCreateWallet();
        List<PlatformWalletTransactionResponse> transactions =
                transactionService.getTransactions(type, dateFrom, dateTo);

        return switch (format) {
            case PDF -> new ReportFile(
                    generatePdfReport(wallet, transactions, type, dateFrom, dateTo),
                    "application/pdf",
                    "relatorio-carteira-plataforma.pdf"
            );
            case EXCEL -> new ReportFile(
                    generateExcelReport(wallet, transactions, type, dateFrom, dateTo),
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "relatorio-carteira-plataforma.xlsx"
            );
        };
    }

    private byte[] generateExcelReport(
            PlatformWallet wallet,
            List<PlatformWalletTransactionResponse> transactions,
            String type,
            LocalDate dateFrom,
            LocalDate dateTo
    ) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Sheet summarySheet = workbook.createSheet("Resumo");
            Sheet transactionsSheet = workbook.createSheet("Transações");

            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.LEFT);

            int summaryRowIndex = 0;
            summaryRowIndex = createKeyValueRow(summarySheet, summaryRowIndex, "Relatório", "Carteira da plataforma", headerStyle);
            summaryRowIndex = createKeyValueRow(summarySheet, summaryRowIndex, "Escopo", "Servi Já", null);
            summaryRowIndex = createKeyValueRow(summarySheet, summaryRowIndex, "Data do relatório", LocalDateTime.now().format(DATE_FMT), null);
            summaryRowIndex = createKeyValueRow(summarySheet, summaryRowIndex, "Tipo de transação", normalizeTypeLabel(type), null);
            summaryRowIndex = createKeyValueRow(summarySheet, summaryRowIndex, "Período", buildPeriodLabel(dateFrom, dateTo), null);
            summaryRowIndex++;
            summaryRowIndex = createKeyValueRow(summarySheet, summaryRowIndex, "Receita liquida acumulada", formatBRL(wallet.getTotalEarned()), headerStyle);
            summaryRowIndex = createKeyValueRow(summarySheet, summaryRowIndex, "Disponível para saque", formatBRL(wallet.getAvailableBalance()), null);
            summaryRowIndex = createKeyValueRow(summarySheet, summaryRowIndex, "Em processamento", formatBRL(wallet.getReservedBalance()), null);
            createKeyValueRow(summarySheet, summaryRowIndex, "Total sacado", formatBRL(wallet.getTotalWithdrawn()), null);

            Row headerRow = transactionsSheet.createRow(0);
            String[] headers = {"Tipo", "Descrição", "Referência", "Serviço", "Valor", "Status", "Método", "Data"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 1;
            for (PlatformWalletTransactionResponse tx : transactions) {
                Row row = transactionsSheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(translateTransactionType(tx.getType()));
                row.createCell(1).setCellValue(tx.getDescription() != null ? tx.getDescription() : "-");
                row.createCell(2).setCellValue(tx.getReferenceLabel() != null ? tx.getReferenceLabel() : "-");
                row.createCell(3).setCellValue(tx.getServiceName() != null ? tx.getServiceName() : "-");
                row.createCell(4).setCellValue(formatBRL(tx.getAmount()));
                row.createCell(5).setCellValue(translateStatus(tx.getStatus()));
                row.createCell(6).setCellValue(tx.getPaymentMethod() != null ? tx.getPaymentMethod() : "-");
                row.createCell(7).setCellValue(tx.getDate() != null ? tx.getDate().format(DATE_FMT) : "-");
            }

            for (int i = 0; i < 2; i++) {
                summarySheet.autoSizeColumn(i);
            }
            for (int i = 0; i < headers.length; i++) {
                transactionsSheet.autoSizeColumn(i);
            }

            workbook.write(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("Erro ao gerar relatório da carteira da plataforma em Excel", e);
            throw new RuntimeException("Erro ao gerar relatório: " + e.getMessage());
        }
    }

    private byte[] generatePdfReport(
            PlatformWallet wallet,
            List<PlatformWalletTransactionResponse> transactions,
            String type,
            LocalDate dateFrom,
            LocalDate dateTo
    ) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate(), 24, 24, 24, 24);
            PdfWriter.getInstance(document, baos);
            document.open();

            document.add(new Paragraph("Relatório da Carteira da Plataforma", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16)));
            document.add(new Paragraph("Escopo: Servi Já"));
            document.add(new Paragraph("Data do relatório: " + LocalDateTime.now().format(DATE_FMT)));
            document.add(new Paragraph("Tipo de transação: " + normalizeTypeLabel(type)));
            document.add(new Paragraph("Período: " + buildPeriodLabel(dateFrom, dateTo)));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Resumo", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
            document.add(new Paragraph("Receita liquida acumulada: " + formatBRL(wallet.getTotalEarned())));
            document.add(new Paragraph("Disponível para saque: " + formatBRL(wallet.getAvailableBalance())));
            document.add(new Paragraph("Em processamento: " + formatBRL(wallet.getReservedBalance())));
            document.add(new Paragraph("Total sacado: " + formatBRL(wallet.getTotalWithdrawn())));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(new float[]{1.4f, 3.2f, 3f, 2.4f, 1.8f, 1.8f, 1.8f, 2.2f});
            table.setWidthPercentage(100);
            addPdfHeaderCell(table, "Tipo");
            addPdfHeaderCell(table, "Descrição");
            addPdfHeaderCell(table, "Referência");
            addPdfHeaderCell(table, "Serviço");
            addPdfHeaderCell(table, "Valor");
            addPdfHeaderCell(table, "Status");
            addPdfHeaderCell(table, "Método");
            addPdfHeaderCell(table, "Data");

            for (PlatformWalletTransactionResponse tx : transactions) {
                table.addCell(createPdfCell(translateTransactionType(tx.getType())));
                table.addCell(createPdfCell(tx.getDescription() != null ? tx.getDescription() : "-"));
                table.addCell(createPdfCell(tx.getReferenceLabel() != null ? tx.getReferenceLabel() : "-"));
                table.addCell(createPdfCell(tx.getServiceName() != null ? tx.getServiceName() : "-"));
                table.addCell(createPdfCell(formatBRL(tx.getAmount())));
                table.addCell(createPdfCell(translateStatus(tx.getStatus())));
                table.addCell(createPdfCell(tx.getPaymentMethod() != null ? tx.getPaymentMethod() : "-"));
                table.addCell(createPdfCell(tx.getDate() != null ? tx.getDate().format(DATE_FMT) : "-"));
            }

            document.add(table);
            document.close();
            return baos.toByteArray();
        } catch (DocumentException | IOException e) {
            log.error("Erro ao gerar relatório da carteira da plataforma em PDF", e);
            throw new RuntimeException("Erro ao gerar relatório: " + e.getMessage());
        }
    }

    private int createKeyValueRow(Sheet sheet, int rowIndex, String label, String value, CellStyle labelStyle) {
        Row row = sheet.createRow(rowIndex);
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        if (labelStyle != null) {
            labelCell.setCellStyle(labelStyle);
        }
        row.createCell(1).setCellValue(value);
        return rowIndex + 1;
    }

    private void addPdfHeaderCell(PdfPTable table, String value) {
        PdfPCell cell = new PdfPCell(new Phrase(value, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10)));
        table.addCell(cell);
    }

    private PdfPCell createPdfCell(String value) {
        return new PdfPCell(new Phrase(value, FontFactory.getFont(FontFactory.HELVETICA, 9)));
    }

    private String formatBRL(BigDecimal value) {
        BigDecimal safeValue = value == null ? BigDecimal.ZERO : value;
        DecimalFormat formatter = new DecimalFormat("R$ #,##0.00", PT_BR_DECIMAL_SYMBOLS);
        formatter.setParseBigDecimal(true);
        return formatter.format(safeValue);
    }

    private String translateTransactionType(String type) {
        if (type == null) return "-";
        return "FEE".equals(type) ? "Receita liquida" : "Saque";
    }

    private String translateStatus(String status) {
        if (status == null) return "-";
        return switch (status) {
            case "COMPLETED" -> "Concluído";
            case "PENDING" -> "Pendente";
            case "PROCESSING" -> "Processando";
            case "FAILED" -> "Falhou";
            case "CANCELLED" -> "Cancelado";
            default -> status;
        };
    }

    private String normalizeTypeLabel(String type) {
        if (type == null || type.isBlank() || "ALL".equalsIgnoreCase(type)) {
            return "Todos";
        }
        return "FEE".equalsIgnoreCase(type) ? "Receitas liquidas" : "Saques";
    }

    private String buildPeriodLabel(LocalDate dateFrom, LocalDate dateTo) {
        if (dateFrom == null && dateTo == null) {
            return "Todo o período";
        }

        LocalDate start = dateFrom;
        LocalDate end = dateTo;
        if (start == null) {
            start = end;
        }
        if (end == null) {
            end = start;
        }
        if (start != null && end != null && start.isAfter(end)) {
            LocalDate temp = start;
            start = end;
            end = temp;
        }

        if (start != null && start.equals(end)) {
            return start.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        }

        return (start != null ? start.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "-")
                + " até "
                + (end != null ? end.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "-");
    }
}
