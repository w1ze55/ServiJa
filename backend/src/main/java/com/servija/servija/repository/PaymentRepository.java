package com.servija.servija.repository;

import com.servija.servija.model.Payment;
import com.servija.servija.model.ServiceRequest;
import com.servija.servija.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    
    List<Payment> findByContractor(User contractor);
    
    List<Payment> findByFreelancer(User freelancer);
    
    List<Payment> findByServiceRequestId(Long serviceRequestId);
    
    boolean existsByServiceRequest(ServiceRequest serviceRequest);
    
    /**
     * Buscar pagamento por ID do Asaas (otimizado para webhook)
     */
    Payment findByAsaasPaymentId(String asaasPaymentId);
}
