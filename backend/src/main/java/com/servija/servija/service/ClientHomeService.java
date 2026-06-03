package com.servija.servija.service;

import com.servija.servija.dto.ClientHomeResponse;
import com.servija.servija.dto.FavoriteResponse;
import com.servija.servija.dto.NearbyFreelancerResponse;
import com.servija.servija.dto.ServiceRequestResponse;
import com.servija.servija.dto.ServiceResponse;
import com.servija.servija.model.Address;
import com.servija.servija.model.Freelancer;
import com.servija.servija.model.Review;
import com.servija.servija.model.ServiceCity;
import com.servija.servija.model.User;
import com.servija.servija.repository.AddressRepository;
import com.servija.servija.repository.FreelancerRepository;
import com.servija.servija.repository.ReviewRepository;
import com.servija.servija.repository.UserRepository;
import com.servija.servija.util.PublicVisibilityRules;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ClientHomeService {

    private static final int RECENT_REQUEST_LIMIT = 3;
    private static final int ACTIVE_PROJECT_LIMIT = 3;
    private static final int FAVORITES_PREVIEW_LIMIT = 4;
    private static final int FEATURED_LIMIT = 6;
    private static final int INTEREST_CATEGORIES_LIMIT = 5;
    private static final long RECENT_REVIEW_WINDOW_DAYS = 90;

    private static final Set<String> RECENT_REQUEST_STATUSES = Set.of(
            "PENDING",
            "SENT",
            "NEGOTIATING",
            "REJECTED",
            "CANCELLED");

    private static final Set<String> ACTIVE_PROJECT_STATUSES = Set.of(
            "ACCEPTED",
            "IN_PROGRESS",
            "DELIVERED");

    private static final Set<String> PREFERRED_INTEREST_STATUSES = Set.of(
            "ACCEPTED",
            "IN_PROGRESS",
            "DELIVERED",
            "COMPLETED");

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final FreelancerRepository freelancerRepository;
    private final ReviewRepository reviewRepository;
    private final ServiceRequestService serviceRequestService;
    private final ServiceService serviceService;
    private final FavoriteService favoriteService;
    private final ServiceSearchWeeklyMetricService serviceSearchWeeklyMetricService;

    @Transactional(readOnly = true)
    public ClientHomeResponse getHome() {
        User contractor = getCurrentContractor();
        List<ServiceRequestResponse> requests = serviceRequestService.getByContractorEmail(contractor.getEmail());

        Address primaryAddress = addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(contractor.getId())
                .stream()
                .findFirst()
                .orElse(null);

        ClientHomeResponse.RegionInfo region = buildRegion(primaryAddress);
        List<String> interestCategories = deriveInterestCategories(requests);
        Set<String> normalizedInterestCategories = interestCategories.stream()
                .map(this::normalizeText)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        List<FavoriteResponse> favorites = favoriteService.getUserFavorites(contractor.getId());
        List<ServiceResponse> activeServices = serviceService.getActiveServices(null, null, null);
        Metadata metadata = buildMetadata(activeServices, favorites);

        Map<Long, RankingStats> serviceRankingCache = new HashMap<>();
        Map<Long, RankingStats> freelancerRankingCache = new HashMap<>();

        return ClientHomeResponse.builder()
                .region(region)
                .interestCategories(interestCategories)
                .recentRequests(requests.stream()
                        .filter(request -> RECENT_REQUEST_STATUSES.contains(normalizeStatus(request.getStatus())))
                        .limit(RECENT_REQUEST_LIMIT)
                        .toList())
                .activeProjects(requests.stream()
                        .filter(request -> ACTIVE_PROJECT_STATUSES.contains(normalizeStatus(request.getStatus())))
                        .limit(ACTIVE_PROJECT_LIMIT)
                        .toList())
                .favorites(buildFavoritesPreview(
                        favorites,
                        metadata,
                        serviceRankingCache,
                        freelancerRankingCache))
                .weeklyMostSearchedServices(selectWeeklyMostSearchedServices(metadata))
                .featuredProfessionals(selectFeaturedProfessionals(
                        region,
                        normalizedInterestCategories,
                        metadata,
                        freelancerRankingCache))
                .featuredServices(selectFeaturedServices(
                        region,
                        normalizedInterestCategories,
                        metadata,
                        serviceRankingCache))
                .build();
    }

    private List<ServiceResponse> selectWeeklyMostSearchedServices(Metadata metadata) {
        return serviceSearchWeeklyMetricService.getTopServiceIdsForCurrentWeek(FEATURED_LIMIT).stream()
                .map(metadata.activeServicesById()::get)
                .filter(Objects::nonNull)
                .limit(FEATURED_LIMIT)
                .toList();
    }

    protected User getCurrentContractor() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        if (user.getUserType() != User.UserType.CONTRACTOR) {
            throw new IllegalStateException("Usuário não é um contratante");
        }

        return user;
    }

    private ClientHomeResponse.RegionInfo buildRegion(Address address) {
        if (address == null || !StringUtils.hasText(address.getCity()) || !StringUtils.hasText(address.getState())) {
            return ClientHomeResponse.RegionInfo.builder()
                    .displayName("toda a plataforma")
                    .localized(false)
                    .build();
        }

        return ClientHomeResponse.RegionInfo.builder()
                .city(address.getCity())
                .state(address.getState())
                .displayName(address.getCity() + ", " + address.getState())
                .localized(true)
                .build();
    }

    private List<String> deriveInterestCategories(List<ServiceRequestResponse> requests) {
        LinkedHashSet<String> categories = collectInterestCategories(requests, PREFERRED_INTEREST_STATUSES);
        if (categories.isEmpty()) {
            categories = collectInterestCategories(requests, null);
        }

        return categories.stream()
                .limit(INTEREST_CATEGORIES_LIMIT)
                .toList();
    }

    private LinkedHashSet<String> collectInterestCategories(
            List<ServiceRequestResponse> requests,
            Set<String> allowedStatuses) {
        LinkedHashSet<String> categories = new LinkedHashSet<>();

        for (ServiceRequestResponse request : requests) {
            if (allowedStatuses != null && !allowedStatuses.contains(normalizeStatus(request.getStatus()))) {
                continue;
            }

            String primaryInterest = firstNonBlank(
                    request.getSubcategoryLabel(),
                    request.getSubcategory(),
                    request.getCategoryLabel(),
                    request.getCategory());

            if (StringUtils.hasText(primaryInterest)) {
                categories.add(primaryInterest.trim());
            }

            if (categories.size() >= INTEREST_CATEGORIES_LIMIT) {
                break;
            }
        }

        return categories;
    }

    private Metadata buildMetadata(List<ServiceResponse> activeServices, List<FavoriteResponse> favorites) {
        Map<Long, List<ServiceResponse>> activeServicesByFreelancerId = activeServices.stream()
                .filter(service -> service.getFreelancerId() != null)
                .collect(Collectors.groupingBy(
                        ServiceResponse::getFreelancerId,
                        LinkedHashMap::new,
                        Collectors.toList()));

        Map<Long, ServiceResponse> activeServicesById = activeServices.stream()
                .collect(Collectors.toMap(
                        ServiceResponse::getId,
                        service -> service,
                        (left, right) -> left,
                        LinkedHashMap::new));

        LinkedHashSet<Long> freelancerIds = new LinkedHashSet<>(activeServicesByFreelancerId.keySet());
        favorites.stream()
                .map(FavoriteResponse::getFreelancer)
                .filter(Objects::nonNull)
                .map(freelancer -> freelancer.getUserId())
                .filter(Objects::nonNull)
                .forEach(freelancerIds::add);

        if (freelancerIds.isEmpty()) {
            return new Metadata(
                    Map.of(),
                    Map.of(),
                    Map.of(),
                    activeServicesByFreelancerId,
                    activeServicesById);
        }

        List<Long> freelancerIdList = new ArrayList<>(freelancerIds);
        List<Freelancer> freelancers = freelancerRepository.findAllByIdInWithServiceCities(freelancerIdList).stream()
                .filter(PublicVisibilityRules::isFreelancerPublic)
                .toList();
        Map<Long, Freelancer> freelancersById = freelancers.stream()
                .collect(Collectors.toMap(Freelancer::getId, freelancer -> freelancer));

        Map<Long, Address> primaryAddressesByUserId = indexPrimaryAddresses(
                addressRepository.findByUserIdInOrderByUserIdAscIsDefaultDescCreatedAtDesc(freelancerIdList));

        Map<Long, List<ServiceCity>> serviceCitiesByFreelancerId = freelancers.stream()
                .collect(Collectors.toMap(
                        Freelancer::getId,
                        freelancer -> freelancer.getServiceCities() == null
                                ? List.of()
                                : List.copyOf(freelancer.getServiceCities())));

        return new Metadata(
                freelancersById,
                primaryAddressesByUserId,
                serviceCitiesByFreelancerId,
                activeServicesByFreelancerId,
                activeServicesById);
    }

    private ClientHomeResponse.FavoritesInfo buildFavoritesPreview(
            List<FavoriteResponse> favorites,
            Metadata metadata,
            Map<Long, RankingStats> serviceRankingCache,
            Map<Long, RankingStats> freelancerRankingCache) {
        List<FavoriteResponse> sortedFavorites = favorites.stream()
                .sorted(Comparator.comparing(FavoriteResponse::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        List<ServiceResponse> favoriteServices = sortedFavorites.stream()
                .map(FavoriteResponse::getService)
                .filter(Objects::nonNull)
                .map(service -> metadata.activeServicesById().getOrDefault(service.getId(), service))
                .limit(FAVORITES_PREVIEW_LIMIT)
                .toList();

        List<NearbyFreelancerResponse> favoriteProfessionals = sortedFavorites.stream()
                .map(FavoriteResponse::getFreelancer)
                .filter(Objects::nonNull)
                .map(freelancer -> buildFreelancerPreview(
                        freelancer.getUserId(),
                        metadata,
                        getFreelancerRankingStats(freelancer.getUserId(), freelancerRankingCache)))
                .filter(Objects::nonNull)
                .limit(FAVORITES_PREVIEW_LIMIT)
                .toList();

        return ClientHomeResponse.FavoritesInfo.builder()
                .serviceCount((int) favorites.stream().filter(favorite -> favorite.getService() != null).count())
                .professionalCount((int) favorites.stream().filter(favorite -> favorite.getFreelancer() != null).count())
                .services(favoriteServices)
                .professionals(favoriteProfessionals)
                .build();
    }

    private List<ServiceResponse> selectFeaturedServices(
            ClientHomeResponse.RegionInfo region,
            Set<String> normalizedInterestCategories,
            Metadata metadata,
            Map<Long, RankingStats> serviceRankingCache) {
        List<ServiceResponse> availableServices = metadata.activeServicesById().values().stream()
                .filter(service -> isAvailableFreelancer(service.getFreelancerId(), metadata))
                .toList();

        return selectUniqueRankedItems(
                buildPools(
                        availableServices,
                        service -> matchesRegion(service.getFreelancerId(), region, metadata),
                        service -> matchesInterest(service, normalizedInterestCategories)),
                service -> service.getId(),
                services -> rankServices(services, serviceRankingCache),
                FEATURED_LIMIT);
    }

    private List<NearbyFreelancerResponse> selectFeaturedProfessionals(
            ClientHomeResponse.RegionInfo region,
            Set<String> normalizedInterestCategories,
            Metadata metadata,
            Map<Long, RankingStats> freelancerRankingCache) {
        List<Long> availableFreelancerIds = metadata.activeServicesByFreelancerId().keySet().stream()
                .filter(freelancerId -> isAvailableFreelancer(freelancerId, metadata))
                .toList();

        List<Long> selectedFreelancerIds = selectUniqueRankedItems(
                buildPools(
                        availableFreelancerIds,
                        freelancerId -> matchesRegion(freelancerId, region, metadata),
                        freelancerId -> matchesInterest(freelancerId, normalizedInterestCategories, metadata)),
                freelancerId -> freelancerId,
                freelancerIds -> rankFreelancerIds(freelancerIds, freelancerRankingCache),
                FEATURED_LIMIT);

        return selectedFreelancerIds.stream()
                .map(freelancerId -> buildFreelancerPreview(
                        freelancerId,
                        metadata,
                        getFreelancerRankingStats(freelancerId, freelancerRankingCache)))
                .filter(Objects::nonNull)
                .toList();
    }

    private <T, K> List<T> selectUniqueRankedItems(
            List<List<T>> pools,
            java.util.function.Function<T, K> keyExtractor,
            java.util.function.Function<List<T>, List<T>> ranker,
            int limit) {
        LinkedHashMap<K, T> uniqueItems = new LinkedHashMap<>();

        for (List<T> pool : pools) {
            for (T item : ranker.apply(pool)) {
                uniqueItems.putIfAbsent(keyExtractor.apply(item), item);
                if (uniqueItems.size() >= limit) {
                    return new ArrayList<>(uniqueItems.values());
                }
            }
        }

        return new ArrayList<>(uniqueItems.values());
    }

    private <T> List<List<T>> buildPools(
            Collection<T> source,
            Predicate<T> regionPredicate,
            Predicate<T> interestPredicate) {
        List<T> sourceList = new ArrayList<>(source);
        boolean hasInterest = interestPredicate != null;
        List<List<T>> pools = new ArrayList<>();

        List<T> regionPreferred = sourceList.stream()
                .filter(regionPredicate)
                .filter(item -> !hasInterest || interestPredicate.test(item))
                .toList();
        pools.add(regionPreferred);

        if (hasInterest) {
            List<T> regionalFallback = sourceList.stream()
                    .filter(regionPredicate)
                    .toList();
            List<T> globalPreferred = sourceList.stream()
                    .filter(interestPredicate)
                    .toList();
            pools.add(regionalFallback);
            pools.add(globalPreferred);
        }

        pools.add(sourceList);
        return pools;
    }

    private List<ServiceResponse> rankServices(
            List<ServiceResponse> services,
            Map<Long, RankingStats> serviceRankingCache) {
        List<ServiceResponse> rankedServices = new ArrayList<>(services);
        rankedServices.sort(Comparator
                .comparing((ServiceResponse service) -> getServiceRankingStats(service, serviceRankingCache).hasRecentReviews())
                .reversed()
                .thenComparing(service -> getServiceRankingStats(service, serviceRankingCache).getRecentAverage(),
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(service -> getServiceRankingStats(service, serviceRankingCache).getRecentCount(),
                        Comparator.reverseOrder())
                .thenComparing(service -> getServiceRankingStats(service, serviceRankingCache).getLatestRecentReviewAt(),
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(service -> getServiceRankingStats(service, serviceRankingCache).getLifetimeAverage(),
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(service -> getServiceRankingStats(service, serviceRankingCache).getLifetimeCount(),
                        Comparator.reverseOrder())
                .thenComparing(ServiceResponse::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ServiceResponse::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));
        return rankedServices;
    }

    private List<Long> rankFreelancerIds(
            List<Long> freelancerIds,
            Map<Long, RankingStats> freelancerRankingCache) {
        List<Long> rankedFreelancerIds = new ArrayList<>(freelancerIds);
        rankedFreelancerIds.sort(Comparator
                .comparing((Long freelancerId) -> getFreelancerRankingStats(freelancerId, freelancerRankingCache).hasRecentReviews())
                .reversed()
                .thenComparing(freelancerId -> getFreelancerRankingStats(freelancerId, freelancerRankingCache).getRecentAverage(),
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(freelancerId -> getFreelancerRankingStats(freelancerId, freelancerRankingCache).getRecentCount(),
                        Comparator.reverseOrder())
                .thenComparing(freelancerId -> getFreelancerRankingStats(freelancerId, freelancerRankingCache).getLatestRecentReviewAt(),
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(freelancerId -> getFreelancerRankingStats(freelancerId, freelancerRankingCache).getLifetimeAverage(),
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(freelancerId -> getFreelancerRankingStats(freelancerId, freelancerRankingCache).getLifetimeCount(),
                        Comparator.reverseOrder()));
        return rankedFreelancerIds;
    }

    private NearbyFreelancerResponse buildFreelancerPreview(
            Long freelancerId,
            Metadata metadata,
            RankingStats rankingStats) {
        Freelancer freelancer = metadata.freelancersById().get(freelancerId);
        if (!PublicVisibilityRules.isFreelancerPublic(freelancer)) {
            return null;
        }

        BigDecimal rating = rankingStats.getLifetimeAverage();
        if (rating == null) {
            rating = freelancer.getRating() != null
                    ? freelancer.getRating().setScale(1, RoundingMode.HALF_UP)
                    : BigDecimal.ZERO.setScale(1, RoundingMode.HALF_UP);
        }

        long totalReviews = Math.max(
                rankingStats.getLifetimeCount(),
                freelancer.getTotalReviews() != null ? freelancer.getTotalReviews().longValue() : 0L);

        return NearbyFreelancerResponse.builder()
                .id(freelancer.getId())
                .name(freelancer.getName())
                .avatar(freelancer.getAvatarUrl())
                .professionalTitle(freelancer.getProfessionalTitle())
                .rating(rating)
                .totalReviews((int) totalReviews)
                .location(resolveFreelancerLocation(freelancerId, metadata))
                .latitude(freelancer.getLatitude())
                .longitude(freelancer.getLongitude())
                .available(Boolean.TRUE.equals(freelancer.getAvailable()))
                .category(resolveFreelancerCategory(freelancerId, metadata))
                .build();
    }

    private RankingStats getServiceRankingStats(
            ServiceResponse service,
            Map<Long, RankingStats> serviceRankingCache) {
        return serviceRankingCache.computeIfAbsent(service.getId(), ignored -> {
            List<Review> reviews = reviewRepository.findByServiceId(service.getId());
            return buildRankingStats(
                    reviews,
                    service.getServiceRating(),
                    service.getServiceReviewCount() != null ? service.getServiceReviewCount().longValue() : 0L);
        });
    }

    private RankingStats getFreelancerRankingStats(
            Long freelancerId,
            Map<Long, RankingStats> freelancerRankingCache) {
        return freelancerRankingCache.computeIfAbsent(freelancerId, ignored -> {
            Freelancer freelancer = freelancerRepository.findById(freelancerId).orElse(null);
            BigDecimal fallbackAverage = freelancer != null && freelancer.getRating() != null
                    ? freelancer.getRating().setScale(1, RoundingMode.HALF_UP)
                    : null;
            long fallbackCount = freelancer != null && freelancer.getTotalReviews() != null
                    ? freelancer.getTotalReviews().longValue()
                    : 0L;

            List<Review> reviews = reviewRepository.findByReviewedIdOrderByCreatedAtDesc(freelancerId);
            return buildRankingStats(reviews, fallbackAverage, fallbackCount);
        });
    }

    private RankingStats buildRankingStats(
            List<Review> reviews,
            BigDecimal fallbackAverage,
            long fallbackCount) {
        LocalDateTime recentCutoff = LocalDateTime.now().minus(RECENT_REVIEW_WINDOW_DAYS, ChronoUnit.DAYS);
        List<Review> recentReviews = reviews.stream()
                .filter(review -> review.getCreatedAt() != null && !review.getCreatedAt().isBefore(recentCutoff))
                .toList();

        BigDecimal recentAverage = calculateAverageRating(recentReviews);
        BigDecimal lifetimeAverage = calculateAverageRating(reviews);
        long lifetimeCount = reviews.size();

        return new RankingStats(
                recentAverage,
                (long) recentReviews.size(),
                recentReviews.stream()
                        .map(Review::getCreatedAt)
                        .filter(Objects::nonNull)
                        .max(LocalDateTime::compareTo)
                        .orElse(null),
                lifetimeAverage != null ? lifetimeAverage : fallbackAverage,
                lifetimeCount > 0 ? lifetimeCount : fallbackCount);
    }

    private BigDecimal calculateAverageRating(List<Review> reviews) {
        if (reviews == null || reviews.isEmpty()) {
            return null;
        }

        double average = reviews.stream()
                .map(Review::getRating)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);

        return BigDecimal.valueOf(average).setScale(1, RoundingMode.HALF_UP);
    }

    private boolean matchesInterest(
            Long freelancerId,
            Set<String> normalizedInterestCategories,
            Metadata metadata) {
        if (normalizedInterestCategories == null || normalizedInterestCategories.isEmpty()) {
            return true;
        }

        return metadata.activeServicesByFreelancerId().getOrDefault(freelancerId, List.of()).stream()
                .anyMatch(service -> matchesInterest(service, normalizedInterestCategories));
    }

    private boolean matchesInterest(
            ServiceResponse service,
            Set<String> normalizedInterestCategories) {
        if (normalizedInterestCategories == null || normalizedInterestCategories.isEmpty()) {
            return true;
        }

        return List.of(
                        service.getSubcategoryLabel(),
                        service.getSubcategory(),
                        service.getCategoryLabel(),
                        service.getCategory())
                .stream()
                .filter(StringUtils::hasText)
                .map(this::normalizeText)
                .anyMatch(normalizedInterestCategories::contains);
    }

    private boolean matchesRegion(
            Long freelancerId,
            ClientHomeResponse.RegionInfo region,
            Metadata metadata) {
        if (region == null || !Boolean.TRUE.equals(region.getLocalized())) {
            return true;
        }

        String normalizedRegionCity = normalizeText(region.getCity());
        String normalizedRegionState = normalizeState(region.getState());

        Address defaultAddress = metadata.primaryAddressesByUserId().get(freelancerId);
        if (defaultAddress != null
                && normalizedRegionCity.equals(normalizeText(defaultAddress.getCity()))
                && normalizedRegionState.equals(normalizeState(defaultAddress.getState()))) {
            return true;
        }

        return metadata.serviceCitiesByFreelancerId().getOrDefault(freelancerId, List.of()).stream()
                .anyMatch(serviceCity -> normalizedRegionCity.equals(normalizeText(serviceCity.getCity()))
                        && normalizedRegionState.equals(normalizeState(serviceCity.getState())));
    }

    private boolean isAvailableFreelancer(Long freelancerId, Metadata metadata) {
        Freelancer freelancer = metadata.freelancersById().get(freelancerId);
        return PublicVisibilityRules.isFreelancerPublic(freelancer)
                && Boolean.TRUE.equals(freelancer.getAvailable());
    }

    private String resolveFreelancerLocation(Long freelancerId, Metadata metadata) {
        Address address = metadata.primaryAddressesByUserId().get(freelancerId);
        if (address != null && StringUtils.hasText(address.getCity()) && StringUtils.hasText(address.getState())) {
            return address.getCity() + ", " + address.getState();
        }

        List<ServiceCity> serviceCities = metadata.serviceCitiesByFreelancerId().getOrDefault(freelancerId, List.of());
        if (!serviceCities.isEmpty()) {
            ServiceCity firstCity = serviceCities.get(0);
            return firstCity.getCity() + ", " + firstCity.getState();
        }

        Freelancer freelancer = metadata.freelancersById().get(freelancerId);
        return freelancer != null ? freelancer.getLocation() : null;
    }

    private String resolveFreelancerCategory(Long freelancerId, Metadata metadata) {
        List<ServiceResponse> services = metadata.activeServicesByFreelancerId().getOrDefault(freelancerId, List.of());
        if (!services.isEmpty()) {
            return firstNonBlank(
                    services.get(0).getCategoryLabel(),
                    services.get(0).getCategory());
        }

        Freelancer freelancer = metadata.freelancersById().get(freelancerId);
        if (freelancer == null) {
            return null;
        }

        return firstNonBlank(
                freelancer.getServiceCategory() != null ? freelancer.getServiceCategory().getName() : null,
                freelancer.getCategory(),
                freelancer.getServiceType());
    }

    private Map<Long, Address> indexPrimaryAddresses(List<Address> addresses) {
        Map<Long, Address> primaryAddresses = new LinkedHashMap<>();
        for (Address address : addresses) {
            if (address.getUser() != null && address.getUser().getId() != null) {
                primaryAddresses.putIfAbsent(address.getUser().getId(), address);
            }
        }
        return primaryAddresses;
    }

    private String normalizeStatus(String status) {
        return status == null ? "" : status.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeState(String state) {
        return state == null ? "" : state.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeText(String value) {
        if (value == null) {
            return "";
        }

        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .trim()
                .toLowerCase(Locale.ROOT);
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

    private record Metadata(
            Map<Long, Freelancer> freelancersById,
            Map<Long, Address> primaryAddressesByUserId,
            Map<Long, List<ServiceCity>> serviceCitiesByFreelancerId,
            Map<Long, List<ServiceResponse>> activeServicesByFreelancerId,
            Map<Long, ServiceResponse> activeServicesById) {
    }

    private static final class RankingStats {
        private final BigDecimal recentAverage;
        private final Long recentCount;
        private final LocalDateTime latestRecentReviewAt;
        private final BigDecimal lifetimeAverage;
        private final Long lifetimeCount;

        private RankingStats(
                BigDecimal recentAverage,
                Long recentCount,
                LocalDateTime latestRecentReviewAt,
                BigDecimal lifetimeAverage,
                Long lifetimeCount) {
            this.recentAverage = recentAverage;
            this.recentCount = recentCount;
            this.latestRecentReviewAt = latestRecentReviewAt;
            this.lifetimeAverage = lifetimeAverage;
            this.lifetimeCount = lifetimeCount;
        }

        public boolean hasRecentReviews() {
            return recentCount != null && recentCount > 0;
        }

        public BigDecimal getRecentAverage() {
            return recentAverage;
        }

        public Long getRecentCount() {
            return recentCount != null ? recentCount : 0L;
        }

        public LocalDateTime getLatestRecentReviewAt() {
            return latestRecentReviewAt;
        }

        public BigDecimal getLifetimeAverage() {
            return lifetimeAverage;
        }

        public Long getLifetimeCount() {
            return lifetimeCount != null ? lifetimeCount : 0L;
        }
    }
}
