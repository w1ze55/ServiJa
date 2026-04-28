package com.servija.servija.service;

import com.servija.servija.dto.ClientHomeResponse;
import com.servija.servija.dto.FavoriteResponse;
import com.servija.servija.dto.FreelancerSimpleResponse;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientHomeServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private FreelancerRepository freelancerRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ServiceRequestService serviceRequestService;

    @Mock
    private ServiceService serviceService;

    @Mock
    private FavoriteService favoriteService;

    private ClientHomeService clientHomeService;

    @BeforeEach
    void setUp() {
        clientHomeService = spy(new ClientHomeService(
                userRepository,
                addressRepository,
                freelancerRepository,
                reviewRepository,
                serviceRequestService,
                serviceService,
                favoriteService));
    }

    @Test
    @DisplayName("Deve montar a home do cliente com região, interesses e destaques")
    void getHome_buildsRegionalHomePayload() {
        User contractor = buildContractor(10L, "cliente@servija.com");
        Freelancer regionalFreelancer = buildFreelancer(21L, "Ana UX", true, "São Paulo", "SP");
        Freelancer globalFreelancer = buildFreelancer(22L, "Bruno Dev", true, "Campinas", "SP");

        ServiceResponse regionalService = buildService(100L, 21L, "UX para SaaS", "Desenvolvimento e TI", "UX e UI");
        ServiceResponse globalService = buildService(101L, 22L, "Landing page moderna", "Desenvolvimento e TI", "Desenvolvimento Web");

        List<ServiceRequestResponse> requests = List.of(
                buildRequest(1L, "PENDING", "Briefing de produto", "Desenvolvimento e TI", "UX e UI", LocalDateTime.now().minusDays(1)),
                buildRequest(2L, "ACCEPTED", "Protótipo navegável", "Desenvolvimento e TI", "UX e UI", LocalDateTime.now().minusDays(3)),
                buildRequest(3L, "COMPLETED", "Página de captura", "Desenvolvimento e TI", "Landing Pages", LocalDateTime.now().minusDays(10))
        );

        doReturn(contractor).when(clientHomeService).getCurrentContractor();
        when(serviceRequestService.getByContractorEmail(contractor.getEmail())).thenReturn(requests);
        when(addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(contractor.getId()))
                .thenReturn(List.of(buildAddress(contractor, "Casa", "São Paulo", "SP", true)));
        when(favoriteService.getUserFavorites(contractor.getId())).thenReturn(List.of(
                FavoriteResponse.builder()
                        .id(501L)
                        .service(regionalService)
                        .createdAt(LocalDateTime.now().minusHours(2))
                        .build(),
                FavoriteResponse.builder()
                        .id(502L)
                        .freelancer(FreelancerSimpleResponse.builder()
                                .userId(regionalFreelancer.getId())
                                .name(regionalFreelancer.getName())
                                .professionalTitle(regionalFreelancer.getProfessionalTitle())
                                .category(regionalFreelancer.getCategory())
                                .avatarUrl(regionalFreelancer.getAvatarUrl())
                                .build())
                        .createdAt(LocalDateTime.now().minusHours(1))
                        .build()));
        when(serviceService.getActiveServices(null, null, null)).thenReturn(List.of(regionalService, globalService));
        when(freelancerRepository.findAllByIdInWithServiceCities(anyList()))
                .thenReturn(List.of(regionalFreelancer, globalFreelancer));
        when(freelancerRepository.findById(eq(regionalFreelancer.getId()))).thenReturn(Optional.of(regionalFreelancer));
        when(freelancerRepository.findById(eq(globalFreelancer.getId()))).thenReturn(Optional.of(globalFreelancer));
        when(addressRepository.findByUserIdInOrderByUserIdAscIsDefaultDescCreatedAtDesc(anyList()))
                .thenReturn(List.of(
                        buildAddress(regionalFreelancer, "Atendimento", "São Paulo", "SP", true),
                        buildAddress(globalFreelancer, "Atendimento", "Campinas", "SP", true)));
        when(reviewRepository.findByServiceId(eq(regionalService.getId()))).thenReturn(List.of(
                buildReview(5, LocalDateTime.now().minusDays(5)),
                buildReview(5, LocalDateTime.now().minusDays(15))));
        when(reviewRepository.findByServiceId(eq(globalService.getId()))).thenReturn(List.of(
                buildReview(5, LocalDateTime.now().minusDays(120))));
        when(reviewRepository.findByReviewedIdOrderByCreatedAtDesc(eq(regionalFreelancer.getId()))).thenReturn(List.of(
                buildReview(5, LocalDateTime.now().minusDays(4)),
                buildReview(5, LocalDateTime.now().minusDays(12))));
        when(reviewRepository.findByReviewedIdOrderByCreatedAtDesc(eq(globalFreelancer.getId()))).thenReturn(List.of(
                buildReview(4, LocalDateTime.now().minusDays(160))));

        ClientHomeResponse response = clientHomeService.getHome();

        assertThat(response.getRegion().getLocalized()).isTrue();
        assertThat(response.getRegion().getDisplayName()).isEqualTo("São Paulo, SP");
        assertThat(response.getRecentRequests()).hasSize(1);
        assertThat(response.getActiveProjects()).hasSize(1);
        assertThat(response.getInterestCategories()).contains("UX e UI");
        assertThat(response.getFavorites().getServiceCount()).isEqualTo(1);
        assertThat(response.getFavorites().getProfessionalCount()).isEqualTo(1);
        assertThat(response.getFeaturedServices()).isNotEmpty();
        assertThat(response.getFeaturedServices().get(0).getId()).isEqualTo(regionalService.getId());
        assertThat(response.getFeaturedProfessionals()).isNotEmpty();
        assertThat(response.getFeaturedProfessionals().get(0).getId()).isEqualTo(regionalFreelancer.getId());
    }

    @Test
    @DisplayName("Deve usar fallback global quando o cliente não possui endereço")
    void getHome_fallsBackToGlobalRankingWithoutAddress() {
        User contractor = buildContractor(11L, "cliente2@servija.com");
        Freelancer freelancer = buildFreelancer(31L, "Clara Produto", true, "Curitiba", "PR");
        ServiceResponse service = buildService(201L, 31L, "Diagnóstico de produto", "Consultoria", "Produto");

        doReturn(contractor).when(clientHomeService).getCurrentContractor();
        when(serviceRequestService.getByContractorEmail(contractor.getEmail())).thenReturn(List.of());
        when(addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(contractor.getId())).thenReturn(List.of());
        when(favoriteService.getUserFavorites(contractor.getId())).thenReturn(List.of());
        when(serviceService.getActiveServices(null, null, null)).thenReturn(List.of(service));
        when(freelancerRepository.findAllByIdInWithServiceCities(anyList())).thenReturn(List.of(freelancer));
        when(freelancerRepository.findById(anyLong())).thenReturn(Optional.of(freelancer));
        when(addressRepository.findByUserIdInOrderByUserIdAscIsDefaultDescCreatedAtDesc(anyList()))
                .thenReturn(List.of(buildAddress(freelancer, "Atendimento", "Curitiba", "PR", true)));
        when(reviewRepository.findByReviewedIdOrderByCreatedAtDesc(freelancer.getId()))
                .thenReturn(List.of(buildReview(5, LocalDateTime.now().minusDays(6))));

        ClientHomeResponse response = clientHomeService.getHome();

        assertThat(response.getRegion().getLocalized()).isFalse();
        assertThat(response.getRegion().getDisplayName()).isEqualTo("toda a plataforma");
        assertThat(response.getFeaturedServices()).hasSize(1);
        assertThat(response.getFeaturedProfessionals()).hasSize(1);
    }

    private User buildContractor(Long id, String email) {
        User contractor = new User();
        contractor.setId(id);
        contractor.setName("Cliente");
        contractor.setEmail(email);
        contractor.setCpf("12345678901");
        contractor.setPhone("11999999999");
        contractor.setUserType(User.UserType.CONTRACTOR);
        return contractor;
    }

    private Freelancer buildFreelancer(Long id, String name, boolean available, String city, String state) {
        Freelancer freelancer = new Freelancer();
        freelancer.setId(id);
        freelancer.setName(name);
        freelancer.setEmail(name.toLowerCase(Locale.ROOT).replace(" ", "") + "@servija.com");
        freelancer.setCpf("1234567890" + id);
        freelancer.setPhone("11999999999");
        freelancer.setUserType(User.UserType.FREELANCER);
        freelancer.setProfessionalTitle("Especialista");
        freelancer.setCategory("Desenvolvimento e TI");
        freelancer.setAvailable(available);
        freelancer.setAvatarUrl("https://example.com/avatar.png");
        freelancer.setServiceCities(new ArrayList<>(List.of(ServiceCity.builder()
                .freelancer(freelancer)
                .city(city)
                .state(state)
                .build())));
        return freelancer;
    }

    private Address buildAddress(User user, String label, String city, String state, boolean isDefault) {
        Address address = new Address();
        address.setId(Math.abs(Objects.hash(user.getId(), city, state)) + 1L);
        address.setUser(user);
        address.setLabel(label);
        address.setStreet("Rua Teste");
        address.setNumber("100");
        address.setNeighborhood("Centro");
        address.setCity(city);
        address.setState(state);
        address.setZipCode("01001-000");
        address.setIsDefault(isDefault);
        return address;
    }

    private ServiceResponse buildService(Long id, Long freelancerId, String title, String category, String subcategory) {
        return ServiceResponse.builder()
                .id(id)
                .freelancerId(freelancerId)
                .freelancerName("Freelancer " + freelancerId)
                .title(title)
                .category(category)
                .categoryLabel(category)
                .subcategory(subcategory)
                .subcategoryLabel(subcategory)
                .description("Descrição do serviço")
                .serviceRating(BigDecimal.valueOf(4.9))
                .serviceReviewCount(12)
                .price(BigDecimal.valueOf(1500))
                .priceType("FIXED")
                .deliveryTime(10)
                .deliveryUnit("DAYS")
                .workModel("REMOTO")
                .tags(List.of())
                .images(List.of())
                .isActive(true)
                .freelancerProfilePhoto("https://example.com/profile.png")
                .createdAt(LocalDateTime.now().minusDays(20))
                .updatedAt(LocalDateTime.now().minusDays(2))
                .build();
    }

    private ServiceRequestResponse buildRequest(
            Long id,
            String status,
            String serviceName,
            String category,
            String subcategory,
            LocalDateTime createdAt) {
        return ServiceRequestResponse.builder()
                .id(id)
                .serviceName(serviceName)
                .category(category)
                .categoryLabel(category)
                .subcategory(subcategory)
                .subcategoryLabel(subcategory)
                .description("Descrição da solicitação")
                .status(status)
                .createdAt(createdAt)
                .updatedAt(createdAt.plusHours(6))
                .build();
    }

    private Review buildReview(int rating, LocalDateTime createdAt) {
        Review review = new Review();
        review.setRating(rating);
        review.setCreatedAt(createdAt);
        return review;
    }
}
