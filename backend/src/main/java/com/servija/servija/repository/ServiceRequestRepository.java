package com.servija.servija.repository;

import com.servija.servija.model.ServiceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Long> {

    List<ServiceRequest> findByContractorIdOrderByCreatedAtDesc(Long contractorId);

    List<ServiceRequest> findByFreelancerIdOrderByCreatedAtDesc(Long freelancerId);

    List<ServiceRequest> findByContractorIdAndStatus(Long contractorId, ServiceRequest.Status status);

    List<ServiceRequest> findByFreelancerIdAndStatus(Long freelancerId, ServiceRequest.Status status);

    long countByFreelancerIdAndStatus(Long freelancerId, ServiceRequest.Status status);

    long countByFreelancerIdAndStatusIn(Long freelancerId, List<ServiceRequest.Status> statuses);

    long countByService_IdAndStatusAndResponseValueIsNotNull(Long serviceId, ServiceRequest.Status status);

    @Query(value = """
            SELECT AVG(response_value)
            FROM service_requests
            WHERE service_id = :serviceId
              AND status = :status
              AND response_value IS NOT NULL
            """, nativeQuery = true)
    BigDecimal findAverageCompletedResponseValueByServiceId(
            @Param("serviceId") Long serviceId,
            @Param("status") String status);
}
