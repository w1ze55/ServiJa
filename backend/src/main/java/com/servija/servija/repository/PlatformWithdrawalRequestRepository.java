package com.servija.servija.repository;

import com.servija.servija.model.PlatformWithdrawalRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlatformWithdrawalRequestRepository extends JpaRepository<PlatformWithdrawalRequest, Long> {

    List<PlatformWithdrawalRequest> findAllByOrderByCreatedAtDesc();
}
