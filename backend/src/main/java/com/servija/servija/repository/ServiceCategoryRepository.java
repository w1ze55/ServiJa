package com.servija.servija.repository;

import com.servija.servija.model.ServiceCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceCategoryRepository extends JpaRepository<ServiceCategory, Long> {

    Optional<ServiceCategory> findBySlug(String slug);

    Optional<ServiceCategory> findByNameIgnoreCase(String name);

    boolean existsBySlug(String slug);

    @Query("""
            SELECT DISTINCT c
            FROM ServiceCategory c
            LEFT JOIN FETCH c.subcategories sc
            ORDER BY c.sortOrder ASC, c.name ASC, sc.sortOrder ASC, sc.name ASC
            """)
    List<ServiceCategory> findAllWithSubcategories();

    @Query("""
            SELECT DISTINCT c
            FROM ServiceCategory c
            LEFT JOIN FETCH c.subcategories sc
            WHERE c.active = true
            ORDER BY c.sortOrder ASC, c.name ASC, sc.sortOrder ASC, sc.name ASC
            """)
    List<ServiceCategory> findActiveWithSubcategories();
}
