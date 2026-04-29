package com.servija.servija.repository;

import com.servija.servija.model.ServiceCategory;
import com.servija.servija.model.ServiceSubcategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServiceSubcategoryRepository extends JpaRepository<ServiceSubcategory, Long> {

    Optional<ServiceSubcategory> findBySlug(String slug);

    Optional<ServiceSubcategory> findByNameIgnoreCase(String name);

    Optional<ServiceSubcategory> findByNameIgnoreCaseAndCategoryId(String name, Long categoryId);

    List<ServiceSubcategory> findByCategoryOrderBySortOrderAscNameAsc(ServiceCategory category);

    List<ServiceSubcategory> findByCategoryIdAndActiveTrueOrderBySortOrderAscNameAsc(Long categoryId);

    long countByCategoryIdAndActiveTrue(Long categoryId);
}
