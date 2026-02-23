package com.servija.servija.repository;

import com.servija.servija.model.Freelancer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FreelancerRepository extends JpaRepository<Freelancer, Long> {
    
    // Buscar freelancers com coordenadas definidas
    @Query("SELECT f FROM Freelancer f WHERE f.latitude IS NOT NULL AND f.longitude IS NOT NULL AND f.profileCompleted = true")
    List<Freelancer> findAllWithLocation();
    
    // Buscar freelancers disponíveis com coordenadas
    @Query("SELECT f FROM Freelancer f WHERE f.latitude IS NOT NULL AND f.longitude IS NOT NULL AND f.available = true AND f.profileCompleted = true")
    List<Freelancer> findAvailableWithLocation();
    
    // Buscar freelancers por categoria com coordenadas
    @Query("SELECT f FROM Freelancer f WHERE f.latitude IS NOT NULL AND f.longitude IS NOT NULL AND f.category = :category AND f.profileCompleted = true")
    List<Freelancer> findByCategoryWithLocation(@Param("category") String category);
    
    // Buscar freelancer por ID com serviceCities carregados
    @Query("SELECT f FROM Freelancer f LEFT JOIN FETCH f.serviceCities WHERE f.id = :id")
    java.util.Optional<Freelancer> findByIdWithServiceCities(@Param("id") Long id);
    
    // Buscar freelancer por ID com professionalLinks carregados
    @Query("SELECT f FROM Freelancer f LEFT JOIN FETCH f.professionalLinks WHERE f.id = :id")
    java.util.Optional<Freelancer> findByIdWithProfessionalLinks(@Param("id") Long id);
}


