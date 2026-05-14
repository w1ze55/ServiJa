package com.servija.servija.repository;

import com.servija.servija.model.SupportRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SupportRequestRepository extends JpaRepository<SupportRequest, Long> {

    List<SupportRequest> findAllByOrderByCreatedAtDesc();

    List<SupportRequest> findByRequesterIdOrderByCreatedAtDesc(Long requesterId);
}
