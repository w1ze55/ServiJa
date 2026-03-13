package com.servija.servija.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.servija.servija.dto.FreelancerProfileRequest;
import com.servija.servija.dto.FreelancerProfileResponse;
import com.servija.servija.dto.NearbyFreelancerResponse;
import com.servija.servija.model.Address;
import com.servija.servija.model.Freelancer;
import com.servija.servija.model.User;
import com.servija.servija.repository.AddressRepository;
import com.servija.servija.repository.FreelancerRepository;
import com.servija.servija.repository.UserRepository;
import com.servija.servija.repository.ServiceRequestRepository;
import com.servija.servija.model.ServiceRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.servija.servija.dto.ServiceCityResponse;
import com.servija.servija.dto.ProfessionalLinkResponse;
import com.servija.servija.model.ServiceCity;
import com.servija.servija.model.ProfessionalLink;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FreelancerService {

    private final FreelancerRepository freelancerRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final ObjectMapper objectMapper;

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
        if (request.getCategory() != null) {
            freelancer.setCategory(request.getCategory());
        }
        if (request.getBio() != null) {
            freelancer.setBio(request.getBio());
        }
        if (request.getAvatarUrl() != null) {
            freelancer.setAvatarUrl(request.getAvatarUrl());
        }

        // Atualizar configurações de serviço
        if (request.getServiceType() != null) {
            freelancer.setServiceType(request.getServiceType());
        }
        if (request.getWorkModel() != null) {
            freelancer.setWorkModel(Freelancer.WorkModel.valueOf(request.getWorkModel().toUpperCase()));
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
            // Remover todas as cidades antigas
            freelancer.getServiceCities().clear();

            // Adicionar as novas cidades
            for (ServiceCityResponse cityDto : request.getServiceCities()) {
                ServiceCity serviceCity = ServiceCity.builder()
                        .freelancer(freelancer)
                        .city(cityDto.getCity())
                        .state(cityDto.getState())
                        .build();
                freelancer.getServiceCities().add(serviceCity);
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

        return FreelancerProfileResponse.builder()
                .id(freelancer.getId())
                .name(freelancer.getName())
                .email(freelancer.getEmail())
                .profession(freelancer.getProfessionalTitle())
                .category(freelancer.getCategory())
                .bio(freelancer.getBio())
                .profilePhoto(freelancer.getAvatarUrl())
                .serviceType(freelancer.getServiceType())
                .workModel(freelancer.getWorkModel() != null ? freelancer.getWorkModel().name().toLowerCase() : null)
                .location(location)
                .cities(cities)
                .serviceCities(serviceCityResponses)
                .professionalLinks(professionalLinkResponses)
                .socialLinks(socialLinks)
                .rating(freelancer.getRating())
                .totalReviews(freelancer.getTotalReviews())
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
        List<Freelancer> freelancers;

        if (category != null && !category.isEmpty()) {
            freelancers = freelancerRepository.findByCategoryWithLocation(category);
        } else {
            freelancers = freelancerRepository.findAllWithLocation();
        }

        return freelancers.stream()
                .map(f -> {
                    Double distance = calculateDistance(
                            latitude.doubleValue(),
                            longitude.doubleValue(),
                            f.getLatitude().doubleValue(),
                            f.getLongitude().doubleValue());
                    return mapToNearbyResponse(f, distance);
                })
                .filter(f -> f.getDistance() <= radiusKm)
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

    private NearbyFreelancerResponse mapToNearbyResponse(Freelancer freelancer, Double distance) {
        return NearbyFreelancerResponse.builder()
                .id(freelancer.getId())
                .name(freelancer.getName())
                .avatar(freelancer.getAvatarUrl())
                .professionalTitle(freelancer.getProfessionalTitle())
                .rating(freelancer.getRating())
                .totalReviews(freelancer.getTotalReviews())
                .location(freelancer.getLocation())
                .latitude(freelancer.getLatitude())
                .longitude(freelancer.getLongitude())
                .distance(Math.round(distance * 100.0) / 100.0) // Arredondar para 2 casas decimais
                .available(freelancer.getAvailable())
                .category(freelancer.getCategory())
                .build();
    }
}
