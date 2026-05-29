package com.servija.servija.service;

import com.servija.servija.dto.BankAccountDTO;
import com.servija.servija.dto.BankAccountResponse;
import com.servija.servija.exception.ResourceNotFoundException;
import com.servija.servija.model.PlatformBankAccount;
import com.servija.servija.repository.PlatformBankAccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PlatformBankAccountService {

    private static final int MAX_ACCOUNTS = 5;

    private final PlatformBankAccountRepository platformBankAccountRepository;
    private final AuditLogger auditLogger;

    @Transactional(readOnly = true)
    public List<BankAccountResponse> getAccounts() {
        return platformBankAccountRepository.findByOrderByIsDefaultDescCreatedAtDesc()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public BankAccountResponse createAccount(BankAccountDTO dto) {
        if (platformBankAccountRepository.countBy() >= MAX_ACCOUNTS) {
            throw new IllegalStateException("Limite de " + MAX_ACCOUNTS + " contas atingido");
        }

        PlatformBankAccount.AccountType accountType =
                PlatformBankAccount.AccountType.valueOf(dto.getAccountType());

        if (accountType == PlatformBankAccount.AccountType.PIX_KEY) {
            if (dto.getPixKey() == null || dto.getPixKey().isBlank()) {
                throw new IllegalArgumentException("A chave PIX é obrigatória");
            }
        } else if (dto.getBankCode() == null || dto.getAgency() == null || dto.getAccountNumber() == null) {
            throw new IllegalArgumentException("Os dados bancários são obrigatórios");
        }

        boolean isDefault = dto.getIsDefault() != null && dto.getIsDefault();
        if (isDefault || platformBankAccountRepository.countBy() == 0) {
            platformBankAccountRepository.clearDefault();
            isDefault = true;
        }

        PlatformBankAccount account = PlatformBankAccount.builder()
                .accountType(accountType)
                .bankName(dto.getBankName())
                .bankCode(dto.getBankCode())
                .agency(dto.getAgency())
                .agencyDigit(dto.getAgencyDigit())
                .accountNumber(dto.getAccountNumber())
                .accountDigit(dto.getAccountDigit())
                .accountBankType(dto.getAccountBankType() != null
                        ? PlatformBankAccount.BankAccountType.valueOf(dto.getAccountBankType())
                        : null)
                .pixKey(dto.getPixKey())
                .pixKeyType(dto.getPixKeyType())
                .holderName(dto.getHolderName())
                .holderCpfCnpj(dto.getHolderCpfCnpj())
                .isDefault(isDefault)
                .build();

        account = platformBankAccountRepository.save(account);
        BankAccountResponse response = mapToResponse(account);

        auditLogger.recordSemantic(
                "PLATFORM_BANK_ACCOUNT_ADDED",
                "PLATFORM_BANK_ACCOUNT",
                String.valueOf(account.getId()),
                buildAuditEntityLabel(account),
                com.servija.servija.model.AuditLog.Result.SUCCESS,
                dto,
                buildAuditSnapshot(account),
                null,
                null);

        log.info("Conta global da plataforma criada: {}", account.getId());
        return response;
    }

    @Transactional
    public void deleteAccount(Long accountId) {
        PlatformBankAccount account = getRequiredAccount(accountId);
        String entityLabel = buildAuditEntityLabel(account);
        Map<String, Object> deletedAccount = buildAuditSnapshot(account);

        platformBankAccountRepository.delete(account);

        if (Boolean.TRUE.equals(account.getIsDefault())) {
            platformBankAccountRepository.findByOrderByIsDefaultDescCreatedAtDesc()
                    .stream()
                    .findFirst()
                    .ifPresent(next -> {
                        next.setIsDefault(true);
                        platformBankAccountRepository.save(next);
                    });
        }

        auditLogger.recordSemantic(
                "PLATFORM_BANK_ACCOUNT_DELETED",
                "PLATFORM_BANK_ACCOUNT",
                String.valueOf(accountId),
                entityLabel,
                com.servija.servija.model.AuditLog.Result.SUCCESS,
                null,
                deletedAccount,
                null,
                null);

        log.info("Conta global da plataforma removida: {}", accountId);
    }

    @Transactional
    public BankAccountResponse setDefault(Long accountId) {
        PlatformBankAccount account = getRequiredAccount(accountId);

        Map<String, Object> beforeState = Map.of("default", Boolean.TRUE.equals(account.getIsDefault()));
        platformBankAccountRepository.clearDefault();
        account.setIsDefault(true);
        platformBankAccountRepository.save(account);
        Map<String, Object> afterState = Map.of("default", Boolean.TRUE.equals(account.getIsDefault()));

        List<String> changedFields = AuditStateDiffUtils.changedFields(beforeState, afterState);
        if (!changedFields.isEmpty()) {
            auditLogger.recordSemantic(
                    "PLATFORM_BANK_ACCOUNT_DEFAULT",
                    "PLATFORM_BANK_ACCOUNT",
                    String.valueOf(account.getId()),
                    buildAuditEntityLabel(account),
                    com.servija.servija.model.AuditLog.Result.SUCCESS,
                    Map.of("fieldsChanged", changedFields),
                    null,
                    AuditStateDiffUtils.filterFields(beforeState, changedFields),
                    AuditStateDiffUtils.filterFields(afterState, changedFields));
        }

        return mapToResponse(account);
    }

    @Transactional(readOnly = true)
    public PlatformBankAccount getRequiredAccount(Long accountId) {
        return platformBankAccountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Conta não encontrada"));
    }

    public String buildDestinationLabel(PlatformBankAccount account) {
        if (account.getAccountType() == PlatformBankAccount.AccountType.PIX_KEY) {
            return "PIX - "
                    + safeValue(account.getPixKeyType(), "CHAVE")
                    + " - "
                    + suffix(account.getPixKey(), 3);
        }

        return "TED - "
                + safeValue(account.getBankName(), "Banco")
                + " - Ag * - "
                + suffix(account.getAccountNumber(), 4);
    }

    private BankAccountResponse mapToResponse(PlatformBankAccount account) {
        return BankAccountResponse.builder()
                .id(account.getId())
                .accountType(account.getAccountType().name())
                .bankName(account.getBankName())
                .bankCode(account.getBankCode())
                .agency(account.getAgency())
                .accountNumber(maskAccountNumber(account.getAccountNumber()))
                .accountBankType(account.getAccountBankType() != null ? account.getAccountBankType().name() : null)
                .pixKey(maskPixKey(account.getPixKey(), account.getPixKeyType()))
                .pixKeyValue(account.getPixKey())
                .pixKeyType(account.getPixKeyType())
                .holderName(account.getHolderName())
                .holderCpfCnpj(maskCpfCnpj(account.getHolderCpfCnpj()))
                .isDefault(account.getIsDefault())
                .createdAt(account.getCreatedAt())
                .build();
    }

    private String maskAccountNumber(String number) {
        if (number == null || number.length() <= 4) {
            return number;
        }
        return "****" + number.substring(number.length() - 4);
    }

    private String maskPixKey(String key, String type) {
        if (key == null) {
            return null;
        }
        if ("CPF".equals(type) && key.length() >= 11) {
            return "***." + key.substring(3, 6) + ".***-**";
        }
        if ("EMAIL".equals(type) && key.contains("@")) {
            int atIdx = key.indexOf('@');
            return key.substring(0, Math.min(3, atIdx)) + "***" + key.substring(atIdx);
        }
        if (key.length() > 6) {
            return key.substring(0, 3) + "***" + key.substring(key.length() - 3);
        }
        return key;
    }

    private String maskCpfCnpj(String cpfCnpj) {
        if (cpfCnpj == null) {
            return null;
        }
        String digits = cpfCnpj.replaceAll("\\D", "");
        if (digits.length() == 11) {
            return "***." + digits.substring(3, 6) + ".***-**";
        }
        return "**.***.***/" + digits.substring(8, 12) + "-**";
    }

    private String buildAuditEntityLabel(PlatformBankAccount account) {
        return buildDestinationLabel(account);
    }

    private Map<String, Object> buildAuditSnapshot(PlatformBankAccount account) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        if (account.getAccountType() == PlatformBankAccount.AccountType.PIX_KEY) {
            snapshot.put("type", "PIX");
            snapshot.put("keyType", account.getPixKeyType());
            snapshot.put("keyMasked", suffix(account.getPixKey(), 3));
        } else {
            snapshot.put("type", "TED");
            snapshot.put("bankName", account.getBankName());
            snapshot.put("agencyMasked", "*");
            snapshot.put("accountMasked", suffix(account.getAccountNumber(), 4));
        }
        snapshot.put("default", account.getIsDefault());
        return snapshot;
    }

    private String suffix(String value, int length) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.replaceAll("\\s+", "");
        int start = Math.max(0, normalized.length() - length);
        return "**" + normalized.substring(start);
    }

    private String safeValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
