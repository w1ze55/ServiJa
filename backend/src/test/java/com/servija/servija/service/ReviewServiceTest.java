package com.servija.servija.service;

import com.servija.servija.dto.ReviewRequest;
import com.servija.servija.model.*;
import com.servija.servija.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;
    @Mock
    private ServiceRequestRepository serviceRequestRepository;
    @Mock
    private ServiceRepository serviceRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private FreelancerRepository freelancerRepository;
    @Mock
    private WebSocketService webSocketService;
    @Mock
    private ReviewSummaryRepository reviewSummaryRepository;

    @InjectMocks
    private ReviewService reviewService;

    @Test
    @DisplayName("createReview invalida cache de resumo quando summary existe para o freelancer")
    void createReview_invalidatesCacheWhenSummaryExists() {
        Freelancer freelancer = new Freelancer();
        freelancer.setId(10L);
        freelancer.setEmail("free@test.com");

        User contractor = new User();
        contractor.setId(2L);
        contractor.setEmail("client@test.com");

        ServiceRequest sr = new ServiceRequest();
        sr.setId(1L);
        sr.setStatus(ServiceRequest.Status.COMPLETED);
        sr.setFreelancer(freelancer);
        sr.setContractor(contractor);
        sr.setServiceName("Limpeza");

        ReviewRequest request = new ReviewRequest();
        request.setRating(5);
        request.setComment("Ótimo!");

        Review saved = new Review();
        saved.setId(100L);
        saved.setRating(5);
        saved.setReviewer(contractor);
        saved.setReviewed(freelancer);
        saved.setServiceRequest(sr);
        saved.setReviewerType(Review.ReviewerType.CONTRACTOR);

        ReviewSummary existingSummary = ReviewSummary.builder()
                .freelancer(freelancer)
                .summaryJson("{}")
                .reviewCount(3)
                .generatedAt(LocalDateTime.now().minusHours(1))
                .build();

        when(serviceRequestRepository.findById(1L)).thenReturn(Optional.of(sr));
        when(userRepository.findByEmail("client@test.com")).thenReturn(Optional.of(contractor));
        when(reviewRepository.existsByServiceRequestIdAndReviewerId(1L, 2L)).thenReturn(false);
        when(reviewRepository.save(any())).thenReturn(saved);
        when(reviewRepository.findAverageRatingByReviewedId(10L)).thenReturn(5.0);
        when(reviewRepository.countByReviewedId(10L)).thenReturn(4L);
        when(freelancerRepository.save(any())).thenReturn(freelancer);
        when(reviewSummaryRepository.findByFreelancerId(10L)).thenReturn(Optional.of(existingSummary));
        when(reviewSummaryRepository.save(any())).thenReturn(existingSummary);

        reviewService.createReview(1L, request, "client@test.com");

        verify(reviewSummaryRepository).findByFreelancerId(10L);
        verify(reviewSummaryRepository).save(any());
    }

    @Test
    @DisplayName("createReview não chama save no cache quando freelancer ainda não tem summary")
    void createReview_doesNotCallSaveWhenNoSummaryExists() {
        Freelancer freelancer = new Freelancer();
        freelancer.setId(10L);
        freelancer.setEmail("free@test.com");

        User contractor = new User();
        contractor.setId(2L);
        contractor.setEmail("client@test.com");

        ServiceRequest sr = new ServiceRequest();
        sr.setId(1L);
        sr.setStatus(ServiceRequest.Status.COMPLETED);
        sr.setFreelancer(freelancer);
        sr.setContractor(contractor);
        sr.setServiceName("Pintura");

        ReviewRequest request = new ReviewRequest();
        request.setRating(4);

        Review saved = new Review();
        saved.setId(101L);
        saved.setRating(4);
        saved.setReviewer(contractor);
        saved.setReviewed(freelancer);
        saved.setServiceRequest(sr);
        saved.setReviewerType(Review.ReviewerType.CONTRACTOR);

        when(serviceRequestRepository.findById(1L)).thenReturn(Optional.of(sr));
        when(userRepository.findByEmail("client@test.com")).thenReturn(Optional.of(contractor));
        when(reviewRepository.existsByServiceRequestIdAndReviewerId(1L, 2L)).thenReturn(false);
        when(reviewRepository.save(any())).thenReturn(saved);
        when(reviewRepository.findAverageRatingByReviewedId(10L)).thenReturn(4.0);
        when(reviewRepository.countByReviewedId(10L)).thenReturn(1L);
        when(freelancerRepository.save(any())).thenReturn(freelancer);
        when(reviewSummaryRepository.findByFreelancerId(10L)).thenReturn(Optional.empty());

        reviewService.createReview(1L, request, "client@test.com");

        verify(reviewSummaryRepository).findByFreelancerId(10L);
        verify(reviewSummaryRepository, never()).save(any());
    }
}
