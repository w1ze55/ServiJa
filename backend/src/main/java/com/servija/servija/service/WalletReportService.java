package com.servija.servija.service;

import com.servija.servija.dto.WalletTransactionResponse;
import com.servija.servija.exception.ResourceNotFoundException;
import com.servija.servija.model.User;
import com.servija.servija.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WalletReportService {

    private final WalletTransactionService transactionService;
    private final FreelancerWalletService walletService;
    private final UserRepository userRepository;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    /**
     * Gera relatório CSV com as transações da carteira.
     * Escolhi CSV ao invés de PDF para evitar dependência extra (OpenPDF/iText).
     * CSV abre nativamente no Excel e é mais leve.
     */
    public byte[] generateReport(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        List<WalletTransactionResponse> transactions = transactionService.getTransactions(email, "ALL");
        var wallet = walletService.getWalletBalance(email);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            StringBuilder sb = new StringBuilder();

            // BOM para Excel reconhecer UTF-8
            baos.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});

            // Cabeçalho do relatório
            sb.append("RELATÓRIO DA CARTEIRA - SERVIJÁ\n");
            sb.append("Freelancer:,").append(user.getName()).append("\n");
            sb.append("Email:,").append(email).append("\n");
            sb.append("Data do relatório:,").append(LocalDateTime.now().format(DATE_FMT)).append("\n");
            sb.append("\n");

            // Resumo
            sb.append("RESUMO\n");
            sb.append("Saldo Total:,").append(formatBRL(wallet.get("totalEarned"))).append("\n");
            sb.append("Disponível para Saque:,").append(formatBRL(wallet.get("availableBalance"))).append("\n");
            sb.append("Pendente:,").append(formatBRL(wallet.get("pendingBalance"))).append("\n");
            sb.append("Total Sacado:,").append(formatBRL(wallet.get("totalWithdrawn"))).append("\n");
            sb.append("\n");

            // Transações
            sb.append("TRANSAÇÕES\n");
            sb.append("Tipo,Descrição,Valor,Status,Forma de Pgto,Data\n");

            for (WalletTransactionResponse tx : transactions) {
                sb.append(tx.getType().equals("EARNING") ? "Recebimento" : "Saque").append(",");
                sb.append(escapeCsv(tx.getDescription())).append(",");
                sb.append(formatBRL(tx.getAmount())).append(",");
                sb.append(translateStatus(tx.getStatus())).append(",");
                sb.append(tx.getPaymentMethod() != null ? tx.getPaymentMethod() : "-").append(",");
                sb.append(tx.getDate() != null ? tx.getDate().format(DATE_FMT) : "-").append("\n");
            }

            baos.write(sb.toString().getBytes("UTF-8"));
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("Erro ao gerar relatório", e);
            throw new RuntimeException("Erro ao gerar relatório: " + e.getMessage());
        }
    }

    private String formatBRL(Object value) {
        if (value == null) return "R$ 0,00";
        BigDecimal bd = value instanceof BigDecimal ? (BigDecimal) value : new BigDecimal(value.toString());
        return String.format("R$ %,.2f", bd).replace(",", "X").replace(".", ",").replace("X", ".");
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
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
