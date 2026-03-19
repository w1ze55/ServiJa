package com.servija.servija.controller;

import com.servija.servija.dto.ReviewRequest;
import com.servija.servija.dto.ReviewResponse;
import com.servija.servija.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reviews")
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/{serviceRequestId}")
    public ResponseEntity<ReviewResponse> createReview(
            @PathVariable Long serviceRequestId,
            @Valid @RequestBody ReviewRequest request,
            Authentication authentication) {
        String userEmail = authentication.getName();
        ReviewResponse response = reviewService.createReview(serviceRequestId, request, userEmail);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ReviewResponse>> getReviewsForUser(@PathVariable Long userId) {
        List<ReviewResponse> reviews = reviewService.getReviewsForUser(userId);
        return ResponseEntity.ok(reviews);
    }

    @GetMapping("/can-review/{serviceRequestId}")
    public ResponseEntity<Map<String, Boolean>> canReview(
            @PathVariable Long serviceRequestId,
            Authentication authentication) {
        String userEmail = authentication.getName();
        boolean canReview = reviewService.canReview(serviceRequestId, userEmail);
        return ResponseEntity.ok(Map.of("canReview", canReview));
    }

    @GetMapping("/my-review/{serviceRequestId}")
    public ResponseEntity<ReviewResponse> getMyReview(
            @PathVariable Long serviceRequestId,
            Authentication authentication) {
        String userEmail = authentication.getName();
        ReviewResponse review = reviewService.getMyReview(serviceRequestId, userEmail);
        if (review == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(review);
    }
}
