package com.servija.servija.repository;

import com.servija.servija.model.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface ServiceRepository extends JpaRepository<Service, Long> {

    interface FreelancerCardProjection {
        Long getId();
        String getName();
        String getAvatar();
        String getLocation();
        Long getServicesCount();
        String getCategories();
        BigDecimal getRating();
        Long getReviewCount();
    }

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

    // Espelha PublicVisibilityRules.isServicePublic; ordenação por preço usa a média de concluídos como fallback
    @Query(value = """
            SELECT s.*
            FROM services s
            JOIN freelancers f ON f.user_id = s.freelancer_id
            JOIN users u ON u.id = f.user_id
            LEFT JOIN service_categories sc ON sc.id = s.category_id
            LEFT JOIN service_subcategories ssc ON ssc.id = s.subcategory_id
            WHERE s.is_active = true
              AND f.available = true
              AND u.active = true
              AND u.deleted_at IS NULL
              AND (:categorySlug IS NULL OR sc.slug = :categorySlug)
              AND (:subcategorySlug IS NULL OR ssc.slug = :subcategorySlug)
              AND (:query IS NULL
                   OR LOWER(s.title) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(s.description) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%')))
              AND (:excludeFreelancerId IS NULL OR s.freelancer_id <> :excludeFreelancerId)
            ORDER BY
              CASE WHEN :sort = 'name-asc' THEN s.title END ASC,
              CASE WHEN :sort = 'price-asc' THEN COALESCE(
                   (SELECT AVG(sr.response_value) FROM service_requests sr
                    WHERE sr.service_id = s.id AND sr.status = 'COMPLETED' AND sr.response_value IS NOT NULL),
                   s.price) END ASC,
              CASE WHEN :sort = 'price-desc' THEN COALESCE(
                   (SELECT AVG(sr.response_value) FROM service_requests sr
                    WHERE sr.service_id = s.id AND sr.status = 'COMPLETED' AND sr.response_value IS NOT NULL),
                   s.price) END DESC,
              s.created_at DESC
            """,
            countQuery = """
            SELECT COUNT(*)
            FROM services s
            JOIN freelancers f ON f.user_id = s.freelancer_id
            JOIN users u ON u.id = f.user_id
            LEFT JOIN service_categories sc ON sc.id = s.category_id
            LEFT JOIN service_subcategories ssc ON ssc.id = s.subcategory_id
            WHERE s.is_active = true
              AND f.available = true
              AND u.active = true
              AND u.deleted_at IS NULL
              AND (:categorySlug IS NULL OR sc.slug = :categorySlug)
              AND (:subcategorySlug IS NULL OR ssc.slug = :subcategorySlug)
              AND (:query IS NULL
                   OR LOWER(s.title) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(s.description) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%')))
              AND (:excludeFreelancerId IS NULL OR s.freelancer_id <> :excludeFreelancerId)
            """,
            nativeQuery = true)
    Page<Service> findPublicServicesPage(
            @Param("categorySlug") String categorySlug,
            @Param("subcategorySlug") String subcategorySlug,
            @Param("query") String query,
            @Param("excludeFreelancerId") Long excludeFreelancerId,
            @Param("sort") String sort,
            Pageable pageable);

    // Freelancers com ao menos um serviço público casando com os filtros
    @Query(value = """
            SELECT f.user_id AS id,
                   u.name AS name,
                   u.avatar_url AS avatar,
                   (SELECT CONCAT(a.city, ', ', a.state) FROM addresses a
                    WHERE a.user_id = f.user_id
                    ORDER BY a.is_default DESC, a.created_at DESC LIMIT 1) AS location,
                   COUNT(DISTINCT s.id) AS servicesCount,
                   GROUP_CONCAT(DISTINCT s.category ORDER BY s.category SEPARATOR '||') AS categories,
                   (SELECT AVG(r.rating) FROM reviews r WHERE r.reviewed_id = f.user_id) AS rating,
                   (SELECT COUNT(*) FROM reviews r WHERE r.reviewed_id = f.user_id) AS reviewCount
            FROM services s
            JOIN freelancers f ON f.user_id = s.freelancer_id
            JOIN users u ON u.id = f.user_id
            LEFT JOIN service_categories sc ON sc.id = s.category_id
            LEFT JOIN service_subcategories ssc ON ssc.id = s.subcategory_id
            WHERE s.is_active = true
              AND f.available = true
              AND u.active = true
              AND u.deleted_at IS NULL
              AND (:categorySlug IS NULL OR sc.slug = :categorySlug)
              AND (:subcategorySlug IS NULL OR ssc.slug = :subcategorySlug)
              AND (:query IS NULL
                   OR LOWER(s.title) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(s.description) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%')))
              AND (:excludeFreelancerId IS NULL OR s.freelancer_id <> :excludeFreelancerId)
            GROUP BY f.user_id, u.name, u.avatar_url
            ORDER BY rating DESC, reviewCount DESC, u.name ASC
            """,
            countQuery = """
            SELECT COUNT(DISTINCT s.freelancer_id)
            FROM services s
            JOIN freelancers f ON f.user_id = s.freelancer_id
            JOIN users u ON u.id = f.user_id
            LEFT JOIN service_categories sc ON sc.id = s.category_id
            LEFT JOIN service_subcategories ssc ON ssc.id = s.subcategory_id
            WHERE s.is_active = true
              AND f.available = true
              AND u.active = true
              AND u.deleted_at IS NULL
              AND (:categorySlug IS NULL OR sc.slug = :categorySlug)
              AND (:subcategorySlug IS NULL OR ssc.slug = :subcategorySlug)
              AND (:query IS NULL
                   OR LOWER(s.title) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(s.description) LIKE LOWER(CONCAT('%', :query, '%'))
                   OR LOWER(u.name) LIKE LOWER(CONCAT('%', :query, '%')))
              AND (:excludeFreelancerId IS NULL OR s.freelancer_id <> :excludeFreelancerId)
            """,
            nativeQuery = true)
    Page<FreelancerCardProjection> findPublicFreelancerCardsPage(
            @Param("categorySlug") String categorySlug,
            @Param("subcategorySlug") String subcategorySlug,
            @Param("query") String query,
            @Param("excludeFreelancerId") Long excludeFreelancerId,
            Pageable pageable);
}
