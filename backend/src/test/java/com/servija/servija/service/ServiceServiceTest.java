package com.servija.servija.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.servija.servija.dto.ServiceResponse;
import com.servija.servija.exception.ResourceNotFoundException;
import com.servija.servija.model.AuditLog;
import com.servija.servija.model.Freelancer;
import com.servija.servija.model.Service;
import com.servija.servija.model.ServiceRequest;
import com.servija.servija.model.User;
import com.servija.servija.repository.FavoriteRepository;
import com.servija.servija.repository.FreelancerRepository;
import com.servija.servija.repository.ReviewRepository;
import com.servija.servija.repository.ServiceRepository;
import com.servija.servija.repository.ServiceRequestRepository;
import com.servija.servija.repository.ServiceSearchWeeklyMetricRepository;
import com.servija.servija.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceServiceTest {

    @Mock private ServiceRepository serviceRepository;
    @Mock private FreelancerRepository freelancerRepository;
    @Mock private UserRepository userRepository;
    @Mock private FileStorageService fileStorageService;
    @Mock private ServiceRequestRepository serviceRequestRepository;
    @Mock private FavoriteRepository favoriteRepository;
    @Mock private ReviewRepository reviewRepository;
    @Mock private ObjectMapper objectMapper;
    @Mock private CategoryCatalogService categoryCatalogService;
    @Mock private ServiceSearchWeeklyMetricService serviceSearchWeeklyMetricService;
    @Mock private AuditLogger auditLogger;
    @Mock private CurrentUserContextService currentUserContextService;
    @Mock private ServiceSearchWeeklyMetricRepository serviceSearchWeeklyMetricRepository;

    @InjectMocks
    private ServiceService serviceService;

    @BeforeEach
    void setUp() {
        lenient().when(currentUserContextService.getOptionalAuthenticatedUser()).thenReturn(Optional.empty());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Deve retornar faixa média de preço quando houver serviços concluídos com valor")
    void getPublicServiceByIdWithPricing_returnsAverageRange() {
        Service service = buildService(7L, new BigDecimal("200.00"));

        when(serviceRepository.findById(7L)).thenReturn(Optional.of(service));
        when(reviewRepository.countByServiceId(7L)).thenReturn(3L);
        when(reviewRepository.findAverageRatingByServiceId(7L)).thenReturn(4.66);
        when(serviceRequestRepository.countByService_IdAndStatusAndResponseValueIsNotNull(7L, ServiceRequest.Status.COMPLETED))
                .thenReturn(4L);
        when(serviceRequestRepository.findAverageCompletedResponseValueByServiceId(7L, "COMPLETED"))
                .thenReturn(new BigDecimal("225.00"));

        ServiceResponse response = serviceService.getVisiblePublicServiceByIdWithPricing(7L);

        assertThat(response.getPrice()).isEqualByComparingTo("200.00");
        assertThat(response.getServiceRating()).isEqualByComparingTo("4.7");
        assertThat(response.getServiceReviewCount()).isEqualTo(3);
        assertThat(response.getCompletedServicesCount()).isEqualTo(4L);
        assertThat(response.getAverageCompletedPrice()).isEqualByComparingTo("225.00");
        assertThat(response.getAveragePriceMin()).isEqualByComparingTo("202.50");
        assertThat(response.getAveragePriceMax()).isEqualByComparingTo("247.50");
    }

    @Test
    @DisplayName("Deve manter o preço cadastrado quando não houver histórico concluído")
    void getPublicServiceByIdWithPricing_keepsRegisteredPriceWithoutHistory() {
        Service service = buildService(9L, new BigDecimal("180.00"));

        when(serviceRepository.findById(9L)).thenReturn(Optional.of(service));
        when(reviewRepository.countByServiceId(9L)).thenReturn(2L);
        when(reviewRepository.findAverageRatingByServiceId(9L)).thenReturn(4.25);
        when(serviceRequestRepository.countByService_IdAndStatusAndResponseValueIsNotNull(9L, ServiceRequest.Status.COMPLETED))
                .thenReturn(0L);

        ServiceResponse response = serviceService.getVisiblePublicServiceByIdWithPricing(9L);

        assertThat(response.getPrice()).isEqualByComparingTo("180.00");
        assertThat(response.getServiceRating()).isEqualByComparingTo("4.3");
        assertThat(response.getServiceReviewCount()).isEqualTo(2);
        assertThat(response.getCompletedServicesCount()).isZero();
        assertThat(response.getAverageCompletedPrice()).isNull();
        assertThat(response.getAveragePriceMin()).isNull();
        assertThat(response.getAveragePriceMax()).isNull();
        verify(serviceRequestRepository, never()).findAverageCompletedResponseValueByServiceId(9L, "COMPLETED");
    }

    @Test
    @DisplayName("Deve retornar faixa média de preço nas listagens públicas quando houver histórico concluído")
    void getAllActiveServices_returnsAverageRangeForPublicListings() {
        Service service = buildService(11L, new BigDecimal("80.00"));

        when(serviceRepository.findByIsActiveOrderByCreatedAtDesc(true)).thenReturn(List.of(service));
        when(reviewRepository.countByServiceId(11L)).thenReturn(4L);
        when(reviewRepository.findAverageRatingByServiceId(11L)).thenReturn(4.74);
        when(serviceRequestRepository.countByService_IdAndStatusAndResponseValueIsNotNull(11L, ServiceRequest.Status.COMPLETED))
                .thenReturn(3L);
        when(serviceRequestRepository.findAverageCompletedResponseValueByServiceId(11L, "COMPLETED"))
                .thenReturn(new BigDecimal("64.90"));

        List<ServiceResponse> responses = serviceService.getAllActiveServices();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getPrice()).isEqualByComparingTo("80.00");
        assertThat(responses.get(0).getServiceRating()).isEqualByComparingTo("4.7");
        assertThat(responses.get(0).getServiceReviewCount()).isEqualTo(4);
        assertThat(responses.get(0).getCompletedServicesCount()).isEqualTo(3L);
        assertThat(responses.get(0).getAverageCompletedPrice()).isEqualByComparingTo("64.90");
        assertThat(responses.get(0).getAveragePriceMin()).isEqualByComparingTo("58.41");
        assertThat(responses.get(0).getAveragePriceMax()).isEqualByComparingTo("71.39");
    }

    @Test
    @DisplayName("Deve manter o preço cadastrado nas listagens públicas quando não houver histórico concluído")
    void getAllActiveServices_keepsRegisteredPriceWithoutHistory() {
        Service service = buildService(12L, new BigDecimal("150.00"));

        when(serviceRepository.findByIsActiveOrderByCreatedAtDesc(true)).thenReturn(List.of(service));
        when(reviewRepository.countByServiceId(12L)).thenReturn(2L);
        when(reviewRepository.findAverageRatingByServiceId(12L)).thenReturn(4.24);
        when(serviceRequestRepository.countByService_IdAndStatusAndResponseValueIsNotNull(12L, ServiceRequest.Status.COMPLETED))
                .thenReturn(0L);

        List<ServiceResponse> responses = serviceService.getAllActiveServices();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getPrice()).isEqualByComparingTo("150.00");
        assertThat(responses.get(0).getServiceRating()).isEqualByComparingTo("4.2");
        assertThat(responses.get(0).getServiceReviewCount()).isEqualTo(2);
        assertThat(responses.get(0).getCompletedServicesCount()).isZero();
        assertThat(responses.get(0).getAverageCompletedPrice()).isNull();
        assertThat(responses.get(0).getAveragePriceMin()).isNull();
        assertThat(responses.get(0).getAveragePriceMax()).isNull();
        verify(serviceRequestRepository, never()).findAverageCompletedResponseValueByServiceId(12L, "COMPLETED");
    }

    @Test
    @DisplayName("Deve ocultar serviço público quando o freelancer está bloqueado")
    void getVisiblePublicServiceByIdWithPricing_hidesBlockedFreelancer() {
        Service service = buildService(13L, new BigDecimal("120.00"));
        service.getFreelancer().setActive(false);

        when(serviceRepository.findById(13L)).thenReturn(Optional.of(service));

        org.assertj.core.api.Assertions.assertThatThrownBy(
                        () -> serviceService.getVisiblePublicServiceByIdWithPricing(13L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Serviço não encontrado");
    }

    @Test
    @DisplayName("Deve remover da listagem pública serviços de freelancers bloqueados")
    void getAllActiveServices_filtersBlockedFreelancerServices() {
        Service visibleService = buildService(14L, new BigDecimal("90.00"));
        Service hiddenService = buildService(15L, new BigDecimal("110.00"));
        hiddenService.getFreelancer().setActive(false);

        when(serviceRepository.findByIsActiveOrderByCreatedAtDesc(true)).thenReturn(List.of(visibleService, hiddenService));
        when(reviewRepository.countByServiceId(14L)).thenReturn(0L);
        when(serviceRequestRepository.countByService_IdAndStatusAndResponseValueIsNotNull(14L, ServiceRequest.Status.COMPLETED))
                .thenReturn(0L);

        List<ServiceResponse> responses = serviceService.getAllActiveServices();

        assertThat(responses).extracting(ServiceResponse::getId).containsExactly(14L);
    }

    @Test
    @DisplayName("Deve filtrar serviços por nome do profissional quando houver categoria selecionada")
    void getActiveServices_filtersByFreelancerNameWithCategorySelection() {
        Service matchingService = buildService(21L, new BigDecimal("95.00"));
        matchingService.getFreelancer().setName("Joao Silva");

        Service nonMatchingService = buildService(22L, new BigDecimal("105.00"));
        nonMatchingService.getFreelancer().setName("Maria Souza");

        when(serviceRepository.findByServiceCategory_SlugAndIsActiveOrderByCreatedAtDesc("desenvolvimento-web", true))
                .thenReturn(List.of(matchingService, nonMatchingService));
        when(reviewRepository.countByServiceId(21L)).thenReturn(0L);
        when(serviceRequestRepository.countByService_IdAndStatusAndResponseValueIsNotNull(21L, ServiceRequest.Status.COMPLETED))
                .thenReturn(0L);

        List<ServiceResponse> responses = serviceService.getActiveServices("desenvolvimento-web", null, "joao");

        assertThat(responses).extracting(ServiceResponse::getId).containsExactly(21L);
        assertThat(responses).extracting(ServiceResponse::getFreelancerName).containsExactly("Joao Silva");
    }

    @Test
    @DisplayName("Deve registrar métricas semanais quando a busca pública for rastreável")
    void getActiveServices_tracksWeeklySearchResultsWhenRequested() {
        Service firstService = buildService(23L, new BigDecimal("90.00"));
        Service secondService = buildService(24L, new BigDecimal("130.00"));

        when(serviceRepository.findByServiceCategory_SlugAndIsActiveOrderByCreatedAtDesc("design-grafico", true))
                .thenReturn(List.of(firstService, secondService));
        when(reviewRepository.countByServiceId(23L)).thenReturn(0L);
        when(reviewRepository.countByServiceId(24L)).thenReturn(0L);
        when(serviceRequestRepository.countByService_IdAndStatusAndResponseValueIsNotNull(23L, ServiceRequest.Status.COMPLETED))
                .thenReturn(0L);
        when(serviceRequestRepository.countByService_IdAndStatusAndResponseValueIsNotNull(24L, ServiceRequest.Status.COMPLETED))
                .thenReturn(0L);

        List<ServiceResponse> responses = serviceService.getActiveServices("design-grafico", null, null, true);

        assertThat(responses).extracting(ServiceResponse::getId).containsExactly(23L, 24L);
        verify(serviceSearchWeeklyMetricService).trackSearchResults(List.of(23L, 24L));
    }

    @Test
    @DisplayName("Não deve registrar métricas quando a busca pública não for rastreável")
    void getActiveServices_skipsWeeklySearchTrackingWithoutIntent() {
        Service service = buildService(25L, new BigDecimal("75.00"));

        when(serviceRepository.findByIsActiveOrderByCreatedAtDesc(true)).thenReturn(List.of(service));
        when(reviewRepository.countByServiceId(25L)).thenReturn(0L);
        when(serviceRequestRepository.countByService_IdAndStatusAndResponseValueIsNotNull(25L, ServiceRequest.Status.COMPLETED))
                .thenReturn(0L);

        List<ServiceResponse> responses = serviceService.getActiveServices(null, null, null, true);

        assertThat(responses).extracting(ServiceResponse::getId).containsExactly(25L);
        verify(serviceSearchWeeklyMetricService, never()).trackSearchResults(any());
    }

    @Test
    @DisplayName("Deve gerar auditoria semântica com diff ao atualizar o título do serviço")
    void updateService_recordsSemanticAuditForTitleChange() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("freela@servija.com", "secret"));

        User freelancerUser = buildFreelancerUser();
        Freelancer freelancerProfile = buildFreelancerProfile(freelancerUser);

        Service service = buildService(30L, new BigDecimal("1000"));
        service.setTitle("Nome antigo");

        com.servija.servija.dto.ServiceRequest request = com.servija.servija.dto.ServiceRequest.builder()
                .title("Nome novo")
                .build();

        when(currentUserContextService.requireActiveRole(User.UserType.FREELANCER)).thenReturn(freelancerUser);
        when(freelancerRepository.findById(3L)).thenReturn(Optional.of(freelancerProfile));
        when(serviceRepository.findById(30L)).thenReturn(Optional.of(service));
        when(serviceRepository.save(service)).thenAnswer(invocation -> {
            service.setPrice(new BigDecimal("1000.00"));
            return service;
        });

        ServiceResponse response = serviceService.updateService(30L, request);

        assertThat(response.getTitle()).isEqualTo("Nome novo");
        verify(auditLogger).recordSemantic(
                eq("SERVICE_UPDATED"),
                eq("SERVICE"),
                eq("30"),
                eq("Nome novo"),
                eq(AuditLog.Result.SUCCESS),
                eq(Map.of("fieldsChanged", List.of("title"))),
                eq(Map.of("title", "Nome antigo")),
                eq(Map.of("title", "Nome novo")));
    }

    @Test
    @DisplayName("Deve fazer soft delete quando o serviço possui solicitações associadas")
    void deleteService_softDeletesWhenServiceHasRequests() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("freela@servija.com", "secret"));

        User freelancerUser = buildFreelancerUser();
        Freelancer freelancerProfile = buildFreelancerProfile(freelancerUser);
        Service service = buildService(40L, new BigDecimal("500.00"));

        when(currentUserContextService.requireActiveRole(User.UserType.FREELANCER)).thenReturn(freelancerUser);
        when(freelancerRepository.findById(3L)).thenReturn(Optional.of(freelancerProfile));
        when(serviceRepository.findById(40L)).thenReturn(Optional.of(service));
        when(serviceRequestRepository.existsByService_Id(40L)).thenReturn(true);

        serviceService.deleteService(40L);

        assertThat(service.getDeletedAt()).isNotNull();
        assertThat(service.getIsActive()).isFalse();
        verify(favoriteRepository).deleteByServiceId(40L);
        verify(serviceRepository).save(service);
        verify(serviceRepository, never()).delete(any(Service.class));
        verify(fileStorageService, never()).deleteFile(any());
    }

    @Test
    @DisplayName("Deve fazer hard delete quando o serviço não possui solicitações associadas")
    void deleteService_hardDeletesWhenServiceHasNoRequests() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("freela@servija.com", "secret"));

        User freelancerUser = buildFreelancerUser();
        Freelancer freelancerProfile = buildFreelancerProfile(freelancerUser);
        Service service = buildService(41L, new BigDecimal("500.00"));

        when(currentUserContextService.requireActiveRole(User.UserType.FREELANCER)).thenReturn(freelancerUser);
        when(freelancerRepository.findById(3L)).thenReturn(Optional.of(freelancerProfile));
        when(serviceRepository.findById(41L)).thenReturn(Optional.of(service));
        when(serviceRequestRepository.existsByService_Id(41L)).thenReturn(false);

        serviceService.deleteService(41L);

        verify(favoriteRepository).deleteByServiceId(41L);
        verify(serviceRepository).delete(service);
        verify(serviceRepository, never()).save(any(Service.class));
        assertThat(service.getDeletedAt()).isNull();
    }

    private User buildFreelancerUser() {
        return User.builder()
                .id(3L)
                .name("Freelancer")
                .email("freela@servija.com")
                .cpf("12345678901")
                .phone("11999999999")
                .userType(User.UserType.FREELANCER)
                .activeRole(User.UserType.FREELANCER)
                .roles(Set.of(User.UserType.FREELANCER))
                .active(true)
                .avatarUrl("https://cdn.servija.com/avatar.png")
                .build();
    }

    private Freelancer buildFreelancerProfile(User user) {
        Freelancer freelancer = new Freelancer();
        freelancer.setId(user.getId());
        freelancer.setUser(user);
        freelancer.setAvailable(true);
        return freelancer;
    }

    private Service buildService(Long id, BigDecimal price) {
        User freelancerUser = buildFreelancerUser();
        Freelancer freelancer = buildFreelancerProfile(freelancerUser);

        return Service.builder()
                .id(id)
                .freelancer(freelancer)
                .title("Deploy AWS")
                .category("Desenvolvimento Web")
                .description("Deploy completo")
                .price(price)
                .priceType(Service.PriceType.FIXED)
                .deliveryTime(5)
                .deliveryUnit(Service.DeliveryUnit.DAYS)
                .workModel(Service.WorkModel.REMOTO)
                .isActive(true)
                .build();
    }
}
