package com.servija.servija.repository;

import com.servija.servija.model.ServiceSearchWeeklyMetric;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ServiceSearchWeeklyMetricRepository extends JpaRepository<ServiceSearchWeeklyMetric, Long> {

    List<ServiceSearchWeeklyMetric> findByService_IdInAndWeekStartDate(List<Long> serviceIds, LocalDate weekStartDate);

    @Query("""
            select metric.service.id
            from ServiceSearchWeeklyMetric metric
            where metric.weekStartDate = :weekStartDate
            order by metric.searchCount desc, metric.updatedAt desc, metric.service.id asc
            """)
    List<Long> findTopServiceIdsByWeekStartDate(
            @Param("weekStartDate") LocalDate weekStartDate,
            Pageable pageable);
}
