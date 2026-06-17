package com.servija.servija.service;

import com.servija.servija.dto.WalletTransactionPageResponse;
import com.servija.servija.dto.WalletTransactionResponse;
import com.servija.servija.exception.ResourceNotFoundException;
import com.servija.servija.model.Payment;
import com.servija.servija.model.User;
import com.servija.servija.model.WithdrawalRequest;
import com.servija.servija.repository.PaymentRepository;
import com.servija.servija.repository.UserRepository;
import com.servija.servija.repository.WithdrawalRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WalletTransactionService {

    private final PaymentRepository paymentRepository;
    private final WithdrawalRequestRepository withdrawalRepository;
    private final UserRepository userRepository;

    /**
     * Lista unificada de transações (recebimentos + saques)
     */
    @Transactional(readOnly = true)
    public List<WalletTransactionResponse> getTransactions(String email, String typeFilter) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        return assembleTransactions(user, typeFilter, null, null);
    }

    /**
     * Lista paginada de transações (recebimentos + saques), com filtro opcional por período.
     */
    @Transactional(readOnly = true)
    public WalletTransactionPageResponse getTransactionsPaged(
            String email, String typeFilter, LocalDateTime from, LocalDateTime to, int page, int size) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        List<WalletTransactionResponse> all = assembleTransactions(user, typeFilter, from, to);

        int safeSize = size <= 0 ? 20 : size;
        int safePage = Math.max(page, 0);
        int total = all.size();
        int totalPages = (int) Math.ceil((double) total / safeSize);

        int fromIndex = Math.min(safePage * safeSize, total);
        int toIndex = Math.min(fromIndex + safeSize, total);
        List<WalletTransactionResponse> content = all.subList(fromIndex, toIndex);

        return WalletTransactionPageResponse.builder()
                .content(content)
                .totalElements(total)
                .totalPages(totalPages)
                .currentPage(safePage)
                .pageSize(safeSize)
                .build();
    }

    /**
     * Monta a lista unificada de transações ordenada por data (mais recente primeiro),
     * aplicando filtro opcional por tipo e por período ({@code from}/{@code to} inclusivos).
     */
    private List<WalletTransactionResponse> assembleTransactions(
            User user, String typeFilter, LocalDateTime from, LocalDateTime to) {
        List<WalletTransactionResponse> transactions = new ArrayList<>();

        // Recebimentos (pagamentos do freelancer com status COMPLETED)
        if (typeFilter == null || typeFilter.equalsIgnoreCase("ALL") || typeFilter.equalsIgnoreCase("EARNING")) {
            List<Payment> payments = paymentRepository.findByFreelancer(user);
            payments.stream()
                    .filter(p -> p.getStatus() == Payment.PaymentStatus.COMPLETED)
                    .forEach(p -> {
                        String serviceName = "Serviço";
                        String clientName = "";
                        try {
                            if (p.getServiceRequest() != null) {
                                serviceName = p.getServiceRequest().getServiceName();
                            }
                            if (p.getContractor() != null) {
                                clientName = p.getContractor().getName();
                            }
                        } catch (Exception e) {
                            // Fallback for lazy loading issues
                        }
                        transactions.add(WalletTransactionResponse.builder()
                            .id(p.getId())
                            .type("EARNING")
                            .amount(p.getAmount())
                            .status(p.getStatus().name())
                            .description(serviceName)
                            .paymentMethod(p.getPaymentMethod())
                            .serviceName(serviceName)
                            .clientName(clientName)
                            .date(p.getCompletedAt() != null ? p.getCompletedAt() : p.getCreatedAt())
                            .build());
                    });
        }

        // Saques
        if (typeFilter == null || typeFilter.equalsIgnoreCase("ALL") || typeFilter.equalsIgnoreCase("WITHDRAWAL")) {
            List<WithdrawalRequest> withdrawals = withdrawalRepository.findByFreelancerIdOrderByCreatedAtDesc(user.getId());
            withdrawals.forEach(w -> {
                String method = w.getWithdrawalMethod() != null
                        ? w.getWithdrawalMethod().name()
                        : "PIX";
                transactions.add(WalletTransactionResponse.builder()
                    .id(w.getId())
                    .type("WITHDRAWAL")
                    .amount(w.getAmount().negate())
                    .status(w.getStatus().name())
                    .description("Saque via " + method)
                    .paymentMethod(method)
                    .date(w.getCreatedAt())
                    .build());
            });
        }

        // Filtro por período (inclusivo)
        List<WalletTransactionResponse> filtered = transactions.stream()
                .filter(t -> t.getDate() != null)
                .filter(t -> from == null || !t.getDate().isBefore(from))
                .filter(t -> to == null || !t.getDate().isAfter(to))
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));

        // Ordenar por data (mais recente primeiro)
        filtered.sort(Comparator.comparing(WalletTransactionResponse::getDate).reversed());
        return filtered;
    }
}
