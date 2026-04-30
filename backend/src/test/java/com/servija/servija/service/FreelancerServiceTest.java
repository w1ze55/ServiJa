package com.servija.servija.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.servija.servija.dto.FreelancerProfileRequest;
import com.servija.servija.dto.ServiceCityRequest;
import com.servija.servija.model.Freelancer;
import com.servija.servija.model.ServiceCity;
import com.servija.servija.model.ServiceRequest;
import com.servija.servija.repository.AddressRepository;
import com.servija.servija.repository.FreelancerRepository;
import com.servija.servija.repository.ReviewRepository;
import com.servija.servija.repository.ServiceRequestRepository;
import com.servija.servija.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FreelancerServiceTest {

    @Mock
    private FreelancerRepository freelancerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private ServiceRequestRepository serviceRequestRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private CategoryCatalogService categoryCatalogService;

    @Mock
    private IbgeLocationService ibgeLocationService;

    @Mock
    private GeocodingService geocodingService;

    private FreelancerService freelancerService;

    @BeforeEach
    void setUp() {
        freelancerService = spy(new FreelancerService(
                freelancerRepository,
                userRepository,
                addressRepository,
                serviceRequestRepository,
                reviewRepository,
                new ObjectMapper(),
                categoryCatalogService,
                ibgeLocationService,
                geocodingService));
    }

    @Test
    @DisplayName("Deve aceitar cidade válida e persistir o nome canônico do IBGE")
    void updateProfile_acceptsValidServiceCity() {
        Freelancer freelancer = buildFreelancer();
        FreelancerProfileRequest request = FreelancerProfileRequest.builder()
                .serviceCities(List.of(ServiceCityRequest.builder()
                        .city("sao paulo")
                        .state("sp")
                        .build()))
                .build();

        mockCurrentFreelancer(freelancer);
        mockPersistenceDependencies(freelancer);
        when(ibgeLocationService.isValidState("SP")).thenReturn(true);
        when(ibgeLocationService.findCanonicalCity("SP", "sao paulo")).thenReturn(Optional.of("Sao Paulo"));

        freelancerService.updateProfile(request);

        assertThat(freelancer.getServiceCities()).hasSize(1);
        assertThat(freelancer.getServiceCities().get(0).getCity()).isEqualTo("Sao Paulo");
        assertThat(freelancer.getServiceCities().get(0).getState()).isEqualTo("SP");
    }

    @Test
    @DisplayName("Deve rejeitar cidade que não pertence ao estado informado")
    void updateProfile_rejectsInvalidCityStatePair() {
        Freelancer freelancer = buildFreelancer();
        FreelancerProfileRequest request = FreelancerProfileRequest.builder()
                .serviceCities(List.of(ServiceCityRequest.builder()
                        .city("Cidade Fantasma")
                        .state("SP")
                        .build()))
                .build();

        mockCurrentFreelancer(freelancer);
        when(ibgeLocationService.isValidState("SP")).thenReturn(true);
        when(ibgeLocationService.findCanonicalCity("SP", "Cidade Fantasma")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> freelancerService.updateProfile(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não pertence ao estado");
    }

    @Test
    @DisplayName("Deve rejeitar cidades duplicadas na mesma UF")
    void updateProfile_rejectsDuplicateCities() {
        Freelancer freelancer = buildFreelancer();
        FreelancerProfileRequest request = FreelancerProfileRequest.builder()
                .serviceCities(List.of(
                        ServiceCityRequest.builder().city("Sao Paulo").state("SP").build(),
                        ServiceCityRequest.builder().city("sao paulo").state("sp").build()))
                .build();

        mockCurrentFreelancer(freelancer);
        when(ibgeLocationService.isValidState("SP")).thenReturn(true);
        when(ibgeLocationService.findCanonicalCity(eq("SP"), anyString())).thenReturn(Optional.of("Sao Paulo"));

        assertThatThrownBy(() -> freelancerService.updateProfile(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("já foi informada");
    }

    @Test
    @DisplayName("Deve aceitar cidade legacy inválida quando o item existente não foi alterado")
    void updateProfile_acceptsUnchangedLegacyCity() {
        Freelancer freelancer = buildFreelancer();
        freelancer.setServiceCities(new ArrayList<>(List.of(ServiceCity.builder()
                .id(7L)
                .freelancer(freelancer)
                .city("Cidade Inexistente")
                .state("SP")
                .build())));

        FreelancerProfileRequest request = FreelancerProfileRequest.builder()
                .serviceCities(List.of(ServiceCityRequest.builder()
                        .id(7L)
                        .city("Cidade Inexistente")
                        .state("SP")
                        .build()))
                .build();

        mockCurrentFreelancer(freelancer);
        mockPersistenceDependencies(freelancer);
        when(ibgeLocationService.isValidState("SP")).thenReturn(true);
        when(ibgeLocationService.findCanonicalCity("SP", "Cidade Inexistente")).thenReturn(Optional.empty());

        freelancerService.updateProfile(request);

        assertThat(freelancer.getServiceCities()).hasSize(1);
        assertThat(freelancer.getServiceCities().get(0).getCity()).isEqualTo("Cidade Inexistente");
        assertThat(freelancer.getServiceCities().get(0).getState()).isEqualTo("SP");
    }

    @Test
    @DisplayName("Deve rejeitar cidade legacy inválida quando o item existente foi alterado")
    void updateProfile_rejectsChangedLegacyCity() {
        Freelancer freelancer = buildFreelancer();
        freelancer.setServiceCities(new ArrayList<>(List.of(ServiceCity.builder()
                .id(7L)
                .freelancer(freelancer)
                .city("Cidade Inexistente")
                .state("SP")
                .build())));

        FreelancerProfileRequest request = FreelancerProfileRequest.builder()
                .serviceCities(List.of(ServiceCityRequest.builder()
                        .id(7L)
                        .city("Outra Cidade Inexistente")
                        .state("SP")
                        .build()))
                .build();

        mockCurrentFreelancer(freelancer);
        when(ibgeLocationService.isValidState("SP")).thenReturn(true);
        when(ibgeLocationService.findCanonicalCity("SP", "Outra Cidade Inexistente")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> freelancerService.updateProfile(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("não pertence ao estado");
    }

    private void mockCurrentFreelancer(Freelancer freelancer) {
        freelancer.setId(1L);
        doReturn(freelancer).when(freelancerService).getCurrentFreelancer();
    }

    private void mockPersistenceDependencies(Freelancer freelancer) {
        when(freelancerRepository.save(any(Freelancer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(freelancer.getId())).thenReturn(List.of());
        when(serviceRequestRepository.countByFreelancerIdAndStatus(freelancer.getId(), ServiceRequest.Status.COMPLETED))
                .thenReturn(0L);
        when(reviewRepository.findAverageRatingByReviewedId(freelancer.getId())).thenReturn(null);
        when(reviewRepository.countByReviewedId(freelancer.getId())).thenReturn(0L);
    }

    private Freelancer buildFreelancer() {
        Freelancer freelancer = new Freelancer();
        freelancer.setServiceCities(new ArrayList<>());
        freelancer.setProfessionalLinks(new ArrayList<>());
        freelancer.setServiceSubcategories(new ArrayList<>());
        return freelancer;
    }
}
