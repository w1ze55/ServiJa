package com.servija.servija.repository;

import com.servija.servija.model.Freelancer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FreelancerRepository extends JpaRepository<Freelancer, Long> {
    
    // Buscar freelancers com coordenadas na tabela freelancers (legado)
    @Query("SELECT f FROM Freelancer f WHERE f.latitude IS NOT NULL AND f.longitude IS NOT NULL")
    List<Freelancer> findAllWithLocation();

    // Buscar freelancers disponíveis com coordenadas (legado)
    @Query("SELECT f FROM Freelancer f WHERE f.latitude IS NOT NULL AND f.longitude IS NOT NULL AND f.available = true")
    List<Freelancer> findAvailableWithLocation();

    // Buscar freelancers por categoria com coordenadas (legado)
    @Query("SELECT f FROM Freelancer f WHERE f.latitude IS NOT NULL AND f.longitude IS NOT NULL AND f.category = :category")
    List<Freelancer> findByCategoryWithLocation(@Param("category") String category);

    // Buscar freelancers que possuem service_cities com coordenadas (com fetch das cidades)
    @Query("SELECT DISTINCT f FROM Freelancer f JOIN FETCH f.serviceCities sc WHERE sc.latitude IS NOT NULL AND sc.longitude IS NOT NULL")
    List<Freelancer> findAllWithServiceCityCoordinates();

    // Buscar freelancers por categoria que possuem service_cities com coordenadas (com fetch das cidades)
    @Query("SELECT DISTINCT f FROM Freelancer f JOIN FETCH f.serviceCities sc WHERE sc.latitude IS NOT NULL AND sc.longitude IS NOT NULL AND f.category = :category")
    List<Freelancer> findByCategoryWithServiceCityCoordinates(@Param("category") String category);
    
    // Buscar freelancer por ID com serviceCities carregados
    @Query("SELECT f FROM Freelancer f LEFT JOIN FETCH f.serviceCities WHERE f.id = :id")
    java.util.Optional<Freelancer> findByIdWithServiceCities(@Param("id") Long id);
    
    // Buscar freelancer por ID com professionalLinks carregados
    @Query("SELECT f FROM Freelancer f LEFT JOIN FETCH f.professionalLinks WHERE f.id = :id")
    java.util.Optional<Freelancer> findByIdWithProfessionalLinks(@Param("id") Long id);

    @Query("SELECT DISTINCT f FROM Freelancer f LEFT JOIN FETCH f.serviceCities WHERE f.id IN :ids")
    List<Freelancer> findAllByIdInWithServiceCities(@Param("ids") List<Long> ids);
}


