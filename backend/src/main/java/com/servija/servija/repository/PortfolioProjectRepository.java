package com.servija.servija.repository;

import com.servija.servija.model.PortfolioProject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PortfolioProjectRepository extends JpaRepository<PortfolioProject, Long> {
    List<PortfolioProject> findByFreelancerIdOrderByCreatedAtDesc(Long freelancerId);
    List<PortfolioProject> findByFreelancerIdAndFeaturedOrderByCreatedAtDesc(Long freelancerId, Boolean featured);
    Optional<PortfolioProject> findByIdAndFreelancerId(Long id, Long freelancerId);
    void deleteByIdAndFreelancerId(Long id, Long freelancerId);
}
