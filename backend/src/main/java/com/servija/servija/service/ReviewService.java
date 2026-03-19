package com.servija.servija.service;

import com.servija.servija.dto.ReviewRequest;
import com.servija.servija.dto.ReviewResponse;
import com.servija.servija.exception.ResourceNotFoundException;
import com.servija.servija.model.Freelancer;
import com.servija.servija.model.Review;
import com.servija.servija.model.ServiceRequest;
import com.servija.servija.model.User;
import com.servija.servija.repository.FreelancerRepository;
import com.servija.servija.repository.ReviewRepository;
import com.servija.servija.repository.ServiceRequestRepository;
import com.servija.servija.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final UserRepository userRepository;
    private final FreelancerRepository freelancerRepository;
    private final WebSocketService webSocketService;

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

        User reviewed;
        Review.ReviewerType reviewerType;

        boolean isContractor = serviceRequest.getContractor().getId().equals(reviewer.getId());
        boolean isFreelancer = serviceRequest.getFreelancer() != null
                && serviceRequest.getFreelancer().getId().equals(reviewer.getId());

        if (isContractor) {
            reviewerType = Review.ReviewerType.CONTRACTOR;
            reviewed = serviceRequest.getFreelancer();
        } else if (isFreelancer) {
            reviewerType = Review.ReviewerType.FREELANCER;
            reviewed = serviceRequest.getContractor();
        } else {
            throw new IllegalStateException("Usuário não autorizado a avaliar este serviço");
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

        if (reviewerType == Review.ReviewerType.CONTRACTOR && reviewed instanceof Freelancer freelancer) {
            updateFreelancerRating(freelancer);
        }

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

    public boolean canReview(Long serviceRequestId, String userEmail) {
        ServiceRequest serviceRequest = serviceRequestRepository.findById(serviceRequestId)
                .orElseThrow(() -> new ResourceNotFoundException("Solicitação de serviço não encontrada"));

        if (serviceRequest.getStatus() != ServiceRequest.Status.COMPLETED) {
            return false;
        }

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado"));

        boolean isParticipant = serviceRequest.getContractor().getId().equals(user.getId())
                || (serviceRequest.getFreelancer() != null
                    && serviceRequest.getFreelancer().getId().equals(user.getId()));

        if (!isParticipant) {
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
