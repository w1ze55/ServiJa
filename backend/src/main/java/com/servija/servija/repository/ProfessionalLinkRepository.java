package com.servija.servija.repository;

import com.servija.servija.model.ProfessionalLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProfessionalLinkRepository extends JpaRepository<ProfessionalLink, Long> {
    List<ProfessionalLink> findByFreelancerId(Long freelancerId);
    void deleteByFreelancerIdAndId(Long freelancerId, Long id);
}
