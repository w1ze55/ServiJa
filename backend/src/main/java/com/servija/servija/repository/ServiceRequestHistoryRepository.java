package com.servija.servija.repository;

import com.servija.servija.model.ServiceRequestHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ServiceRequestHistoryRepository extends JpaRepository<ServiceRequestHistory, Long> {

    interface TopContractedServiceProjection {
        String getLabel();
        long getTotal();
    }

    interface RevenueByDayProjection {
        String getBucketDate();
        BigDecimal getRevenue();
    }

    interface RevenueByMonthProjection {
        String getBucketMonth();
        BigDecimal getRevenue();
    }

    List<ServiceRequestHistory> findByServiceRequestIdOrderByCreatedAtDesc(Long serviceRequestId);

    @Query("""
            select count(distinct h.serviceRequest.id)
            from ServiceRequestHistory h
            where h.operationType = :operationType
              and h.createdAt >= :start
              and h.createdAt < :end
            """)
    long countDistinctServiceRequestsByOperationTypeAndCreatedAtBetween(
            @Param("operationType") ServiceRequestHistory.OperationType operationType,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("""
            select sum(p.totalAmount)
            from ServiceRequestHistory h
            join Payment p on p.serviceRequest = h.serviceRequest
            where h.operationType = :operationType
              and h.createdAt >= :start
              and h.createdAt < :end
            """)
    BigDecimal sumCompletedRevenueByOperationTypeAndCreatedAtBetween(
            @Param("operationType") ServiceRequestHistory.OperationType operationType,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("""
            select case
                     when subcategory.name is not null then subcategory.name
                     when sr.subcategory is not null and sr.subcategory <> '' then sr.subcategory
                     else sr.category
                   end as label,
                   count(distinct h.serviceRequest.id) as total
            from ServiceRequestHistory h
            join h.serviceRequest sr
            left join sr.serviceSubcategory subcategory
            where h.operationType in :operationTypes
              and h.createdAt >= :start
              and h.createdAt < :end
            group by case
                     when subcategory.name is not null then subcategory.name
                     when sr.subcategory is not null and sr.subcategory <> '' then sr.subcategory
                     else sr.category
                   end
            order by count(distinct h.serviceRequest.id) desc,
                     case
                       when subcategory.name is not null then subcategory.name
                       when sr.subcategory is not null and sr.subcategory <> '' then sr.subcategory
                       else sr.category
                     end asc
            """)
    List<TopContractedServiceProjection> findTopContractedServicesByOperationTypeInAndCreatedAtBetween(
            @Param("operationTypes") List<ServiceRequestHistory.OperationType> operationTypes,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query(value = """
            SELECT DATE(h.created_at) AS bucketDate,
                   COALESCE(SUM(p.total_amount), 0) AS revenue
            FROM service_request_history h
            JOIN payments p ON p.service_request_id = h.service_request_id
            WHERE h.operation_type = :operationType
              AND h.created_at >= :start
              AND h.created_at < :end
            GROUP BY DATE(h.created_at)
            ORDER BY DATE(h.created_at)
            """, nativeQuery = true)
    List<RevenueByDayProjection> findRevenueSeriesByDay(
            @Param("operationType") String operationType,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query(value = """
            SELECT DATE_FORMAT(h.created_at, '%Y-%m-01') AS bucketMonth,
                   COALESCE(SUM(p.total_amount), 0) AS revenue
            FROM service_request_history h
            JOIN payments p ON p.service_request_id = h.service_request_id
            WHERE h.operation_type = :operationType
              AND h.created_at >= :start
              AND h.created_at < :end
            GROUP BY DATE_FORMAT(h.created_at, '%Y-%m-01')
            ORDER BY DATE_FORMAT(h.created_at, '%Y-%m-01')
            """, nativeQuery = true)
    List<RevenueByMonthProjection> findRevenueSeriesByMonth(
            @Param("operationType") String operationType,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);
}
