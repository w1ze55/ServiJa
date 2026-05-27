package com.servija.servija.repository;

import com.servija.servija.model.User;
import com.servija.servija.model.UserTwoFactorBackupCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserTwoFactorBackupCodeRepository extends JpaRepository<UserTwoFactorBackupCode, Long> {

    List<UserTwoFactorBackupCode> findByUserAndUsedAtIsNull(User user);

    @Modifying
    @Query("DELETE FROM UserTwoFactorBackupCode b WHERE b.user.id = :userId")
    void deleteByUserId(@Param("userId") Long userId);
}
