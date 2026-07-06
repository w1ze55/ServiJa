package com.servija.servija.repository;

import com.servija.servija.model.DeliveryAdjustment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface DeliveryAdjustmentRepository extends JpaRepository<DeliveryAdjustment, Long> {

    List<DeliveryAdjustment> findByServiceRequestIdOrderByCreatedAtDesc(Long serviceRequestId);

    boolean existsByServiceRequestId(Long serviceRequestId);

    boolean existsByServiceRequestIdAndStatusIn(Long serviceRequestId, Collection<DeliveryAdjustment.Status> statuses);
    
    List<DeliveryAdjustment> findByServiceRequestFreelancerEmailOrderByCreatedAtDesc(String freelancerEmail);
    
    List<DeliveryAdjustment> findByRequestedByEmailOrderByCreatedAtDesc(String contractorEmail);
}
