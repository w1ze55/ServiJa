package com.servija.servija.repository;

import com.servija.servija.model.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceRepository extends JpaRepository<Service, Long> {

    // Buscar servicos por freelancer
    List<Service> findByFreelancerIdOrderByCreatedAtDesc(Long freelancerId);

    // Buscar servicos ativos por freelancer
    List<Service> findByFreelancerIdAndIsActiveOrderByCreatedAtDesc(Long freelancerId, Boolean isActive);

    List<Service> findByFreelancerIdInAndIsActiveOrderByCreatedAtDesc(List<Long> freelancerIds, Boolean isActive);

    // Buscar servicos por categoria
    List<Service> findByCategoryAndIsActiveOrderByCreatedAtDesc(String category, Boolean isActive);

    List<Service> findByServiceCategory_SlugAndIsActiveOrderByCreatedAtDesc(String categorySlug, Boolean isActive);

    List<Service> findByServiceSubcategory_SlugAndIsActiveOrderByCreatedAtDesc(String subcategorySlug, Boolean isActive);

    // Buscar servicos ativos
    List<Service> findByIsActiveOrderByCreatedAtDesc(Boolean isActive);

    // Contar servicos por freelancer
    Long countByFreelancerId(Long freelancerId);

    // Contar servicos ativos por freelancer
    Long countByFreelancerIdAndIsActive(Long freelancerId, Boolean isActive);

    // Buscar servicos por texto (titulo, descricao ou nome do freelancer)
    @Query("SELECT s FROM Service s " +
           "JOIN s.freelancer f " +
           "JOIN f.user u " +
           "WHERE s.isActive = true AND " +
           "(LOWER(s.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Service> searchServices(@Param("query") String query);

    // Buscar servicos por categoria e localizacao do freelancer
    @Query("SELECT s FROM Service s JOIN s.freelancer f WHERE s.isActive = true AND s.category = :category " +
           "AND f.latitude IS NOT NULL AND f.longitude IS NOT NULL")
    List<Service> findByCategoryWithLocation(@Param("category") String category);
}
