package com.servija.servija.service;

import com.servija.servija.dto.FavoriteRequest;
import com.servija.servija.dto.FavoriteResponse;
import com.servija.servija.dto.FreelancerSimpleResponse;
import com.servija.servija.exception.ResourceNotFoundException;
import com.servija.servija.model.Favorite;
import com.servija.servija.model.Freelancer;
import com.servija.servija.model.User;
import com.servija.servija.repository.FavoriteRepository;
import com.servija.servija.repository.FreelancerRepository;
import com.servija.servija.repository.ServiceRepository;
import com.servija.servija.repository.UserRepository;
import com.servija.servija.util.PublicVisibilityRules;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final ServiceRepository serviceRepository;
    private final FreelancerRepository freelancerRepository;
    private final UserRepository userRepository;
    private final ServiceService serviceService;

    @Transactional
    public FavoriteResponse addFavorite(Long userId, FavoriteRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (request.getServiceId() != null) {
            return favoriteRepository.findByUserIdAndServiceId(userId, request.getServiceId())
                    .flatMap(this::mapToVisibleResponse)
                    .orElseGet(() -> {
                        com.servija.servija.model.Service service = serviceRepository.findById(request.getServiceId())
                                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado"));
                        if (!serviceService.isPubliclyVisible(service)) {
                            throw new ResourceNotFoundException("Serviço não encontrado");
                        }
                        return mapToVisibleResponse(
                                favoriteRepository.save(Favorite.builder().user(user).service(service).build()))
                                .orElseThrow(() -> new ResourceNotFoundException("Serviço não encontrado"));
                    });
        } else if (request.getFreelancerId() != null) {
            return favoriteRepository.findByUserIdAndFreelancerId(userId, request.getFreelancerId())
                    .flatMap(this::mapToVisibleResponse)
                    .orElseGet(() -> {
                        Freelancer freelancer = freelancerRepository.findById(request.getFreelancerId())
                                .orElseThrow(() -> new ResourceNotFoundException("Freelancer não encontrado"));
                        if (!PublicVisibilityRules.isFreelancerPublic(freelancer)) {
                            throw new ResourceNotFoundException("Freelancer não encontrado");
                        }
                        return mapToVisibleResponse(
                                favoriteRepository.save(Favorite.builder().user(user).freelancer(freelancer).build()))
                                .orElseThrow(() -> new ResourceNotFoundException("Freelancer não encontrado"));
                    });
        } else {
            throw new RuntimeException("Either serviceId or freelancerId must be provided");
        }

    }

    @Transactional
    public void removeFavorite(Long userId, Long favoriteId) {
        Favorite favorite = favoriteRepository.findById(favoriteId)
                .orElseThrow(() -> new RuntimeException("Favorite not found"));

        if (!favorite.getUser().getId().equals(userId)) {
            throw new RuntimeException("You don't have permission to remove this favorite");
        }

        favoriteRepository.delete(favorite);
    }

    public List<FavoriteResponse> getUserFavorites(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return favoriteRepository.findByUser(user)
                .stream()
                .map(this::mapToVisibleResponse)
                .flatMap(Optional::stream)
                .collect(Collectors.toList());
    }

    public boolean isFavorited(Long userId, Long serviceId, Long freelancerId) {
        if (serviceId != null) {
            return favoriteRepository.findByUserIdAndServiceId(userId, serviceId)
                    .flatMap(this::mapToVisibleResponse)
                    .isPresent();
        }
        if (freelancerId != null) {
            return favoriteRepository.findByUserIdAndFreelancerId(userId, freelancerId)
                    .flatMap(this::mapToVisibleResponse)
                    .isPresent();
        }
        return false;
    }

    private Optional<FavoriteResponse> mapToVisibleResponse(Favorite favorite) {
        FavoriteResponse.FavoriteResponseBuilder builder = FavoriteResponse.builder()
                .id(favorite.getId())
                .userId(favorite.getUser().getId())
                .createdAt(favorite.getCreatedAt());

        if (favorite.getService() != null) {
            if (!serviceService.isPubliclyVisible(favorite.getService())) {
                return Optional.empty();
            }
            builder.service(serviceService.toResponse(favorite.getService()));
        }

        if (favorite.getFreelancer() != null) {
            if (!PublicVisibilityRules.isFreelancerPublic(favorite.getFreelancer())) {
                return Optional.empty();
            }
            builder.freelancer(FreelancerSimpleResponse.builder()
                    .userId(favorite.getFreelancer().getId())
                    .name(favorite.getFreelancer().getName())
                    .professionalTitle(favorite.getFreelancer().getProfessionalTitle())
                    .category(favorite.getFreelancer().getCategory())
                    .avatarUrl(favorite.getFreelancer().getAvatarUrl())
                    .build());
        }

        return Optional.of(builder.build());
    }
}
