package com.servija.servija.repository;

import com.servija.servija.model.User;
import com.servija.servija.model.WithdrawalRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WithdrawalRequestRepository extends JpaRepository<WithdrawalRequest, Long> {
    
    List<WithdrawalRequest> findByFreelancer(User freelancer);
    
    List<WithdrawalRequest> findByFreelancerIdOrderByCreatedAtDesc(Long freelancerId);
    
    List<WithdrawalRequest> findByStatusOrderByCreatedAtDesc(WithdrawalRequest.WithdrawalStatus status);

    Optional<WithdrawalRequest> findFirstByAsaasTransferId(String asaasTransferId);
}
