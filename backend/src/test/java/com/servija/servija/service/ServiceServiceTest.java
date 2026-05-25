package com.servija.servija.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.servija.servija.dto.ServiceResponse;
import com.servija.servija.exception.ResourceNotFoundException;
import com.servija.servija.model.AuditLog;
import com.servija.servija.model.Freelancer;
import com.servija.servija.model.Service;
import com.servija.servija.model.ServiceRequest;
import com.servija.servija.repository.FreelancerRepository;
import com.servija.servija.repository.ReviewRepository;
import com.servija.servija.repository.ServiceRepository;
import com.servija.servija.repository.ServiceRequestRepository;
import com.servija.servija.repository.UserRepository;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ServiceServiceTest {

    @Mock
    private ServiceRepository serviceRepository;

    @Mock
    private FreelancerRepository freelancerRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private ServiceRequestRepository serviceRequestRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private CategoryCatalogService categoryCatalogService;

    @Mock
    private AuditLogger auditLogger;

    @InjectMocks
    private ServiceService serviceService;

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
        when(serviceRequestRepository.countByService_IdAndStatusAndResponseValueIsNotNull(
                7L,
                ServiceRequest.Status.COMPLETED)).thenReturn(4L);
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
        when(serviceRequestRepository.countByService_IdAndStatusAndResponseValueIsNotNull(
                9L,
                ServiceRequest.Status.COMPLETED)).thenReturn(0L);

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
        when(serviceRequestRepository.countByService_IdAndStatusAndResponseValueIsNotNull(
                11L,
                ServiceRequest.Status.COMPLETED)).thenReturn(3L);
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
        when(serviceRequestRepository.countByService_IdAndStatusAndResponseValueIsNotNull(
                12L,
                ServiceRequest.Status.COMPLETED)).thenReturn(0L);

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
        when(serviceRequestRepository.countByService_IdAndStatusAndResponseValueIsNotNull(
                14L,
                ServiceRequest.Status.COMPLETED)).thenReturn(0L);

        List<ServiceResponse> responses = serviceService.getAllActiveServices();

        assertThat(responses).extracting(ServiceResponse::getId).containsExactly(14L);
    }

    @Test
    @DisplayName("Deve gerar auditoria semantica com diff ao atualizar o titulo do servico")
    void updateService_recordsSemanticAuditForTitleChange() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("freela@servija.com", "secret"));

        Freelancer freelancer = Freelancer.builder()
                .id(3L)
                .email("freela@servija.com")
                .name("Freelancer")
                .active(true)
                .build();

        Service service = buildService(30L, new BigDecimal("250.00"));
        service.setTitle("Nome antigo");

        com.servija.servija.dto.ServiceRequest request = com.servija.servija.dto.ServiceRequest.builder()
                .title("Nome novo")
                .build();

        when(userRepository.findByEmail("freela@servija.com")).thenReturn(Optional.of(freelancer));
        when(freelancerRepository.findById(3L)).thenReturn(Optional.of(freelancer));
        when(serviceRepository.findById(30L)).thenReturn(Optional.of(service));
        when(serviceRepository.save(service)).thenReturn(service);

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

    private Service buildService(Long id, BigDecimal price) {
        return Service.builder()
                .id(id)
                .freelancer(Freelancer.builder()
                        .id(3L)
                        .name("Freelancer")
                        .avatarUrl("https://cdn.servija.com/avatar.png")
                        .active(true)
                        .build())
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
