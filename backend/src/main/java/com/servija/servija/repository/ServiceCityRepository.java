package com.servija.servija.repository;

import com.servija.servija.model.ServiceCity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceCityRepository extends JpaRepository<ServiceCity, Long> {
    List<ServiceCity> findByFreelancerId(Long freelancerId);
    void deleteByFreelancerIdAndId(Long freelancerId, Long id);
}
