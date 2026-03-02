package com.servija.servija.repository;

import com.servija.servija.model.FreelancerWallet;
import com.servija.servija.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FreelancerWalletRepository extends JpaRepository<FreelancerWallet, Long> {
    
    Optional<FreelancerWallet> findByUser(User user);
    
    Optional<FreelancerWallet> findByUserId(Long userId);
}
