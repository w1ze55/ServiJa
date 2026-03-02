package com.servija.servija.controller;

import com.servija.servija.dto.FreelancerWalletResponse;
import com.servija.servija.dto.WithdrawalRequestDTO;
import com.servija.servija.dto.WithdrawalResponse;
import com.servija.servija.model.FreelancerWallet;
import com.servija.servija.model.WithdrawalRequest;
import com.servija.servija.service.FreelancerWalletService;
import com.servija.servija.service.WithdrawalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
@Tag(name = "Carteira do Freelancer", description = "Endpoints para gerenciamento de saldo e saques")
public class FreelancerWalletController {

    private final FreelancerWalletService walletService;
    private final WithdrawalService withdrawalService;

    @GetMapping
    @Operation(summary = "Obter saldo da carteira", description = "Retorna o saldo disponível, pendente e histórico do freelancer")
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

    @PostMapping("/withdraw")
    @Operation(summary = "Solicitar saque", description = "Solicita saque do saldo disponível via PIX")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<WithdrawalResponse> requestWithdrawal(
            @Valid @RequestBody WithdrawalRequestDTO request,
            Authentication authentication
    ) {
        String email = authentication.getName();
        
        WithdrawalRequest withdrawal = withdrawalService.requestWithdrawal(
            email,
            request.getAmount(),
            request.getPixKey(),
            request.getPixKeyType()
        );

        WithdrawalResponse response = mapToResponse(withdrawal);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/withdrawals")
    @Operation(summary = "Listar saques", description = "Lista todas as solicitações de saque do freelancer")
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
    @Operation(summary = "Cancelar saque", description = "Cancela uma solicitação de saque pendente")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<Void> cancelWithdrawal(
            @PathVariable Long id,
            Authentication authentication
    ) {
        String email = authentication.getName();
        withdrawalService.cancelWithdrawal(id, email);
        return ResponseEntity.noContent().build();
    }

    private WithdrawalResponse mapToResponse(WithdrawalRequest withdrawal) {
        return WithdrawalResponse.builder()
                .id(withdrawal.getId())
                .amount(withdrawal.getAmount())
                .status(withdrawal.getStatus().name())
                .pixKey(withdrawal.getPixKey())
                .pixKeyType(withdrawal.getPixKeyType())
                .transactionId(withdrawal.getTransactionId())
                .rejectionReason(withdrawal.getRejectionReason())
                .processedAt(withdrawal.getProcessedAt())
                .createdAt(withdrawal.getCreatedAt())
                .build();
    }
}
