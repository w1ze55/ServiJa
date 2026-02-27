package com.servija.servija.repository;

import com.servija.servija.model.Favorite;
import com.servija.servija.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    List<Favorite> findByUser(User user);

    Optional<Favorite> findByUserIdAndServiceId(Long userId, Long serviceId);

    Optional<Favorite> findByUserIdAndFreelancerId(Long userId, Long freelancerId);

    boolean existsByUserIdAndServiceId(Long userId, Long serviceId);

    boolean existsByUserIdAndFreelancerId(Long userId, Long freelancerId);
}
