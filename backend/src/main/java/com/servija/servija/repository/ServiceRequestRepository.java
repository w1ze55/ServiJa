package com.servija.servija.repository;

import com.servija.servija.model.ServiceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Long> {

    List<ServiceRequest> findByContractorIdOrderByCreatedAtDesc(Long contractorId);

    List<ServiceRequest> findByFreelancerIdOrderByCreatedAtDesc(Long freelancerId);

    List<ServiceRequest> findByContractorIdAndStatus(Long contractorId, ServiceRequest.Status status);

    List<ServiceRequest> findByFreelancerIdAndStatus(Long freelancerId, ServiceRequest.Status status);
}
