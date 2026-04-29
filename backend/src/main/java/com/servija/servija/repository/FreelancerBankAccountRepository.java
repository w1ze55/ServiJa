package com.servija.servija.repository;

import com.servija.servija.model.FreelancerBankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FreelancerBankAccountRepository extends JpaRepository<FreelancerBankAccount, Long> {

    List<FreelancerBankAccount> findByUserIdOrderByIsDefaultDescCreatedAtDesc(Long userId);

    Optional<FreelancerBankAccount> findByUserIdAndIsDefaultTrue(Long userId);

    long countByUserId(Long userId);

    @Modifying
    @Query("UPDATE FreelancerBankAccount b SET b.isDefault = false WHERE b.user.id = :userId")
    void clearDefaultForUser(Long userId);
}
