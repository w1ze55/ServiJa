package com.servija.servija.controller;

import com.servija.servija.dto.BankAccountDTO;
import com.servija.servija.dto.BankAccountResponse;
import com.servija.servija.dto.FreelancerWalletResponse;
import com.servija.servija.dto.WalletTransactionResponse;
import com.servija.servija.dto.WithdrawalRequestDTO;
import com.servija.servija.dto.WithdrawalResponse;
import com.servija.servija.model.FreelancerWallet;
import com.servija.servija.model.WithdrawalRequest;
import com.servija.servija.service.BankAccountService;
import com.servija.servija.service.FreelancerWalletService;
import com.servija.servija.service.WalletReportService;
import com.servija.servija.service.WalletTransactionService;
import com.servija.servija.service.WithdrawalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
@Tag(name = "Carteira do Freelancer", description = "Endpoints para gerenciamento de saldo e saques")
public class FreelancerWalletController {

    private final FreelancerWalletService walletService;
    private final WithdrawalService withdrawalService;
    private final BankAccountService bankAccountService;
    private final WalletTransactionService transactionService;
    private final WalletReportService reportService;

    @GetMapping
    @Operation(summary = "Obter saldo da carteira")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<FreelancerWalletResponse> getWallet(Authentication authentication) {
        String email = authentication.getName();
        FreelancerWallet wallet = walletService.getOrCreateWallet(email);

        FreelancerWalletResponse response = FreelancerWalletResponse.builder()
                .id(wallet.getId())
                .availableBalance(wallet.getAvailableBalance())
                .pendingBalance(wallet.getPendingBalance())
                .totalEarned(wallet.getTotalEarned())
                .totalWithdrawn(wallet.getTotalWithdrawn())
                .createdAt(wallet.getCreatedAt())
                .updatedAt(wallet.getUpdatedAt())
                .build();

        return ResponseEntity.ok(response);
    }

    @GetMapping("/transactions")
    @Operation(summary = "Listar transacoes", description = "Lista historico de recebimentos e saques")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<WalletTransactionResponse>> getTransactions(
            Authentication authentication,
            @RequestParam(required = false, defaultValue = "ALL") String type
    ) {
        String email = authentication.getName();
        List<WalletTransactionResponse> transactions = transactionService.getTransactions(email, type);
        return ResponseEntity.ok(transactions);
    }

    @PostMapping("/withdraw")
    @Operation(summary = "Solicitar saque")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<WithdrawalResponse> requestWithdrawal(
            @Valid @RequestBody WithdrawalRequestDTO request,
            Authentication authentication
    ) {
        String email = authentication.getName();

        WithdrawalRequest withdrawal = withdrawalService.requestWithdrawal(
                email,
                request.getAmount(),
                request.getWithdrawalMethod(),
                request.getPixKey(),
                request.getPixKeyType(),
                request.getBankAccountId()
        );

        WithdrawalResponse response = mapToResponse(withdrawal);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/withdrawals")
    @Operation(summary = "Listar saques")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<WithdrawalResponse>> getMyWithdrawals(Authentication authentication) {
        String email = authentication.getName();
        List<WithdrawalRequest> withdrawals = withdrawalService.getMyWithdrawals(email);

        List<WithdrawalResponse> response = withdrawals.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/withdrawals/{id}")
    @Operation(summary = "Cancelar saque")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> cancelWithdrawal(
            @PathVariable Long id,
            Authentication authentication
    ) {
        String email = authentication.getName();
        withdrawalService.cancelWithdrawal(id, email);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/bank-accounts")
    @Operation(summary = "Listar contas bancarias")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<BankAccountResponse>> getBankAccounts(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(bankAccountService.getAccountsByEmail(email));
    }

    @PostMapping("/bank-accounts")
    @Operation(summary = "Cadastrar conta bancaria ou chave PIX")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<BankAccountResponse> addBankAccount(
            @Valid @RequestBody BankAccountDTO request,
            Authentication authentication
    ) {
        String email = authentication.getName();
        BankAccountResponse response = bankAccountService.createAccount(email, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/bank-accounts/{id}")
    @Operation(summary = "Remover conta bancaria")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> deleteBankAccount(
            @PathVariable Long id,
            Authentication authentication
    ) {
        String email = authentication.getName();
        bankAccountService.deleteAccount(email, id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/bank-accounts/{id}/default")
    @Operation(summary = "Definir conta padrao")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<BankAccountResponse> setDefaultAccount(
            @PathVariable Long id,
            Authentication authentication
    ) {
        String email = authentication.getName();
        return ResponseEntity.ok(bankAccountService.setDefault(email, id));
    }

    @GetMapping("/report")
    @Operation(summary = "Baixar relatorio da carteira em PDF ou Excel")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<byte[]> downloadReport(
            Authentication authentication,
            @RequestParam(defaultValue = "EXCEL") WalletReportService.ReportFormat format
    ) {
        String email = authentication.getName();
        WalletReportService.ReportFile report = reportService.generateReport(email, format);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(report.contentType()));
        headers.setContentDispositionFormData("attachment", report.filename());

        return new ResponseEntity<>(report.content(), headers, HttpStatus.OK);
    }

    private WithdrawalResponse mapToResponse(WithdrawalRequest withdrawal) {
        return WithdrawalResponse.builder()
                .id(withdrawal.getId())
                .amount(withdrawal.getAmount())
                .status(withdrawal.getStatus().name())
                .withdrawalMethod(withdrawal.getWithdrawalMethod() != null
                        ? withdrawal.getWithdrawalMethod().name()
                        : null)
                .bankAccountId(withdrawal.getBankAccount() != null
                        ? withdrawal.getBankAccount().getId()
                        : null)
                .pixKey(withdrawal.getPixKey())
                .pixKeyType(withdrawal.getPixKeyType())
                .transactionId(withdrawal.getTransactionId())
                .rejectionReason(withdrawal.getRejectionReason())
                .processedAt(withdrawal.getProcessedAt())
                .createdAt(withdrawal.getCreatedAt())
                .build();
    }
}
