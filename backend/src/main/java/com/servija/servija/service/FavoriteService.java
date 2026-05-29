package com.servija.servija.service;

import com.servija.servija.dto.FavoriteRequest;
import com.servija.servija.dto.FavoriteResponse;
import com.servija.servija.dto.FreelancerSimpleResponse;
import com.servija.servija.model.Address;
import com.servija.servija.exception.ResourceNotFoundException;
import com.servija.servija.model.Favorite;
import com.servija.servija.model.Freelancer;
import com.servija.servija.model.User;
import com.servija.servija.repository.AddressRepository;
import com.servija.servija.repository.FavoriteRepository;
import com.servija.servija.repository.FreelancerRepository;
import com.servija.servija.repository.ServiceRepository;
import com.servija.servija.repository.UserRepository;
import com.servija.servija.util.PublicVisibilityRules;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final ServiceRepository serviceRepository;
    private final FreelancerRepository freelancerRepository;
    private final AddressRepository addressRepository;
    private final UserRepository userRepository;
    private final ServiceService serviceService;
    private final AuditLogger auditLogger;

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

        FavoriteResponse deletedFavorite = mapToVisibleResponse(favorite).orElse(null);
        favoriteRepository.delete(favorite);

        auditLogger.recordSemantic(
                "FAVORITE_REMOVED",
                "FAVORITE",
                String.valueOf(favoriteId),
                "Favorito " + favoriteId,
                com.servija.servija.model.AuditLog.Result.SUCCESS,
                null,
                deletedFavorite,
                null,
                null);
    }

    public List<FavoriteResponse> getUserFavorites(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        List<Favorite> favorites = favoriteRepository.findByUser(user);
        Map<Long, String> locationsByFreelancerId = loadLocationsByFreelancerId(favorites);

        return favorites
                .stream()
                .map(favorite -> mapToVisibleResponse(favorite, locationsByFreelancerId))
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
        return mapToVisibleResponse(favorite, Map.of());
    }

    private Optional<FavoriteResponse> mapToVisibleResponse(Favorite favorite, Map<Long, String> locationsByFreelancerId) {
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
            Long freelancerId = favorite.getFreelancer().getId();
            builder.freelancer(FreelancerSimpleResponse.builder()
                    .userId(freelancerId)
                    .name(favorite.getFreelancer().getName())
                    .professionalTitle(favorite.getFreelancer().getProfessionalTitle())
                    .category(favorite.getFreelancer().getCategory())
                    .avatarUrl(favorite.getFreelancer().getAvatarUrl())
                    .location(firstNonBlank(
                            locationsByFreelancerId.get(freelancerId),
                            resolveLocationFromAddress(freelancerId),
                            favorite.getFreelancer().getLocation()))
                    .build());
        }

        return Optional.of(builder.build());
    }

    private Map<Long, String> loadLocationsByFreelancerId(List<Favorite> favorites) {
        List<Long> freelancerIds = favorites.stream()
                .map(Favorite::getFreelancer)
                .filter(java.util.Objects::nonNull)
                .map(Freelancer::getId)
                .distinct()
                .toList();

        if (freelancerIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, String> locationsByFreelancerId = new LinkedHashMap<>();
        for (Address address : addressRepository.findByUserIdInOrderByUserIdAscIsDefaultDescCreatedAtDesc(freelancerIds)) {
            if (address.getUser() == null || address.getUser().getId() == null) {
                continue;
            }
            String location = formatAddressLocation(address);
            if (StringUtils.hasText(location)) {
                locationsByFreelancerId.putIfAbsent(address.getUser().getId(), location);
            }
        }
        return locationsByFreelancerId;
    }

    private String resolveLocationFromAddress(Long freelancerId) {
        return addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(freelancerId).stream()
                .map(this::formatAddressLocation)
                .filter(StringUtils::hasText)
                .findFirst()
                .orElse(null);
    }

    private String formatAddressLocation(Address address) {
        if (address == null) {
            return null;
        }

        String city = StringUtils.hasText(address.getCity()) ? address.getCity().trim() : null;
        String state = StringUtils.hasText(address.getState()) ? address.getState().trim() : null;

        if (city != null && state != null) {
            return city + ", " + state;
        }
        return firstNonBlank(city, state);
    }

    private String firstNonBlank(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }
}
