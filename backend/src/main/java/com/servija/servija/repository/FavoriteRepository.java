package com.servija.servija.repository;

import com.servija.servija.model.Favorite;
import com.servija.servija.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
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

    // Remove todos os favoritos que apontam para um servico (ao excluir o servico).
    // Bulk delete imediato para garantir a ordem em relacao a remocao do servico (FK).
    @Modifying
    @Query("delete from Favorite f where f.service.id = :serviceId")
    void deleteByServiceId(@Param("serviceId") Long serviceId);
}
