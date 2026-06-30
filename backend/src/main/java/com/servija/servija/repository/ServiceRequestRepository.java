package com.servija.servija.repository;

import com.servija.servija.model.ServiceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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

    // Existe alguma solicitação associada a este serviço? (usado para decidir soft vs hard delete)
    boolean existsByService_Id(Long serviceId);

    @Query("""
            select count(sr)
            from ServiceRequest sr
            where sr.createdAt >= :start
              and sr.createdAt < :end
            """)
    long countCreatedBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("""
            select min(sr.createdAt)
            from ServiceRequest sr
            """)
    LocalDateTime findEarliestCreatedAt();

    @Query(value = """
            SELECT COUNT(DISTINCT activity.user_id)
            FROM (
                SELECT sr.contractor_id AS user_id
                FROM service_requests sr
                WHERE sr.created_at >= :start
                  AND sr.created_at < :end
                UNION
                SELECT p.contractor_id AS user_id
                FROM payments p
                WHERE (p.created_at >= :start AND p.created_at < :end)
                   OR (p.completed_at IS NOT NULL AND p.completed_at >= :start AND p.completed_at < :end)
            ) activity
            """, nativeQuery = true)
    long countDistinctActiveContractorsBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query(value = """
            SELECT COUNT(DISTINCT activity.user_id)
            FROM (
                SELECT sr.freelancer_id AS user_id
                FROM service_requests sr
                WHERE sr.freelancer_id IS NOT NULL
                  AND sr.created_at >= :start
                  AND sr.created_at < :end
                UNION
                SELECT p.freelancer_id AS user_id
                FROM payments p
                WHERE p.freelancer_id IS NOT NULL
                  AND ((p.created_at >= :start AND p.created_at < :end)
                   OR (p.completed_at IS NOT NULL AND p.completed_at >= :start AND p.completed_at < :end))
            ) activity
            """, nativeQuery = true)
    long countDistinctActiveFreelancersBetween(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

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
