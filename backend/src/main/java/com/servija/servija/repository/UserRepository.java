package com.servija.servija.repository;

import com.servija.servija.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    Optional<User> findByEmail(String email);

    Optional<User> findByCpf(String cpf);

    boolean existsByEmail(String email);

    boolean existsByCpf(String cpf);

    Optional<User> findByEmailAndEmailVerifiedTrue(String email);

    Optional<User> findByAdminResetPasswordTokenHash(String adminResetPasswordTokenHash);

    @Query("SELECT COUNT(u) > 0 FROM User u WHERE u.email = :email AND u.id <> :excludeId")
    boolean existsByEmailAndIdNot(@Param("email") String email, @Param("excludeId") Long excludeId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE users SET user_type = :userType WHERE id = :id", nativeQuery = true)
    void updateUserTypeNative(@Param("id") Long id, @Param("userType") String userType);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "INSERT IGNORE INTO freelancers (user_id, kyc_completed, profile_completed, available, total_reviews, completed_jobs, active_jobs) VALUES (:userId, false, false, true, 0, 0, 0)", nativeQuery = true)
    void insertFreelancerRecordIfNotExists(@Param("userId") Long userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "INSERT IGNORE INTO contractors (user_id, total_contracts, active_contracts, completed_contracts) VALUES (:userId, 0, 0, 0)", nativeQuery = true)
    void insertContractorRecordIfNotExists(@Param("userId") Long userId);
}
