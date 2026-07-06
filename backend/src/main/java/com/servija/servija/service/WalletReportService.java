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
import com.servija.servija.dto.WalletTransactionResponse;
import com.servija.servija.exception.ResourceNotFoundException;
import com.servija.servija.model.User;
import com.servija.servija.repository.UserRepository;
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
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletReportService {

    public enum ReportFormat {
        PDF,
        EXCEL
    }

    public record ReportFile(byte[] content, String contentType, String filename) {
    }

    private final WalletTransactionService transactionService;
    private final FreelancerWalletService walletService;
    private final UserRepository userRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DecimalFormatSymbols PT_BR_DECIMAL_SYMBOLS =
            DecimalFormatSymbols.getInstance(Locale.forLanguageTag("pt-BR"));

    public ReportFile generateReport(String email, ReportFormat format) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        List<WalletTransactionResponse> transactions = transactionService.getTransactions(email, "ALL");
        Map<String, BigDecimal> wallet = walletService.getWalletBalance(email);

        return switch (format) {
            case PDF -> new ReportFile(
                    generatePdfReport(user, email, transactions, wallet),
                    "application/pdf",
                    "relatorio-carteira.pdf"
            );
            case EXCEL -> new ReportFile(
                    generateExcelReport(user, email, transactions, wallet),
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    "relatorio-carteira.xlsx"
            );
        };
    }

    private byte[] generateExcelReport(
            User user,
            String email,
            List<WalletTransactionResponse> transactions,
            Map<String, BigDecimal> wallet
    ) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Sheet resumoSheet = workbook.createSheet("Resumo");
            Sheet transacoesSheet = workbook.createSheet("Transações");

            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.LEFT);

            int resumoRowIndex = 0;
            resumoRowIndex = createKeyValueRow(resumoSheet, resumoRowIndex, "Relatório da carteira", "Servija", headerStyle);
            resumoRowIndex = createKeyValueRow(resumoSheet, resumoRowIndex, "Freelancer", user.getName(), null);
            resumoRowIndex = createKeyValueRow(resumoSheet, resumoRowIndex, "Email", email, null);
            resumoRowIndex = createKeyValueRow(resumoSheet, resumoRowIndex, "Data do relatório", LocalDateTime.now().format(DATE_FMT), null);
            resumoRowIndex++;
            resumoRowIndex = createKeyValueRow(resumoSheet, resumoRowIndex, "Saldo total", formatBRL(wallet.get("totalEarned")), headerStyle);
            resumoRowIndex = createKeyValueRow(resumoSheet, resumoRowIndex, "Disponível para saque", formatBRL(wallet.get("availableBalance")), null);
            resumoRowIndex = createKeyValueRow(resumoSheet, resumoRowIndex, "Pendente", formatBRL(wallet.get("pendingBalance")), null);
            createKeyValueRow(resumoSheet, resumoRowIndex, "Total sacado", formatBRL(wallet.get("totalWithdrawn")), null);

            Row headerRow = transacoesSheet.createRow(0);
            String[] headers = {"Tipo", "Descrição", "Valor", "Status", "Forma de pagamento", "Data"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIndex = 1;
            for (WalletTransactionResponse tx : transactions) {
                Row row = transacoesSheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(translateTransactionType(tx.getType()));
                row.createCell(1).setCellValue(tx.getDescription() != null ? tx.getDescription() : "-");
                row.createCell(2).setCellValue(formatBRL(tx.getAmount()));
                row.createCell(3).setCellValue(translateStatus(tx.getStatus()));
                row.createCell(4).setCellValue(tx.getPaymentMethod() != null ? tx.getPaymentMethod() : "-");
                row.createCell(5).setCellValue(tx.getDate() != null ? tx.getDate().format(DATE_FMT) : "-");
            }

            for (int i = 0; i < 2; i++) {
                resumoSheet.autoSizeColumn(i);
            }
            for (int i = 0; i < headers.length; i++) {
                transacoesSheet.autoSizeColumn(i);
            }

            workbook.write(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("Erro ao gerar relatório em Excel", e);
            throw new RuntimeException("Erro ao gerar relatório: " + e.getMessage());
        }
    }

    private byte[] generatePdfReport(
            User user,
            String email,
            List<WalletTransactionResponse> transactions,
            Map<String, BigDecimal> wallet
    ) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            Document document = new Document(PageSize.A4.rotate(), 24, 24, 24, 24);
            PdfWriter.getInstance(document, baos);
            document.open();

            document.add(new Paragraph("Relatório da Carteira - Servija", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16)));
            document.add(new Paragraph("Freelancer: " + user.getName()));
            document.add(new Paragraph("Email: " + email));
            document.add(new Paragraph("Data do relatório: " + LocalDateTime.now().format(DATE_FMT)));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Resumo", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12)));
            document.add(new Paragraph("Saldo total: " + formatBRL(wallet.get("totalEarned"))));
            document.add(new Paragraph("Disponível para saque: " + formatBRL(wallet.get("availableBalance"))));
            document.add(new Paragraph("Pendente: " + formatBRL(wallet.get("pendingBalance"))));
            document.add(new Paragraph("Total sacado: " + formatBRL(wallet.get("totalWithdrawn"))));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(new float[]{1.5f, 4f, 2f, 2f, 2f, 2.5f});
            table.setWidthPercentage(100);
            addPdfHeaderCell(table, "Tipo");
            addPdfHeaderCell(table, "Descrição");
            addPdfHeaderCell(table, "Valor");
            addPdfHeaderCell(table, "Status");
            addPdfHeaderCell(table, "Forma de pagamento");
            addPdfHeaderCell(table, "Data");

            for (WalletTransactionResponse tx : transactions) {
                table.addCell(createPdfCell(translateTransactionType(tx.getType())));
                table.addCell(createPdfCell(tx.getDescription() != null ? tx.getDescription() : "-"));
                table.addCell(createPdfCell(formatBRL(tx.getAmount())));
                table.addCell(createPdfCell(translateStatus(tx.getStatus())));
                table.addCell(createPdfCell(tx.getPaymentMethod() != null ? tx.getPaymentMethod() : "-"));
                table.addCell(createPdfCell(tx.getDate() != null ? tx.getDate().format(DATE_FMT) : "-"));
            }

            document.add(table);
            document.close();
            return baos.toByteArray();
        } catch (DocumentException | IOException e) {
            log.error("Erro ao gerar relatório em PDF", e);
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

    private String formatBRL(Object value) {
        if (value == null) return "R$ 0,00";
        BigDecimal bd = value instanceof BigDecimal ? (BigDecimal) value : new BigDecimal(value.toString());
        DecimalFormat formatter = new DecimalFormat("R$ #,##0.00", PT_BR_DECIMAL_SYMBOLS);
        formatter.setParseBigDecimal(true);
        return formatter.format(bd);
    }

    private String translateTransactionType(String type) {
        if (type == null) return "-";
        return "EARNING".equals(type) ? "Recebimento" : "Saque";
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
}
