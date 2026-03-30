package com.servija.servija.service;

import com.servija.servija.dto.BankAccountDTO;
import com.servija.servija.dto.BankAccountResponse;
import com.servija.servija.exception.ResourceNotFoundException;
import com.servija.servija.model.FreelancerBankAccount;
import com.servija.servija.model.User;
import com.servija.servija.repository.FreelancerBankAccountRepository;
import com.servija.servija.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BankAccountService {

    private final FreelancerBankAccountRepository bankAccountRepository;
    private final UserRepository userRepository;

    private static final int MAX_ACCOUNTS = 5;

    @Transactional(readOnly = true)
    public List<BankAccountResponse> getAccountsByEmail(String email) {
        User user = findUser(email);
        return bankAccountRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(user.getId())
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public BankAccountResponse createAccount(String email, BankAccountDTO dto) {
        User user = findUser(email);

        if (bankAccountRepository.countByUserId(user.getId()) >= MAX_ACCOUNTS) {
            throw new IllegalStateException("Limite de " + MAX_ACCOUNTS + " contas atingido");
        }

        FreelancerBankAccount.AccountType accountType =
                FreelancerBankAccount.AccountType.valueOf(dto.getAccountType());

        if (accountType == FreelancerBankAccount.AccountType.PIX_KEY) {
            if (dto.getPixKey() == null || dto.getPixKey().isBlank()) {
                throw new IllegalArgumentException("Chave PIX é obrigatória");
            }
        } else {
            if (dto.getBankCode() == null || dto.getAgency() == null || dto.getAccountNumber() == null) {
                throw new IllegalArgumentException("Dados bancários são obrigatórios");
            }
        }

        boolean isDefault = dto.getIsDefault() != null && dto.getIsDefault();
        if (isDefault || bankAccountRepository.countByUserId(user.getId()) == 0) {
            bankAccountRepository.clearDefaultForUser(user.getId());
            isDefault = true;
        }

        FreelancerBankAccount account = FreelancerBankAccount.builder()
                .user(user)
                .accountType(accountType)
                .bankName(dto.getBankName())
                .bankCode(dto.getBankCode())
                .agency(dto.getAgency())
                .agencyDigit(dto.getAgencyDigit())
                .accountNumber(dto.getAccountNumber())
                .accountDigit(dto.getAccountDigit())
                .accountBankType(dto.getAccountBankType() != null
                        ? FreelancerBankAccount.BankAccountType.valueOf(dto.getAccountBankType())
                        : null)
                .pixKey(dto.getPixKey())
                .pixKeyType(dto.getPixKeyType())
                .holderName(dto.getHolderName())
                .holderCpfCnpj(dto.getHolderCpfCnpj())
                .isDefault(isDefault)
                .build();

        account = bankAccountRepository.save(account);
        log.info("Conta bancária criada: {} para {}", account.getId(), email);
        return mapToResponse(account);
    }

    @Transactional
    public void deleteAccount(String email, Long accountId) {
        User user = findUser(email);
        FreelancerBankAccount account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Conta não encontrada"));

        if (!account.getUser().getId().equals(user.getId())) {
            throw new IllegalStateException("Sem permissão para remover esta conta");
        }

        bankAccountRepository.delete(account);

        if (Boolean.TRUE.equals(account.getIsDefault())) {
            bankAccountRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(user.getId())
                    .stream().findFirst().ifPresent(first -> {
                        first.setIsDefault(true);
                        bankAccountRepository.save(first);
                    });
        }

        log.info("Conta bancária removida: {}", accountId);
    }

    @Transactional
    public BankAccountResponse setDefault(String email, Long accountId) {
        User user = findUser(email);
        FreelancerBankAccount account = bankAccountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Conta não encontrada"));

        if (!account.getUser().getId().equals(user.getId())) {
            throw new IllegalStateException("Sem permissão");
        }

        bankAccountRepository.clearDefaultForUser(user.getId());
        account.setIsDefault(true);
        bankAccountRepository.save(account);

        log.info("Conta {} definida como padrão para {}", accountId, email);
        return mapToResponse(account);
    }

    public FreelancerBankAccount getAccountById(Long id) {
        return bankAccountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Conta não encontrada"));
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));
    }

    private BankAccountResponse mapToResponse(FreelancerBankAccount account) {
        return BankAccountResponse.builder()
                .id(account.getId())
                .accountType(account.getAccountType().name())
                .bankName(account.getBankName())
                .bankCode(account.getBankCode())
                .agency(account.getAgency())
                .accountNumber(maskAccountNumber(account.getAccountNumber()))
                .accountBankType(account.getAccountBankType() != null ? account.getAccountBankType().name() : null)
                .pixKey(maskPixKey(account.getPixKey(), account.getPixKeyType()))
                .pixKeyType(account.getPixKeyType())
                .holderName(account.getHolderName())
                .holderCpfCnpj(maskCpfCnpj(account.getHolderCpfCnpj()))
                .isDefault(account.getIsDefault())
                .createdAt(account.getCreatedAt())
                .build();
    }

    private String maskAccountNumber(String number) {
        if (number == null || number.length() <= 4) return number;
        return "****" + number.substring(number.length() - 4);
    }

    private String maskPixKey(String key, String type) {
        if (key == null) return null;
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
        if (cpfCnpj == null) return null;
        String digits = cpfCnpj.replaceAll("\\D", "");
        if (digits.length() == 11) {
            return "***." + digits.substring(3, 6) + ".***-**";
        }
        return "**.***.***/" + digits.substring(8, 12) + "-**";
    }
}
