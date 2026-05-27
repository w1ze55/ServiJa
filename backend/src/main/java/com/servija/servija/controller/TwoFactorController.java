package com.servija.servija.controller;

import com.servija.servija.dto.MessageResponse;
import com.servija.servija.dto.TwoFactorBackupCodesResponse;
import com.servija.servija.dto.TwoFactorDisableRequest;
import com.servija.servija.dto.TwoFactorEnableRequest;
import com.servija.servija.dto.TwoFactorSetupResponse;
import com.servija.servija.dto.TwoFactorStatusResponse;
import com.servija.servija.exception.InvalidCredentialsException;
import com.servija.servija.exception.ResourceNotFoundException;
import com.servija.servija.model.AuditLog;
import com.servija.servija.model.User;
import com.servija.servija.repository.UserRepository;
import com.servija.servija.repository.UserTwoFactorBackupCodeRepository;
import com.servija.servija.service.AuditLogger;
import com.servija.servija.service.TotpService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users/me/2fa")
@RequiredArgsConstructor
@Tag(name = "Autenticacao 2FA", description = "Endpoints para gerenciar o segundo fator de autenticacao (TOTP)")
@SecurityRequirement(name = "bearerAuth")
public class TwoFactorController {

    private final UserRepository userRepository;
    private final UserTwoFactorBackupCodeRepository backupCodeRepository;
    private final TotpService totpService;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogger auditLogger;

    @GetMapping("/status")
    @Operation(summary = "Status do 2FA do usuario logado")
    public ResponseEntity<TwoFactorStatusResponse> status() {
        User user = currentUser();
        int unused = backupCodeRepository.findByUserAndUsedAtIsNull(user).size();
        return ResponseEntity.ok(TwoFactorStatusResponse.builder()
                .enabled(Boolean.TRUE.equals(user.getTotpEnabled()))
                .enabledAt(user.getTotpEnabledAt())
                .unusedBackupCodes(unused)
                .build());
    }

    @PostMapping("/setup")
    @Operation(summary = "Inicia o setup do 2FA", description = "Gera um segredo pendente e retorna QR code. Nao ativa ate /enable.")
    public ResponseEntity<TwoFactorSetupResponse> setup() {
        User user = currentUser();
        if (Boolean.TRUE.equals(user.getTotpEnabled())) {
            throw new IllegalStateException("2FA ja esta ativado");
        }
        TotpService.SetupResult result = totpService.beginSetup(user);
        auditLogger.recordFor(
                user,
                "2FA_SETUP_INITIATED",
                "AUTH",
                null,
                user.getEmail(),
                AuditLog.Result.SUCCESS,
                Map.of());
        return ResponseEntity.ok(TwoFactorSetupResponse.builder()
                .secret(result.secret())
                .qrCodeDataUri(result.qrCodeDataUri())
                .build());
    }

    @PostMapping("/enable")
    @Operation(summary = "Confirma o codigo TOTP e ativa o 2FA", description = "Retorna os backup codes uma unica vez.")
    public ResponseEntity<TwoFactorBackupCodesResponse> enable(@Valid @RequestBody TwoFactorEnableRequest request) {
        User user = currentUser();
        List<String> backupCodes = totpService.confirmSetup(user, request.getCode());
        auditLogger.recordFor(
                user,
                "2FA_ENABLED",
                "AUTH",
                null,
                user.getEmail(),
                AuditLog.Result.SUCCESS,
                Map.of());
        return ResponseEntity.ok(TwoFactorBackupCodesResponse.builder()
                .backupCodes(backupCodes)
                .build());
    }

    @PostMapping("/disable")
    @Operation(summary = "Desativa o 2FA", description = "Requer senha + codigo TOTP/backup atual.")
    public ResponseEntity<MessageResponse> disable(@Valid @RequestBody TwoFactorDisableRequest request) {
        User user = currentUser();
        if (!Boolean.TRUE.equals(user.getTotpEnabled())) {
            throw new IllegalStateException("2FA nao esta ativado");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Senha invalida");
        }
        TotpService.VerifyResult result = totpService.verifyChallenge(user, request.getCode());
        if (result == TotpService.VerifyResult.INVALID || result == TotpService.VerifyResult.LOCKED) {
            throw new InvalidCredentialsException("Codigo invalido");
        }
        totpService.disable(user);
        auditLogger.recordFor(
                user,
                "2FA_DISABLED",
                "AUTH",
                null,
                user.getEmail(),
                AuditLog.Result.SUCCESS,
                Map.of("reason", "user_request"));
        return ResponseEntity.ok(new MessageResponse("2FA desativado com sucesso"));
    }

    @PostMapping("/backup-codes/regenerate")
    @Operation(summary = "Gera novos backup codes", description = "Invalida os antigos. Requer senha + codigo TOTP/backup atual.")
    public ResponseEntity<TwoFactorBackupCodesResponse> regenerate(@Valid @RequestBody TwoFactorDisableRequest request) {
        User user = currentUser();
        if (!Boolean.TRUE.equals(user.getTotpEnabled())) {
            throw new IllegalStateException("2FA nao esta ativado");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new InvalidCredentialsException("Senha invalida");
        }
        TotpService.VerifyResult result = totpService.verifyChallenge(user, request.getCode());
        if (result == TotpService.VerifyResult.INVALID || result == TotpService.VerifyResult.LOCKED) {
            throw new InvalidCredentialsException("Codigo invalido");
        }
        List<String> codes = totpService.rotateBackupCodes(user);
        auditLogger.recordFor(
                user,
                "2FA_BACKUP_REGENERATED",
                "AUTH",
                null,
                user.getEmail(),
                AuditLog.Result.SUCCESS,
                Map.of());
        return ResponseEntity.ok(TwoFactorBackupCodesResponse.builder()
                .backupCodes(codes)
                .build());
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        String email = auth.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario nao encontrado"));
    }
}
