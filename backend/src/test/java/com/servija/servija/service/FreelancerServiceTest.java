package com.servija.servija.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.servija.servija.dto.FreelancerProfileRequest;
import com.servija.servija.dto.NearbyFreelancerResponse;
import com.servija.servija.dto.ServiceCityRequest;
import com.servija.servija.exception.ResourceNotFoundException;
import com.servija.servija.model.Address;
import com.servija.servija.model.Freelancer;
import com.servija.servija.model.Service;
import com.servija.servija.model.ServiceCategory;
import com.servija.servija.model.ServiceCity;
import com.servija.servija.model.ServiceRequest;
import com.servija.servija.model.ServiceSubcategory;
import com.servija.servija.repository.AddressRepository;
import com.servija.servija.repository.FreelancerRepository;
import com.servija.servija.repository.ReviewRepository;
import com.servija.servija.repository.ServiceRepository;
import com.servija.servija.repository.ServiceRequestRepository;
import com.servija.servija.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FreelancerServiceTest {

    @Mock
    private FreelancerRepository freelancerRepository;

    @Mock
    private ServiceRepository serviceRepository;

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

    @Mock
    private AuditLogger auditLogger;

    @Mock
    private CurrentUserContextService currentUserContextService;

    private FreelancerService freelancerService;

    @BeforeEach
    void setUp() {
        freelancerService = spy(new FreelancerService(
                freelancerRepository,
                serviceRepository,
                userRepository,
                addressRepository,
                serviceRequestRepository,
                reviewRepository,
                new ObjectMapper(),
                categoryCatalogService,
                ibgeLocationService,
                geocodingService,
                auditLogger,
                currentUserContextService));
        lenient().when(currentUserContextService.getOptionalAuthenticatedUser()).thenReturn(Optional.empty());
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

    @Test
    @DisplayName("Deve buscar freelancers próximos por serviço, tag e subcategoria")
    void getVisiblePublicProfile_hidesBlockedFreelancer() {
        Freelancer freelancer = buildFreelancer();
        freelancer.setId(99L);
        freelancer.setActive(false);

        when(freelancerRepository.findByIdWithServiceCities(99L)).thenReturn(Optional.of(freelancer));

        assertThatThrownBy(() -> freelancerService.getVisiblePublicProfile(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Freelancer");
    }

    @Test
    @DisplayName("Deve ocultar perfil público de freelancer bloqueado")
    void findNearbyFreelancers_filtersByServiceSearchAndCatalog() {
        ServiceCategory category = ServiceCategory.builder()
                .id(10L)
                .slug("eletrica")
                .name("Elétrica")
                .build();
        ServiceSubcategory subcategory = ServiceSubcategory.builder()
                .id(20L)
                .category(category)
                .slug("instalacao")
                .name("Instalação")
                .build();

        Freelancer matching = buildNearbyFreelancer(1L, "João Silva", -23.5505, -46.6333);
        Freelancer other = buildNearbyFreelancer(2L, "Maria Souza", -23.5510, -46.6340);
        Service service = Service.builder()
                .id(100L)
                .freelancer(matching)
                .title("Instalação de ventilador")
                .description("Serviço residencial")
                .category("Elétrica")
                .serviceCategory(category)
                .serviceSubcategory(subcategory)
                .tags("[\"ventilador\",\"urgente\"]")
                .isActive(true)
                .build();

        when(freelancerRepository.findAllWithServiceCities()).thenReturn(List.of(matching, other));
        when(addressRepository.findByUserIdInOrderByUserIdAscIsDefaultDescCreatedAtDesc(List.of(1L, 2L)))
                .thenReturn(List.of(
                        buildAddress(matching, "Sao Paulo", "SP"),
                        buildAddress(other, "Sao Paulo", "SP")));
        when(geocodingService.geocodeAddress(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(new GeocodingService.Coordinates(
                        BigDecimal.valueOf(-23.5505),
                        BigDecimal.valueOf(-46.6333))));
        when(serviceRepository.findByFreelancerIdInAndIsActiveOrderByCreatedAtDesc(any(), eq(true)))
                .thenReturn(List.of(service));
        when(reviewRepository.findAverageRatingByReviewedId(anyLong())).thenReturn(4.8);
        when(reviewRepository.countByReviewedId(anyLong())).thenReturn(12L);
        when(serviceRequestRepository.countCompletedJobsByFreelancerIds(any(), eq(ServiceRequest.Status.COMPLETED)))
                .thenReturn(List.of(completedJobsCount(1L, 7L)));

        List<NearbyFreelancerResponse> result = freelancerService.findNearbyFreelancers(
                BigDecimal.valueOf(-23.5505),
                BigDecimal.valueOf(-46.6333),
                10.0,
                null,
                "ventilador",
                10L,
                null,
                null,
                "instalacao",
                BigDecimal.valueOf(4),
                true,
                null,
                null);

        assertThat(result.get(0).getCompletedJobs()).isEqualTo(7);
        assertThat(result).singleElement().satisfies(response -> {
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getCategoryId()).isEqualTo(10L);
            assertThat(response.getSubcategoryLabel()).isEqualTo("Instalação");
        });
    }

    @Test
    @DisplayName("Deve usar cidade atendida para elegibilidade e endereço padrão para marker")
    void findNearbyFreelancers_usesDefaultAddressForMarkerAndServiceCityForCoverage() {
        Freelancer servesPalhoca = buildNearbyFreelancer(1L, "Ana Costa", -27.6455, -48.6697);
        Freelancer servesOnlyFlorianopolis = buildNearbyFreelancer(2L, "Bruno Lima", -27.5949, -48.5482);
        servesPalhoca.getServiceCities().clear();
        servesPalhoca.getServiceCities().add(ServiceCity.builder()
                .freelancer(servesPalhoca)
                .city("Palhoca")
                .state("SC")
                .latitude(BigDecimal.valueOf(-27.6455))
                .longitude(BigDecimal.valueOf(-48.6697))
                .build());
        servesOnlyFlorianopolis.getServiceCities().clear();
        servesOnlyFlorianopolis.getServiceCities().add(ServiceCity.builder()
                .freelancer(servesOnlyFlorianopolis)
                .city("Florianopolis")
                .state("SC")
                .latitude(BigDecimal.valueOf(-27.5949))
                .longitude(BigDecimal.valueOf(-48.5482))
                .build());

        Address anaAddress = buildAddress(servesPalhoca, "Florianopolis", "SC");

        when(freelancerRepository.findAllWithServiceCities()).thenReturn(List.of(servesPalhoca, servesOnlyFlorianopolis));
        when(addressRepository.findByUserIdInOrderByUserIdAscIsDefaultDescCreatedAtDesc(List.of(1L)))
                .thenReturn(List.of(anaAddress));
        when(geocodingService.geocodeAddress(
                eq(anaAddress.getStreet()),
                eq(anaAddress.getNumber()),
                eq(anaAddress.getNeighborhood()),
                eq(anaAddress.getCity()),
                eq(anaAddress.getState())))
                .thenReturn(Optional.of(new GeocodingService.Coordinates(
                        BigDecimal.valueOf(-27.5949),
                        BigDecimal.valueOf(-48.5482))));
        when(serviceRepository.findByFreelancerIdInAndIsActiveOrderByCreatedAtDesc(any(), eq(true)))
                .thenReturn(List.of());
        when(reviewRepository.findAverageRatingByReviewedId(anyLong())).thenReturn(null);
        when(reviewRepository.countByReviewedId(anyLong())).thenReturn(0L);

        List<NearbyFreelancerResponse> result = freelancerService.findNearbyFreelancers(
                BigDecimal.valueOf(-27.6455),
                BigDecimal.valueOf(-48.6697),
                30.0,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "Palhoca",
                "SC");

        assertThat(result).singleElement().satisfies(response -> {
            assertThat(response.getId()).isEqualTo(1L);
            assertThat(response.getLatitude()).isEqualByComparingTo("-27.5949");
            assertThat(response.getLongitude()).isEqualByComparingTo("-48.5482");
            assertThat(response.getLocation()).isEqualTo("Florianopolis, SC");
        });
    }

    @Test
    @DisplayName("Deve remover do mapa freelancers bloqueados")
    void findNearbyFreelancers_excludesBlockedFreelancers() {
        Freelancer visibleFreelancer = buildNearbyFreelancer(1L, "Ana Costa", -23.5505, -46.6333);
        Freelancer blockedFreelancer = buildNearbyFreelancer(2L, "Bruno Lima", -23.5510, -46.6335);
        blockedFreelancer.setActive(false);

        when(freelancerRepository.findAllWithServiceCities()).thenReturn(List.of(visibleFreelancer, blockedFreelancer));
        when(addressRepository.findByUserIdInOrderByUserIdAscIsDefaultDescCreatedAtDesc(List.of(1L)))
                .thenReturn(List.of(buildAddress(visibleFreelancer, "Sao Paulo", "SP")));
        when(geocodingService.geocodeAddress(anyString(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(new GeocodingService.Coordinates(
                        BigDecimal.valueOf(-23.5505),
                        BigDecimal.valueOf(-46.6333))));
        when(serviceRepository.findByFreelancerIdInAndIsActiveOrderByCreatedAtDesc(any(), eq(true)))
                .thenReturn(List.of());
        when(reviewRepository.findAverageRatingByReviewedId(anyLong())).thenReturn(null);
        when(reviewRepository.countByReviewedId(anyLong())).thenReturn(0L);

        List<NearbyFreelancerResponse> result = freelancerService.findNearbyFreelancers(
                BigDecimal.valueOf(-23.5505),
                BigDecimal.valueOf(-46.6333),
                10.0,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);

        assertThat(result).extracting(NearbyFreelancerResponse::getId).containsExactly(1L);
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

    private Freelancer buildNearbyFreelancer(Long id, String name, double latitude, double longitude) {
        Freelancer freelancer = buildFreelancer();
        freelancer.setId(id);
        freelancer.setName(name);
        freelancer.setProfessionalTitle("Profissional");
        freelancer.setAvailable(true);
        freelancer.setActive(true);
        freelancer.setCompletedJobs(3);
        freelancer.getServiceCities().add(ServiceCity.builder()
                .freelancer(freelancer)
                .city("São Paulo")
                .state("SP")
                .latitude(BigDecimal.valueOf(latitude))
                .longitude(BigDecimal.valueOf(longitude))
                .build());
        return freelancer;
    }

    private Address buildAddress(Freelancer freelancer, String city, String state) {
        return Address.builder()
                .user(freelancer.getUser())
                .label("Principal")
                .street("Rua Teste")
                .number("123")
                .neighborhood("Centro")
                .city(city)
                .state(state)
                .zipCode("88000000")
                .isDefault(true)
                .build();
    }

    private ServiceRequestRepository.CompletedJobsCountProjection completedJobsCount(Long freelancerId, long completedJobs) {
        return new ServiceRequestRepository.CompletedJobsCountProjection() {
            @Override
            public Long getFreelancerId() {
                return freelancerId;
            }

            @Override
            public long getCompletedJobs() {
                return completedJobs;
            }
        };
    }
}
