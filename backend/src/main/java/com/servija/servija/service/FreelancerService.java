package com.servija.servija.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.servija.servija.dto.FreelancerProfileRequest;
import com.servija.servija.dto.FreelancerProfileResponse;
import com.servija.servija.dto.NearbyFreelancerResponse;
import com.servija.servija.dto.ProfessionalLinkResponse;
import com.servija.servija.dto.ServiceCityRequest;
import com.servija.servija.dto.ServiceCityResponse;
import com.servija.servija.model.Address;
import com.servija.servija.model.Freelancer;
import com.servija.servija.model.ProfessionalLink;
import com.servija.servija.model.ServiceCity;
import com.servija.servija.model.ServiceSubcategory;
import com.servija.servija.model.ServiceRequest;
import com.servija.servija.model.User;
import com.servija.servija.repository.AddressRepository;
import com.servija.servija.repository.FreelancerRepository;
import com.servija.servija.repository.ReviewRepository;
import com.servija.servija.repository.ServiceRequestRepository;
import com.servija.servija.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FreelancerService {

    private final FreelancerRepository freelancerRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final ReviewRepository reviewRepository;
    private final ObjectMapper objectMapper;
    private final CategoryCatalogService categoryCatalogService;
    private final IbgeLocationService ibgeLocationService;
    private final GeocodingService geocodingService;

    @Transactional(readOnly = true)
    public FreelancerProfileResponse getProfile() {
        Freelancer freelancer = getCurrentFreelancer();
        return mapToResponse(freelancer);
    }

    /**
     * Obter perfil público de um freelancer por ID (não requer autenticação)
     */
    @Transactional(readOnly = true)
    public FreelancerProfileResponse getPublicProfile(Long freelancerId) {
        // Carregar freelancer com suas cidades e links
        Freelancer freelancer = freelancerRepository.findByIdWithServiceCities(freelancerId)
                .orElseThrow(() -> new RuntimeException("Freelancer não encontrado"));

        // Carregar links profissionais
        freelancerRepository.findByIdWithProfessionalLinks(freelancerId);

        return mapToResponse(freelancer);
    }

    @Transactional
    public FreelancerProfileResponse updateProfile(FreelancerProfileRequest request) {
        Freelancer freelancer = getCurrentFreelancer();

        // Atualizar campos básicos
        if (request.getName() != null) {
            freelancer.setName(request.getName());
        }
        if (request.getProfession() != null) {
            freelancer.setProfessionalTitle(request.getProfession());
        }
        if (request.getBio() != null) {
            freelancer.setBio(request.getBio());
        }
        if (request.getAvatarUrl() != null) {
            freelancer.setAvatarUrl(request.getAvatarUrl());
        }

        // Atualizar configurações de serviço
        if (hasCategorySelection(request)) {
            List<Long> requestedSubcategoryIds = normalizeSubcategoryIds(request);
            CategoryCatalogService.CategorySelection categorySelection = categoryCatalogService.resolveSelection(
                    request.getCategoryId(),
                    requestedSubcategoryIds.isEmpty() ? request.getSubcategoryId() : requestedSubcategoryIds.get(0),
                    request.getCategory(),
                    request.getSubcategory(),
                    true,
                    requestedSubcategoryIds.isEmpty(),
                    false,
                    false);
            List<ServiceSubcategory> resolvedSubcategories = resolveRequestedSubcategories(
                    categorySelection,
                    requestedSubcategoryIds);
            ServiceSubcategory primarySubcategory = !resolvedSubcategories.isEmpty()
                    ? resolvedSubcategories.get(0)
                    : categorySelection.getSubcategory();

            freelancer.setCategory(categorySelection.getCategoryLabel());
            freelancer.setServiceCategory(categorySelection.getCategory());
            freelancer.setServiceSubcategory(primarySubcategory);
            freelancer.getServiceSubcategories().clear();
            freelancer.getServiceSubcategories().addAll(resolvedSubcategories);
            freelancer.setServiceType(StringUtils.hasText(request.getServiceType())
                    ? request.getServiceType()
                    : categorySelection.getCategoryLabel());
        } else if (request.getServiceType() != null) {
            freelancer.setServiceType(request.getServiceType());
        }
        if (request.getCities() != null) {
            try {
                freelancer.setCities(objectMapper.writeValueAsString(request.getCities()));
            } catch (JsonProcessingException e) {
                log.error("Erro ao serializar cidades", e);
            }
        }

        // Atualizar serviceCities (formato novo - entidades)
        if (request.getServiceCities() != null) {
            List<NormalizedServiceCity> normalizedCities = validateAndNormalizeServiceCities(
                    freelancer,
                    request.getServiceCities());

            // Remover todas as cidades antigas
            freelancer.getServiceCities().clear();

            // Adicionar as novas cidades
            for (NormalizedServiceCity cityDto : normalizedCities) {
                var builder = ServiceCity.builder()
                        .freelancer(freelancer)
                        .city(cityDto.getCity())
                        .state(cityDto.getState());

                if (cityDto.getLatitude() != null && cityDto.getLongitude() != null) {
                    builder.latitude(cityDto.getLatitude()).longitude(cityDto.getLongitude());
                } else {
                    geocodingService.geocodeCity(cityDto.getCity(), cityDto.getState())
                            .ifPresent(coords -> builder.latitude(coords.latitude()).longitude(coords.longitude()));
                }

                freelancer.getServiceCities().add(builder.build());
            }
        }

        // Atualizar professionalLinks (formato novo - entidades)
        if (request.getProfessionalLinks() != null) {
            // Remover todos os links antigos
            freelancer.getProfessionalLinks().clear();

            // Adicionar os novos links
            for (ProfessionalLinkResponse linkDto : request.getProfessionalLinks()) {
                ProfessionalLink professionalLink = ProfessionalLink.builder()
                        .freelancer(freelancer)
                        .type(ProfessionalLink.LinkType.valueOf(linkDto.getType().toUpperCase()))
                        .url(linkDto.getUrl())
                        .label(linkDto.getLabel())
                        .build();
                freelancer.getProfessionalLinks().add(professionalLink);
            }
        }

        // Atualizar links sociais
        if (request.getSocialLinks() != null) {
            try {
                freelancer.setSocialLinks(objectMapper.writeValueAsString(request.getSocialLinks()));
            } catch (JsonProcessingException e) {
                log.error("Erro ao serializar links sociais", e);
            }
        }

        freelancer = freelancerRepository.save(freelancer);
        return mapToResponse(freelancer);
    }

    public Freelancer getCurrentFreelancer() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));

        if (user.getUserType() != User.UserType.FREELANCER) {
            throw new RuntimeException("Usuário não é um freelancer");
        }

        // Carregar serviceCities primeiro
        Freelancer freelancer = freelancerRepository.findByIdWithServiceCities(user.getId())
                .orElseThrow(() -> new RuntimeException("Perfil de freelancer não encontrado"));

        // Carregar professionalLinks em seguida (já com serviceCities carregados)
        freelancerRepository.findByIdWithProfessionalLinks(user.getId());

        return freelancer;
    }

    private FreelancerProfileResponse mapToResponse(Freelancer freelancer) {
        List<String> cities = new ArrayList<>();
        FreelancerProfileResponse.SocialLinks socialLinks = FreelancerProfileResponse.SocialLinks.builder().build();

        // Deserializar cidades (formato antigo - JSON string)
        if (freelancer.getCities() != null && !freelancer.getCities().isEmpty()) {
            try {
                cities = objectMapper.readValue(freelancer.getCities(), new TypeReference<List<String>>() {
                });
            } catch (JsonProcessingException e) {
                log.error("Erro ao deserializar cidades", e);
            }
        }

        // Deserializar links sociais
        if (freelancer.getSocialLinks() != null && !freelancer.getSocialLinks().isEmpty()) {
            try {
                socialLinks = objectMapper.readValue(freelancer.getSocialLinks(),
                        FreelancerProfileResponse.SocialLinks.class);
            } catch (JsonProcessingException e) {
                log.error("Erro ao deserializar links sociais", e);
            }
        }

        // Mapear serviceCities (formato novo - entidades)
        List<ServiceCityResponse> serviceCityResponses = new ArrayList<>();
        if (freelancer.getServiceCities() != null && !freelancer.getServiceCities().isEmpty()) {
            serviceCityResponses = freelancer.getServiceCities().stream()
                    .map(sc -> ServiceCityResponse.builder()
                            .id(sc.getId())
                            .city(sc.getCity())
                            .state(sc.getState())
                            .latitude(sc.getLatitude())
                            .longitude(sc.getLongitude())
                            .build())
                    .collect(Collectors.toList());
        }

        // Mapear professionalLinks (formato novo - entidades)
        List<ProfessionalLinkResponse> professionalLinkResponses = new ArrayList<>();
        if (freelancer.getProfessionalLinks() != null && !freelancer.getProfessionalLinks().isEmpty()) {
            professionalLinkResponses = freelancer.getProfessionalLinks().stream()
                    .map(pl -> ProfessionalLinkResponse.builder()
                            .id(pl.getId())
                            .type(pl.getType().name().toLowerCase())
                            .url(pl.getUrl())
                            .label(pl.getLabel())
                            .build())
                    .collect(Collectors.toList());
        }

        // Buscar localização do endereço cadastrado (prioriza o padrão)
        String location = null;
        List<Address> addresses = addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(freelancer.getId());
        if (!addresses.isEmpty()) {
            Address address = addresses.get(0); // Pega o primeiro (padrão ou mais recente)
            location = address.getCity() + ", " + address.getState();
        }

        long completedJobsCount = serviceRequestRepository.countByFreelancerIdAndStatus(freelancer.getId(),
                ServiceRequest.Status.COMPLETED);

        // Calcular média das avaliações e total de reviews a partir da tabela reviews
        Double averageRating = reviewRepository.findAverageRatingByReviewedId(freelancer.getId());
        long totalReviewsCount = reviewRepository.countByReviewedId(freelancer.getId());
        List<ServiceSubcategory> freelancerSubcategories = getResolvedFreelancerSubcategories(freelancer);
        ServiceSubcategory primarySubcategory = freelancer.getServiceSubcategory() != null
                ? freelancer.getServiceSubcategory()
                : (!freelancerSubcategories.isEmpty() ? freelancerSubcategories.get(0) : null);

        return FreelancerProfileResponse.builder()
                .id(freelancer.getId())
                .name(freelancer.getName())
                .email(freelancer.getEmail())
                .profession(freelancer.getProfessionalTitle())
                .categoryId(freelancer.getServiceCategory() != null ? freelancer.getServiceCategory().getId() : null)
                .categorySlug(
                        freelancer.getServiceCategory() != null ? freelancer.getServiceCategory().getSlug() : null)
                .categoryLabel(
                        freelancer.getServiceCategory() != null ? freelancer.getServiceCategory().getName()
                                : freelancer.getCategory())
                .category(freelancer.getCategory())
                .subcategoryId(primarySubcategory != null ? primarySubcategory.getId() : null)
                .subcategoryIds(freelancerSubcategories.stream().map(ServiceSubcategory::getId).toList())
                .subcategorySlug(primarySubcategory != null
                        ? primarySubcategory.getSlug()
                        : null)
                .subcategoryLabel(primarySubcategory != null
                        ? primarySubcategory.getName()
                        : null)
                .subcategory(primarySubcategory != null ? primarySubcategory.getName() : null)
                .subcategoryLabels(freelancerSubcategories.stream().map(ServiceSubcategory::getName).toList())
                .bio(freelancer.getBio())
                .profilePhoto(freelancer.getAvatarUrl())
                .serviceType(freelancer.getServiceType())
                .location(location)
                .cities(cities)
                .serviceCities(serviceCityResponses)
                .professionalLinks(professionalLinkResponses)
                .socialLinks(socialLinks)
                .rating(averageRating != null ? BigDecimal.valueOf(averageRating) : null)
                .totalReviews((int) totalReviewsCount)
                .completedJobs((int) completedJobsCount)
                .available(freelancer.getAvailable())
                .build();
    }

    /**
     * Buscar profissionais próximos de uma coordenada
     * 
     * @param latitude  Latitude do usuário
     * @param longitude Longitude do usuário
     * @param radiusKm  Raio de busca em km
     * @param category  Categoria opcional para filtrar
     * @return Lista de profissionais próximos ordenados por distância
     */
    @Transactional(readOnly = true)
    public List<NearbyFreelancerResponse> findNearbyFreelancers(
            BigDecimal latitude,
            BigDecimal longitude,
            Double radiusKm,
            String category) {

        boolean hasCategory = category != null && !category.isEmpty();

        // Freelancers com coordenadas em service_cities (novo modelo)
        List<Freelancer> withServiceCities = hasCategory
                ? freelancerRepository.findByCategoryWithServiceCityCoordinates(category)
                : freelancerRepository.findAllWithServiceCityCoordinates();

        // Freelancers com coordenadas diretas na tabela freelancers (legado)
        List<Freelancer> withDirectCoords = hasCategory
                ? freelancerRepository.findByCategoryWithLocation(category)
                : freelancerRepository.findAllWithLocation();

        double userLat = latitude.doubleValue();
        double userLon = longitude.doubleValue();

        // IDs já cobertos via service_cities
        java.util.Set<Long> coveredIds = withServiceCities.stream()
                .map(Freelancer::getId)
                .collect(Collectors.toSet());

        // Candidatos via service_cities
        java.util.Map<Long, NearbyFreelancerResponse> results = withServiceCities.stream()
                .flatMap(f -> f.getServiceCities().stream()
                        .filter(sc -> sc.getLatitude() != null && sc.getLongitude() != null)
                        .map(sc -> {
                            double distance = calculateDistance(userLat, userLon,
                                    sc.getLatitude().doubleValue(), sc.getLongitude().doubleValue());
                            return mapToNearbyResponse(f, sc.getLatitude(), sc.getLongitude(), distance);
                        }))
                .filter(r -> r.getDistance() <= radiusKm)
                .collect(Collectors.toMap(
                        NearbyFreelancerResponse::getId,
                        r -> r,
                        (r1, r2) -> r1.getDistance() <= r2.getDistance() ? r1 : r2));

        // Candidatos via coordenadas diretas (apenas os não cobertos pelo modelo novo)
        withDirectCoords.stream()
                .filter(f -> !coveredIds.contains(f.getId()))
                .forEach(f -> {
                    double distance = calculateDistance(userLat, userLon,
                            f.getLatitude().doubleValue(), f.getLongitude().doubleValue());
                    if (distance <= radiusKm) {
                        results.merge(f.getId(),
                                mapToNearbyResponse(f, f.getLatitude(), f.getLongitude(), distance),
                                (r1, r2) -> r1.getDistance() <= r2.getDistance() ? r1 : r2);
                    }
                });

        return results.values().stream()
                .sorted(Comparator.comparingDouble(NearbyFreelancerResponse::getDistance))
                .collect(Collectors.toList());
    }

    /**
     * Atualizar localização do freelancer
     */
    @Transactional
    public void updateLocation(BigDecimal latitude, BigDecimal longitude) {
        Freelancer freelancer = getCurrentFreelancer();
        freelancer.setLatitude(latitude);
        freelancer.setLongitude(longitude);
        freelancerRepository.save(freelancer);
    }

    /**
     * Calcular distância entre dois pontos usando fórmula de Haversine
     */
    private List<NormalizedServiceCity> validateAndNormalizeServiceCities(
            Freelancer freelancer,
            List<ServiceCityRequest> requestedCities) {
        Map<Long, ServiceCity> existingCitiesById = freelancer.getServiceCities().stream()
                .filter(existingCity -> existingCity.getId() != null)
                .collect(Collectors.toMap(ServiceCity::getId, existingCity -> existingCity));
        LinkedHashSet<String> uniqueCities = new LinkedHashSet<>();
        List<NormalizedServiceCity> normalizedCities = new ArrayList<>();

        for (ServiceCityRequest requestedCity : requestedCities) {
            if (requestedCity == null) {
                throw new IllegalArgumentException("Cidade de atendimento inválida.");
            }

            String normalizedState = normalizeState(requestedCity.getState());
            String normalizedCity = normalizeCityValue(requestedCity.getCity());

            if (!StringUtils.hasText(normalizedCity)) {
                throw new IllegalArgumentException("Cidade de atendimento obrigatória.");
            }

            if (!StringUtils.hasText(normalizedState)) {
                throw new IllegalArgumentException("Estado da cidade de atendimento obrigatório.");
            }

            ServiceCity existingCity = null;
            if (requestedCity.getId() != null) {
                existingCity = existingCitiesById.get(requestedCity.getId());
                if (existingCity == null) {
                    throw new IllegalArgumentException("Cidade de atendimento inválida.");
                }
            }

            boolean unchangedExistingCity = existingCity != null
                    && normalizeState(existingCity.getState()).equals(normalizedState)
                    && normalizeLocationValue(existingCity.getCity()).equals(normalizeLocationValue(normalizedCity));

            boolean validState = ibgeLocationService.isValidState(normalizedState);
            if (!validState && !unchangedExistingCity) {
                throw new IllegalArgumentException("Estado de atendimento invalido: " + normalizedState + ".");
            }

            Optional<String> canonicalCity = validState
                    ? ibgeLocationService.findCanonicalCity(normalizedState, normalizedCity)
                    : Optional.empty();
            if (canonicalCity.isEmpty() && !unchangedExistingCity) {
                throw new IllegalArgumentException(
                        "A cidade '" + normalizedCity + "' não pertence ao estado " + normalizedState + ".");
            }

            String cityToPersist = canonicalCity.orElse(normalizedCity);
            String duplicateKey = normalizedState + ":" + normalizeLocationValue(cityToPersist);
            if (!uniqueCities.add(duplicateKey)) {
                throw new IllegalArgumentException(
                        "A cidade '" + cityToPersist + "/" + normalizedState + "' já foi informada.");
            }

            BigDecimal lat = existingCity != null ? existingCity.getLatitude() : null;
            BigDecimal lng = existingCity != null ? existingCity.getLongitude() : null;
            normalizedCities.add(new NormalizedServiceCity(cityToPersist, normalizedState, lat, lng));
        }

        return normalizedCities;
    }

    private String normalizeState(String state) {
        return state == null ? "" : state.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeCityValue(String city) {
        return city == null ? "" : city.trim();
    }

    private String normalizeLocationValue(String value) {
        return value == null
                ? ""
                : java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
                        .replaceAll("\\p{M}", "")
                        .trim()
                        .toLowerCase(Locale.ROOT);
    }

    private Double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371; // Raio da Terra em km

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    private NearbyFreelancerResponse mapToNearbyResponse(Freelancer freelancer, BigDecimal latitude, BigDecimal longitude, Double distance) {
        Double averageRating = reviewRepository.findAverageRatingByReviewedId(freelancer.getId());
        long totalReviewsCount = reviewRepository.countByReviewedId(freelancer.getId());

        return NearbyFreelancerResponse.builder()
                .id(freelancer.getId())
                .name(freelancer.getName())
                .avatar(freelancer.getAvatarUrl())
                .professionalTitle(freelancer.getProfessionalTitle())
                .rating(averageRating != null ? BigDecimal.valueOf(averageRating) : null)
                .totalReviews((int) totalReviewsCount)
                .location(freelancer.getLocation())
                .latitude(latitude)
                .longitude(longitude)
                .distance(Math.round(distance * 100.0) / 100.0)
                .available(freelancer.getAvailable())
                .category(freelancer.getCategory())
                .build();
    }

    private boolean hasCategorySelection(FreelancerProfileRequest request) {
        return request.getCategoryId() != null
                || request.getSubcategoryId() != null
                || (request.getSubcategoryIds() != null && !request.getSubcategoryIds().isEmpty())
                || StringUtils.hasText(request.getCategory())
                || StringUtils.hasText(request.getSubcategory());
    }

    private boolean matchesCategoryFilter(Freelancer freelancer, String filter) {
        if (!StringUtils.hasText(filter)) {
            return true;
        }

        String normalizedFilter = filter.trim().toLowerCase();
        return (freelancer.getServiceCategory() != null
                && (freelancer.getServiceCategory().getSlug().equalsIgnoreCase(filter)
                        || freelancer.getServiceCategory().getName().equalsIgnoreCase(filter)))
                || (freelancer.getServiceSubcategory() != null
                        && (freelancer.getServiceSubcategory().getSlug().equalsIgnoreCase(filter)
                                || freelancer.getServiceSubcategory().getName().equalsIgnoreCase(filter)))
                || getResolvedFreelancerSubcategories(freelancer).stream()
                        .anyMatch(subcategory -> subcategory.getSlug().equalsIgnoreCase(filter)
                                || subcategory.getName().equalsIgnoreCase(filter))
                || (freelancer.getCategory() != null && freelancer.getCategory().toLowerCase().equals(normalizedFilter))
                || (freelancer.getServiceType() != null
                        && freelancer.getServiceType().toLowerCase().equals(normalizedFilter));
    }

    private List<Long> normalizeSubcategoryIds(FreelancerProfileRequest request) {
        LinkedHashSet<Long> subcategoryIds = new LinkedHashSet<>();

        if (request.getSubcategoryIds() != null) {
            request.getSubcategoryIds().stream()
                    .filter(Objects::nonNull)
                    .forEach(subcategoryIds::add);
        }
        if (request.getSubcategoryId() != null) {
            subcategoryIds.add(request.getSubcategoryId());
        }

        return new ArrayList<>(subcategoryIds);
    }

    private List<ServiceSubcategory> resolveRequestedSubcategories(
            CategoryCatalogService.CategorySelection categorySelection,
            List<Long> requestedSubcategoryIds) {
        List<ServiceSubcategory> resolvedSubcategories = new ArrayList<>();

        if (categorySelection.getSubcategory() != null) {
            addSubcategoryIfMissing(resolvedSubcategories, categorySelection.getSubcategory());
        }

        for (Long subcategoryId : requestedSubcategoryIds) {
            CategoryCatalogService.CategorySelection selection = categoryCatalogService.resolveSelection(
                    categorySelection.getCategoryId(),
                    subcategoryId,
                    categorySelection.getCategoryLabel(),
                    null,
                    true,
                    false,
                    false,
                    false);
            if (selection.getSubcategory() != null) {
                addSubcategoryIfMissing(resolvedSubcategories, selection.getSubcategory());
            }
        }

        return resolvedSubcategories;
    }

    private List<ServiceSubcategory> getResolvedFreelancerSubcategories(Freelancer freelancer) {
        List<ServiceSubcategory> resolvedSubcategories = new ArrayList<>();

        if (freelancer.getServiceSubcategories() != null) {
            freelancer.getServiceSubcategories().stream()
                    .filter(Objects::nonNull)
                    .sorted(Comparator.comparing(ServiceSubcategory::getSortOrder).thenComparing(ServiceSubcategory::getName))
                    .forEach(subcategory -> addSubcategoryIfMissing(resolvedSubcategories, subcategory));
        }

        if (freelancer.getServiceSubcategory() != null) {
            addSubcategoryIfMissing(resolvedSubcategories, freelancer.getServiceSubcategory());
        }

        return resolvedSubcategories;
    }

    private void addSubcategoryIfMissing(List<ServiceSubcategory> subcategories, ServiceSubcategory candidate) {
        boolean alreadyPresent = subcategories.stream()
                .anyMatch(existing -> Objects.equals(existing.getId(), candidate.getId()));
        if (!alreadyPresent) {
            subcategories.add(candidate);
        }
    }

    private static final class NormalizedServiceCity {
        private final String city;
        private final String state;
        private final BigDecimal latitude;
        private final BigDecimal longitude;

        private NormalizedServiceCity(String city, String state) {
            this(city, state, null, null);
        }

        private NormalizedServiceCity(String city, String state, BigDecimal latitude, BigDecimal longitude) {
            this.city = city;
            this.state = state;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public String getCity() {
            return city;
        }

        public String getState() {
            return state;
        }

        public BigDecimal getLatitude() {
            return latitude;
        }

        public BigDecimal getLongitude() {
            return longitude;
        }
    }
}
