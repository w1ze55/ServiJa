package com.servija.servija.repository;

import com.servija.servija.model.PortfolioImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PortfolioImageRepository extends JpaRepository<PortfolioImage, Long> {
    List<PortfolioImage> findByProjectIdOrderByDisplayOrder(Long projectId);
    void deleteByProjectId(Long projectId);
}
