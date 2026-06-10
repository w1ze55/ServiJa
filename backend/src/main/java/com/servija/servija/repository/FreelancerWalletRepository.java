package com.servija.servija.repository;

import com.servija.servija.model.FreelancerWallet;
import com.servija.servija.model.User;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FreelancerWalletRepository extends JpaRepository<FreelancerWallet, Long> {

    Optional<FreelancerWallet> findByUser(User user);

    Optional<FreelancerWallet> findByUserId(Long userId);

    /**
     * H10: carrega a carteira com lock pessimista de escrita para serializar
     * saques concorrentes (evita TOCTOU / saque duplicado). Deve ser chamado
     * dentro de uma transação.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from FreelancerWallet w where w.user.id = :userId")
    Optional<FreelancerWallet> findByUserIdForUpdate(@Param("userId") Long userId);
}
