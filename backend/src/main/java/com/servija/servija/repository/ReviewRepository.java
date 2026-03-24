package com.servija.servija.repository;

import com.servija.servija.model.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Long> {

    List<Review> findByReviewedIdOrderByCreatedAtDesc(Long reviewedId);

    Optional<Review> findByServiceRequestIdAndReviewerId(Long serviceRequestId, Long reviewerId);

    boolean existsByServiceRequestIdAndReviewerId(Long serviceRequestId, Long reviewerId);

    long countByReviewedId(Long reviewedId);

    @Query("SELECT AVG(r.rating) FROM Review r WHERE r.reviewed.id = :reviewedId")
    Double findAverageRatingByReviewedId(@Param("reviewedId") Long reviewedId);
}
