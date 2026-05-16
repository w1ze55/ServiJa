package com.servija.servija.repository;

import com.servija.servija.model.ServiceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Long> {

    interface CompletedJobsCountProjection {
        Long getFreelancerId();
        long getCompletedJobs();
    }

    List<ServiceRequest> findByContractorIdOrderByCreatedAtDesc(Long contractorId);

    List<ServiceRequest> findByFreelancerIdOrderByCreatedAtDesc(Long freelancerId);

    List<ServiceRequest> findByContractorIdAndStatus(Long contractorId, ServiceRequest.Status status);

    List<ServiceRequest> findByFreelancerIdAndStatus(Long freelancerId, ServiceRequest.Status status);

    long countByFreelancerIdAndStatus(Long freelancerId, ServiceRequest.Status status);

    long countByContractorIdAndStatus(Long contractorId, ServiceRequest.Status status);

    long countByContractorIdAndStatusIn(Long contractorId, List<ServiceRequest.Status> statuses);

    @Query("""
            SELECT sr.freelancer.id AS freelancerId, COUNT(sr) AS completedJobs
            FROM ServiceRequest sr
            WHERE sr.freelancer.id IN :freelancerIds
              AND sr.status = :status
            GROUP BY sr.freelancer.id
            """)
    List<CompletedJobsCountProjection> countCompletedJobsByFreelancerIds(
            @Param("freelancerIds") List<Long> freelancerIds,
            @Param("status") ServiceRequest.Status status);

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

    @Modifying
    @Query(value = """
            UPDATE service_requests
            SET category_id = :categoryId,
                subcategory_id = :subcategoryId
            WHERE id = :requestId
            """, nativeQuery = true)
    void backfillCatalogAssignment(
            @Param("requestId") Long requestId,
            @Param("categoryId") Long categoryId,
            @Param("subcategoryId") Long subcategoryId);
}
