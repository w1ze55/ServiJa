package com.servija.servija.service;

import com.servija.servija.dto.PendingReviewResponse;
import com.servija.servija.dto.ReviewRequest;
import com.servija.servija.dto.ReviewResponse;
import com.servija.servija.exception.ResourceNotFoundException;
import com.servija.servija.model.Freelancer;
import com.servija.servija.model.Review;
import com.servija.servija.model.ServiceRequest;
import com.servija.servija.model.User;
import com.servija.servija.repository.FreelancerRepository;
import com.servija.servija.repository.ReviewRepository;
import com.servija.servija.repository.ReviewSummaryRepository;
import com.servija.servija.repository.ServiceRepository;
import com.servija.servija.repository.ServiceRequestRepository;
import com.servija.servija.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final ServiceRepository serviceRepository;
    private final UserRepository userRepository;
    private final FreelancerRepository freelancerRepository;
    private final WebSocketService webSocketService;
    private final ReviewSummaryRepository reviewSummaryRepository;

    @Transactional
    public ReviewResponse createReview(Long serviceRequestId, ReviewRequest request, String userEmail) {
        ServiceRequest serviceRequest = serviceRequestRepository.findById(serviceRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação de serviço não encontrada"));

        if (serviceRequest.getStatus() != ServiceRequest.Status.COMPLETED) {
            throw new IllegalStateException("Só é possível avaliar um serviço concluído");
        }

        User reviewer = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        if (reviewRepository.existsByServiceRequestIdAndReviewerId(serviceRequestId, reviewer.getId())) {
            throw new IllegalStateException("Você já avaliou este serviço");
        }

        boolean isContractor = serviceRequest.getContractor().getId().equals(reviewer.getId());

        if (!isContractor) {
            throw new IllegalStateException("Apenas o cliente pode avaliar o freelancer");
        }

        Review.ReviewerType reviewerType = Review.ReviewerType.CONTRACTOR;
        User reviewed = serviceRequest.getFreelancer();

        if (reviewed != null && reviewed.getId().equals(reviewer.getId())) {
            throw new IllegalStateException("Voce nao pode avaliar a si mesmo");
        }

        String tagsString = request.getTags() != null && !request.getTags().isEmpty()
                ? String.join(",", request.getTags())
                : null;

        Review review = Review.builder()
                .serviceRequest(serviceRequest)
                .reviewer(reviewer)
                .reviewed(reviewed)
                .reviewerType(reviewerType)
                .rating(request.getRating())
                .comment(request.getComment())
                .tags(tagsString)
                .build();

        Review saved = reviewRepository.save(review);

        if (reviewerType == Review.ReviewerType.CONTRACTOR && reviewed != null) {
            freelancerRepository.findById(reviewed.getId()).ifPresent(this::updateFreelancerRating);
        }

        reviewSummaryRepository.findByFreelancerId(reviewed.getId())
                .ifPresent(summary -> {
                    summary.setGeneratedAt(LocalDateTime.now().minusDays(2));
                    reviewSummaryRepository.save(summary);
                });

        webSocketService.notifyNewReview(
                serviceRequestId,
                saved.getId(),
                reviewed.getEmail(),
                reviewer.getName(),
                saved.getRating()
        );

        log.info("Avaliação criada - ID: {}, Serviço: {}, Avaliador: {}, Nota: {}",
                saved.getId(), serviceRequestId, userEmail, saved.getRating());

        return toResponse(saved);
    }

    public List<ReviewResponse> getReviewsForUser(Long userId) {
        List<Review> reviews = reviewRepository.findByReviewedIdOrderByCreatedAtDesc(userId);
        return reviews.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<ReviewResponse> getReviewsByServiceId(Long serviceId) {
        List<Review> reviews = reviewRepository.findByServiceId(serviceId);

        if (reviews.isEmpty()) {
            com.servija.servija.model.Service service = serviceRepository.findById(serviceId).orElse(null);
            if (service != null) {
                reviews = reviewRepository.findByFreelancerIdAndServiceName(
                        service.getFreelancer().getId(), service.getTitle());
            }
        }

        return reviews.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public boolean canReview(Long serviceRequestId, String userEmail) {
        ServiceRequest serviceRequest = serviceRequestRepository.findById(serviceRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação de serviço não encontrada"));

        if (serviceRequest.getStatus() != ServiceRequest.Status.COMPLETED) {
            return false;
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        boolean isContractor = serviceRequest.getContractor().getId().equals(user.getId());
        if (!isContractor) {
            return false;
        }

        return !reviewRepository.existsByServiceRequestIdAndReviewerId(serviceRequestId, user.getId());
    }

    public ReviewResponse getMyReview(Long serviceRequestId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        return reviewRepository.findByServiceRequestIdAndReviewerId(serviceRequestId, user.getId())
                .map(this::toResponse)
                .orElse(null);
    }

    public List<PendingReviewResponse> getPendingReviews(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        if (user.getUserType() != User.UserType.CONTRACTOR) {
            return Collections.emptyList();
        }

        List<ServiceRequest> completedRequests = serviceRequestRepository.findByContractorIdAndStatus(
                user.getId(), ServiceRequest.Status.COMPLETED);

        return completedRequests.stream()
                .filter(sr -> !reviewRepository.existsByServiceRequestIdAndReviewerId(sr.getId(), user.getId()))
                .map(sr -> {
                    User target = sr.getFreelancer();

                    return PendingReviewResponse.builder()
                            .serviceRequestId(sr.getId())
                            .serviceName(sr.getServiceName())
                            .targetName(target != null ? target.getName() : "Usuário")
                            .targetAvatar(target != null ? target.getAvatarUrl() : null)
                            .targetType("FREELANCER")
                            .completedAt(sr.getUpdatedAt())
                            .build();
                })
                .collect(Collectors.toList());
    }

    private void updateFreelancerRating(Freelancer freelancer) {
        Double avgRating = reviewRepository.findAverageRatingByReviewedId(freelancer.getId());
        long totalReviews = reviewRepository.countByReviewedId(freelancer.getId());

        if (avgRating != null) {
            freelancer.setRating(BigDecimal.valueOf(avgRating).setScale(2, RoundingMode.HALF_UP));
        }
        freelancer.setTotalReviews((int) totalReviews);
        freelancerRepository.save(freelancer);

        log.info("Rating do freelancer {} atualizado: {} ({} avaliações)",
                freelancer.getId(), avgRating, totalReviews);
    }

    private ReviewResponse toResponse(Review review) {
        List<String> tagsList = review.getTags() != null && !review.getTags().isEmpty()
                ? Arrays.asList(review.getTags().split(","))
                : Collections.emptyList();

        return ReviewResponse.builder()
                .id(review.getId())
                .serviceRequestId(review.getServiceRequest().getId())
                .serviceName(review.getServiceRequest().getServiceName())
                .reviewerName(review.getReviewer().getName())
                .reviewerAvatar(review.getReviewer().getAvatarUrl())
                .reviewerType(review.getReviewerType().name())
                .rating(review.getRating())
                .comment(review.getComment())
                .tags(tagsList)
                .createdAt(review.getCreatedAt())
                .build();
    }
}
