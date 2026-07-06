package com.servija.servija.repository;

import com.servija.servija.model.Service;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceRepository extends JpaRepository<Service, Long> {

    // Buscar serviços por freelancer
    List<Service> findByFreelancerIdOrderByCreatedAtDesc(Long freelancerId);

    // Buscar serviços por freelancer ignorando os excluídos logicamente (soft delete)
    List<Service> findByFreelancerIdAndDeletedAtIsNullOrderByCreatedAtDesc(Long freelancerId);

    // Buscar serviços ativos por freelancer
    List<Service> findByFreelancerIdAndIsActiveOrderByCreatedAtDesc(Long freelancerId, Boolean isActive);

    List<Service> findByFreelancerIdInAndIsActiveOrderByCreatedAtDesc(List<Long> freelancerIds, Boolean isActive);

    // Buscar serviços por categoria
    List<Service> findByCategoryAndIsActiveOrderByCreatedAtDesc(String category, Boolean isActive);

    List<Service> findByServiceCategory_SlugAndIsActiveOrderByCreatedAtDesc(String categorySlug, Boolean isActive);

    List<Service> findByServiceSubcategory_SlugAndIsActiveOrderByCreatedAtDesc(String subcategorySlug, Boolean isActive);

    // Buscar serviços ativos
    List<Service> findByIsActiveOrderByCreatedAtDesc(Boolean isActive);

    // Contar serviços por freelancer
    Long countByFreelancerId(Long freelancerId);

    // Contar serviços ativos por freelancer
    Long countByFreelancerIdAndIsActive(Long freelancerId, Boolean isActive);

    // Contagens ignorando os excluídos logicamente (soft delete)
    Long countByFreelancerIdAndDeletedAtIsNull(Long freelancerId);

    Long countByFreelancerIdAndIsActiveAndDeletedAtIsNull(Long freelancerId, Boolean isActive);

    // Buscar serviços por texto (título, descrição ou nome do freelancer)
    @Query("SELECT s FROM Service s " +
           "JOIN s.freelancer f " +
           "JOIN f.user u " +
           "WHERE s.isActive = true AND " +
           "(LOWER(s.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.description) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%')))")
    List<Service> searchServices(@Param("query") String query);

    // Buscar serviços por categoria e localização do freelancer
    @Query("SELECT s FROM Service s JOIN s.freelancer f WHERE s.isActive = true AND s.category = :category " +
           "AND f.latitude IS NOT NULL AND f.longitude IS NOT NULL")
    List<Service> findByCategoryWithLocation(@Param("category") String category);
}
