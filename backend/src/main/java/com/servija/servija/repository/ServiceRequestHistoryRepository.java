package com.servija.servija.repository;

import com.servija.servija.model.ServiceRequestHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceRequestHistoryRepository extends JpaRepository<ServiceRequestHistory, Long> {

    List<ServiceRequestHistory> findByServiceRequestIdOrderByCreatedAtDesc(Long serviceRequestId);
}
