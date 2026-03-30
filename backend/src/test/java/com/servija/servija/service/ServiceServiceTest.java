package com.servija.servija.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.servija.servija.dto.ServiceResponse;
import com.servija.servija.model.Freelancer;
import com.servija.servija.model.Service;
import com.servija.servija.model.ServiceRequest;
import com.servija.servija.repository.FreelancerRepository;
import com.servija.servija.repository.ServiceRepository;
import com.servija.servija.repository.ServiceRequestRepository;
import com.servija.servija.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
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
    private ObjectMapper objectMapper;

    @InjectMocks
    private ServiceService serviceService;

    @Test
    @DisplayName("Deve retornar faixa média de preço quando houver serviços concluídos com valor")
    void getPublicServiceByIdWithPricing_returnsAverageRange() {
        Service service = buildService(7L, new BigDecimal("200.00"));

        when(serviceRepository.findById(7L)).thenReturn(Optional.of(service));
        when(serviceRequestRepository.countByService_IdAndStatusAndResponseValueIsNotNull(
                7L,
                ServiceRequest.Status.COMPLETED)).thenReturn(4L);
        when(serviceRequestRepository.findAverageCompletedResponseValueByServiceId(7L, "COMPLETED"))
                .thenReturn(new BigDecimal("225.00"));

        ServiceResponse response = serviceService.getPublicServiceByIdWithPricing(7L);

        assertThat(response.getPrice()).isEqualByComparingTo("200.00");
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
        when(serviceRequestRepository.countByService_IdAndStatusAndResponseValueIsNotNull(
                9L,
                ServiceRequest.Status.COMPLETED)).thenReturn(0L);

        ServiceResponse response = serviceService.getPublicServiceByIdWithPricing(9L);

        assertThat(response.getPrice()).isEqualByComparingTo("180.00");
        assertThat(response.getCompletedServicesCount()).isZero();
        assertThat(response.getAverageCompletedPrice()).isNull();
        assertThat(response.getAveragePriceMin()).isNull();
        assertThat(response.getAveragePriceMax()).isNull();
        verify(serviceRequestRepository, never()).findAverageCompletedResponseValueByServiceId(9L, "COMPLETED");
    }

    private Service buildService(Long id, BigDecimal price) {
        return Service.builder()
                .id(id)
                .freelancer(Freelancer.builder()
                        .id(3L)
                        .name("Freelancer")
                        .avatarUrl("https://cdn.servija.com/avatar.png")
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
