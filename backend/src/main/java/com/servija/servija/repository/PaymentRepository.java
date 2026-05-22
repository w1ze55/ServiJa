package com.servija.servija.repository;

import com.servija.servija.model.Payment;
import com.servija.servija.model.ServiceRequest;
import com.servija.servija.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    interface PaymentStatusCountProjection {
        Payment.PaymentStatus getStatus();
        long getTotal();
    }
    
    List<Payment> findByContractor(User contractor);
    
    List<Payment> findByFreelancer(User freelancer);
    
    List<Payment> findByServiceRequestId(Long serviceRequestId);
    
    boolean existsByServiceRequest(ServiceRequest serviceRequest);
    
    /**
     * Buscar pagamento por ID do Asaas (otimizado para webhook)
     */
    Payment findByAsaasPaymentId(String asaasPaymentId);

    /**
     * Buscar pagamento por ID da transação PIX
     */
    Payment findByPixTransactionId(String pixTransactionId);

    /**
     * Buscar pagamentos PIX pendentes por valor
     */
    List<Payment> findByPaymentMethodAndStatusInAndTotalAmount(
            String paymentMethod,
            List<Payment.PaymentStatus> statuses,
            java.math.BigDecimal totalAmount);

    @Query("""
            select distinct p.asaasCustomerId
            from Payment p
            where p.asaasCustomerId is not null
              and p.asaasCustomerId <> ''
            """)
    List<String> findDistinctAsaasCustomerIds();

    @Query("""
            select p.status as status, count(p) as total
            from Payment p
            where p.createdAt >= :start
              and p.createdAt < :end
            group by p.status
            """)
    List<PaymentStatusCountProjection> countStatusesCreatedBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("""
            select min(p.createdAt)
            from Payment p
            """)
    LocalDateTime findEarliestCreatedAt();
}
