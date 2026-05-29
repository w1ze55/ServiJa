package com.servija.servija.repository;

import com.servija.servija.model.PlatformWallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

@Repository
public interface PlatformWalletRepository extends JpaRepository<PlatformWallet, Long> {

    Optional<PlatformWallet> findFirstByOrderByIdAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from PlatformWallet w order by w.id asc")
    List<PlatformWallet> lockAllOrderByIdAsc();
}
