package com.servija.servija.repository;

import com.servija.servija.model.PlatformBankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlatformBankAccountRepository extends JpaRepository<PlatformBankAccount, Long> {

    List<PlatformBankAccount> findByOrderByIsDefaultDescCreatedAtDesc();

    long countBy();

    @Modifying
    @Query("update PlatformBankAccount b set b.isDefault = false")
    void clearDefault();
}
