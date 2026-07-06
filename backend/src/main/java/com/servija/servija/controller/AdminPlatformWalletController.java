package com.servija.servija.controller;

import com.servija.servija.dto.BankAccountDTO;
import com.servija.servija.dto.BankAccountResponse;
import com.servija.servija.dto.PlatformWalletResponse;
import com.servija.servija.dto.PlatformWalletTransactionPageResponse;
import com.servija.servija.dto.PlatformWalletTransactionResponse;
import com.servija.servija.dto.PlatformWithdrawalRequestDTO;
import com.servija.servija.dto.PlatformWithdrawalResponse;
import com.servija.servija.model.PlatformWallet;
import com.servija.servija.model.PlatformWithdrawalRequest;
import com.servija.servija.service.PlatformBankAccountService;
import com.servija.servija.service.PlatformWalletReportService;
import com.servija.servija.service.PlatformWalletService;
import com.servija.servija.service.PlatformWalletTransactionService;
import com.servija.servija.service.PlatformWithdrawalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/admin/platform-wallet")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Carteira da Plataforma", description = "Endpoints da carteira institucional da plataforma")
public class AdminPlatformWalletController {

    private final PlatformWalletService platformWalletService;
    private final PlatformWalletTransactionService platformWalletTransactionService;
    private final PlatformBankAccountService platformBankAccountService;
    private final PlatformWithdrawalService platformWithdrawalService;
    private final PlatformWalletReportService platformWalletReportService;

    @GetMapping
    @Operation(summary = "Obter saldo da carteira da plataforma")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<PlatformWalletResponse> getWallet() {
        PlatformWallet wallet = platformWalletService.getOrCreateWallet();
        return ResponseEntity.ok(PlatformWalletResponse.builder()
                .id(wallet.getId())
                .availableBalance(wallet.getAvailableBalance())
                .reservedBalance(wallet.getReservedBalance())
                .totalEarned(wallet.getTotalEarned())
                .totalWithdrawn(wallet.getTotalWithdrawn())
                .createdAt(wallet.getCreatedAt())
                .updatedAt(wallet.getUpdatedAt())
                .build());
    }

    @GetMapping("/transactions")
    @Operation(summary = "Listar transações da carteira da plataforma")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<PlatformWalletTransactionResponse>> getTransactions(
            @RequestParam(required = false, defaultValue = "ALL") String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    ) {
        return ResponseEntity.ok(platformWalletTransactionService.getTransactions(type, dateFrom, dateTo));
    }

    @GetMapping("/transactions/paged")
    @Operation(summary = "Listar transações paginadas da carteira da plataforma")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<PlatformWalletTransactionPageResponse> getTransactionsPaged(
            @RequestParam(required = false, defaultValue = "ALL") String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(
                platformWalletTransactionService.getTransactionsPaged(type, dateFrom, dateTo, page, size));
    }

    @GetMapping("/bank-accounts")
    @Operation(summary = "Listar contas globais da plataforma")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<BankAccountResponse>> getBankAccounts() {
        return ResponseEntity.ok(platformBankAccountService.getAccounts());
    }

    @PostMapping("/bank-accounts")
    @Operation(summary = "Cadastrar conta global da plataforma")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<BankAccountResponse> addBankAccount(@Valid @RequestBody BankAccountDTO request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(platformBankAccountService.createAccount(request));
    }

    @PutMapping("/bank-accounts/{id}/default")
    @Operation(summary = "Definir conta global padrão")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<BankAccountResponse> setDefaultAccount(@PathVariable Long id) {
        return ResponseEntity.ok(platformBankAccountService.setDefault(id));
    }

    @DeleteMapping("/bank-accounts/{id}")
    @Operation(summary = "Remover conta global da plataforma")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> deleteBankAccount(@PathVariable Long id) {
        platformBankAccountService.deleteAccount(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/withdrawals")
    @Operation(summary = "Solicitar saque da carteira da plataforma")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<PlatformWithdrawalResponse> requestWithdrawal(
            @Valid @RequestBody PlatformWithdrawalRequestDTO request
    ) {
        PlatformWithdrawalRequest withdrawal =
                platformWithdrawalService.requestWithdrawal(request.getAmount(), request.getBankAccountId());
        return ResponseEntity.status(HttpStatus.CREATED).body(mapWithdrawalResponse(withdrawal));
    }

    @GetMapping("/withdrawals")
    @Operation(summary = "Listar saques da carteira da plataforma")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<List<PlatformWithdrawalResponse>> getWithdrawals() {
        return ResponseEntity.ok(platformWithdrawalService.getWithdrawals().stream()
                .map(this::mapWithdrawalResponse)
                .toList());
    }

    @GetMapping("/report")
    @Operation(summary = "Baixar relatório da carteira da plataforma")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<byte[]> downloadReport(
            @RequestParam(defaultValue = "EXCEL") PlatformWalletReportService.ReportFormat format,
            @RequestParam(required = false, defaultValue = "ALL") String type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo
    ) {
        PlatformWalletReportService.ReportFile report =
                platformWalletReportService.generateReport(format, type, dateFrom, dateTo);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(report.contentType()));
        headers.setContentDispositionFormData("attachment", report.filename());

        return new ResponseEntity<>(report.content(), headers, HttpStatus.OK);
    }

    private PlatformWithdrawalResponse mapWithdrawalResponse(PlatformWithdrawalRequest withdrawal) {
        return PlatformWithdrawalResponse.builder()
                .id(withdrawal.getId())
                .amount(withdrawal.getAmount())
                .status(withdrawal.getStatus().name())
                .withdrawalMethod(withdrawal.getWithdrawalMethod().name())
                .bankAccountId(withdrawal.getBankAccountId())
                .destinationLabel(withdrawal.getDestinationLabel())
                .transactionId(withdrawal.getTransactionId())
                .rejectionReason(withdrawal.getRejectionReason())
                .processedAt(withdrawal.getProcessedAt())
                .createdAt(withdrawal.getCreatedAt())
                .build();
    }
}
