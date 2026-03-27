package com.servija.servija.repository;

import com.servija.servija.model.Payment;
import com.servija.servija.model.PaymentStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentStatusHistoryRepository extends JpaRepository<PaymentStatusHistory, Long> {
    
    /**
     * Buscar histórico de um pagamento ordenado por data
     */
    List<PaymentStatusHistory> findByPaymentOrderByCreatedAtAsc(Payment payment);
    
    /**
     * Buscar histórico de um pagamento por ID ordenado por data
     */
    List<PaymentStatusHistory> findByPaymentIdOrderByCreatedAtAsc(Long paymentId);
}
