package com.servija.servija.repository;

import com.servija.servija.model.ReviewSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewSummaryRepository extends JpaRepository<ReviewSummary, Long> {

    Optional<ReviewSummary> findByFreelancerId(Long freelancerId);
}
