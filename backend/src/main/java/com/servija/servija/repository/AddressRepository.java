package com.servija.servija.repository;

import com.servija.servija.model.Address;
import com.servija.servija.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AddressRepository extends JpaRepository<Address, Long> {
    
    List<Address> findByUserOrderByIsDefaultDescCreatedAtDesc(User user);
    
    List<Address> findByUserIdOrderByIsDefaultDescCreatedAtDesc(Long userId);

    List<Address> findByUserIdInOrderByUserIdAscIsDefaultDescCreatedAtDesc(List<Long> userIds);
    
    Optional<Address> findByIdAndUserId(Long id, Long userId);
    
    boolean existsByIdAndUserId(Long id, Long userId);
    
    @Modifying
    @Query("UPDATE Address a SET a.isDefault = false WHERE a.user.id = :userId AND a.id != :addressId")
    void clearDefaultForUser(@Param("userId") Long userId, @Param("addressId") Long addressId);
    
    @Query("SELECT COUNT(a) FROM Address a WHERE a.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);
}
