package com.servija.servija.service;

import com.servija.servija.dto.ReviewRequest;
import com.servija.servija.model.Freelancer;
import com.servija.servija.model.Review;
import com.servija.servija.model.ReviewSummary;
import com.servija.servija.model.ServiceRequest;
import com.servija.servija.model.User;
import com.servija.servija.repository.FreelancerRepository;
import com.servija.servija.repository.ReviewRepository;
import com.servija.servija.repository.ReviewSummaryRepository;
import com.servija.servija.repository.ServiceRepository;
import com.servija.servija.repository.ServiceRequestRepository;
import com.servija.servija.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock private ReviewRepository reviewRepository;
    @Mock private ServiceRequestRepository serviceRequestRepository;
    @Mock private ServiceRepository serviceRepository;
    @Mock private UserRepository userRepository;
    @Mock private FreelancerRepository freelancerRepository;
    @Mock private WebSocketService webSocketService;
    @Mock private ReviewSummaryRepository reviewSummaryRepository;

    @InjectMocks
    private ReviewService reviewService;

    @Test
    @DisplayName("createReview invalida cache de resumo quando summary existe para o freelancer")
    void createReview_invalidatesCacheWhenSummaryExists() {
        User freelancerUser = buildUser(10L, "free@test.com", User.UserType.FREELANCER);
        Freelancer freelancer = buildFreelancerProfile(freelancerUser);
        User contractor = buildUser(2L, "client@test.com", User.UserType.CONTRACTOR);

        ServiceRequest sr = new ServiceRequest();
        sr.setId(1L);
        sr.setStatus(ServiceRequest.Status.COMPLETED);
        sr.setFreelancer(freelancerUser);
        sr.setContractor(contractor);
        sr.setServiceName("Limpeza");

        ReviewRequest request = new ReviewRequest();
        request.setRating(5);
        request.setComment("Otimo!");

        Review saved = new Review();
        saved.setId(100L);
        saved.setRating(5);
        saved.setReviewer(contractor);
        saved.setReviewed(freelancerUser);
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
        when(freelancerRepository.findById(10L)).thenReturn(Optional.of(freelancer));
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
        User freelancerUser = buildUser(10L, "free@test.com", User.UserType.FREELANCER);
        Freelancer freelancer = buildFreelancerProfile(freelancerUser);
        User contractor = buildUser(2L, "client@test.com", User.UserType.CONTRACTOR);

        ServiceRequest sr = new ServiceRequest();
        sr.setId(1L);
        sr.setStatus(ServiceRequest.Status.COMPLETED);
        sr.setFreelancer(freelancerUser);
        sr.setContractor(contractor);
        sr.setServiceName("Pintura");

        ReviewRequest request = new ReviewRequest();
        request.setRating(4);

        Review saved = new Review();
        saved.setId(101L);
        saved.setRating(4);
        saved.setReviewer(contractor);
        saved.setReviewed(freelancerUser);
        saved.setServiceRequest(sr);
        saved.setReviewerType(Review.ReviewerType.CONTRACTOR);

        when(serviceRequestRepository.findById(1L)).thenReturn(Optional.of(sr));
        when(userRepository.findByEmail("client@test.com")).thenReturn(Optional.of(contractor));
        when(reviewRepository.existsByServiceRequestIdAndReviewerId(1L, 2L)).thenReturn(false);
        when(reviewRepository.save(any())).thenReturn(saved);
        when(reviewRepository.findAverageRatingByReviewedId(10L)).thenReturn(4.0);
        when(reviewRepository.countByReviewedId(10L)).thenReturn(1L);
        when(freelancerRepository.findById(10L)).thenReturn(Optional.of(freelancer));
        when(freelancerRepository.save(any())).thenReturn(freelancer);
        when(reviewSummaryRepository.findByFreelancerId(10L)).thenReturn(Optional.empty());

        reviewService.createReview(1L, request, "client@test.com");

        verify(reviewSummaryRepository).findByFreelancerId(10L);
        verify(reviewSummaryRepository, never()).save(any());
    }

    private User buildUser(Long id, String email, User.UserType role) {
        return User.builder()
                .id(id)
                .name(role == User.UserType.FREELANCER ? "Freela" : "Cliente")
                .email(email)
                .cpf("1234567890" + id)
                .phone("11999999999")
                .userType(role)
                .activeRole(role)
                .roles(Set.of(role))
                .active(true)
                .build();
    }

    private Freelancer buildFreelancerProfile(User user) {
        Freelancer freelancer = new Freelancer();
        freelancer.setId(user.getId());
        freelancer.setUser(user);
        freelancer.setAvailable(true);
        return freelancer;
    }
}
