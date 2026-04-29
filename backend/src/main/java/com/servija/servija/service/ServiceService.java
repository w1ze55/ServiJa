package com.servija.servija.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.servija.servija.dto.ServiceRequest;
import com.servija.servija.dto.ServiceResponse;
import com.servija.servija.model.Freelancer;
import com.servija.servija.model.Service;
import com.servija.servija.model.User;
import com.servija.servija.repository.FreelancerRepository;
import com.servija.servija.repository.ReviewRepository;
import com.servija.servija.repository.ServiceRepository;
import com.servija.servija.repository.ServiceRequestRepository;
import com.servija.servija.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class ServiceService {

    private final ServiceRepository serviceRepository;
    private final FreelancerRepository freelancerRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final ServiceRequestRepository serviceRequestRepository;
    private final ReviewRepository reviewRepository;
    private final ObjectMapper objectMapper;
    private final CategoryCatalogService categoryCatalogService;
    private static final BigDecimal AVERAGE_PRICE_RANGE_SPREAD = new BigDecimal("0.20");

    /**
     * Obter o usuário autenticado atual
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
    }

    /**
     * Obter o freelancer do usuário atual
     */
    private Freelancer getCurrentFreelancer() {
        User user = getCurrentUser();
        return freelancerRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("Perfil de freelancer não encontrado"));
    }

    /**
     * Listar todos os serviços do freelancer atual
     */
    @Transactional(readOnly = true)
    public List<ServiceResponse> getMyServices() {
        Freelancer freelancer = getCurrentFreelancer();
        List<Service> services = serviceRepository.findByFreelancerIdOrderByCreatedAtDesc(freelancer.getId());
        return services.stream().map(this::toResponse).collect(Collectors.toList());
    }

    /**
     * Obter um serviço por ID
     */
    @Transactional(readOnly = true)
    public ServiceResponse getServiceById(Long id) {
        Service service = serviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Serviço não encontrado"));
        return toResponse(service);
    }

    /**
     * Criar um novo serviço
     */
    @Transactional
    public ServiceResponse createService(ServiceRequest request) {
        Freelancer freelancer = getCurrentFreelancer();
        CategoryCatalogService.CategorySelection categorySelection = resolveRequiredSelection(request);

        Service service = Service.builder()
                .freelancer(freelancer)
                .title(request.getTitle())
                .category(categorySelection.getCategoryLabel())
                .serviceCategory(categorySelection.getCategory())
                .serviceSubcategory(categorySelection.getSubcategory())
                .description(request.getDescription())
                .price(request.getPrice())
                .priceType(parseEnum(Service.PriceType.class, request.getPriceType(), Service.PriceType.FIXED))
                .deliveryTime(request.getDeliveryTime())
                .deliveryUnit(
                        parseEnum(Service.DeliveryUnit.class, request.getDeliveryUnit(), Service.DeliveryUnit.DAYS))
                .workModel(parseEnum(Service.WorkModel.class, request.getWorkModel(), Service.WorkModel.REMOTO))
                .tags(toJson(request.getTags()))
                .images(toJson(request.getImages()))
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        service = serviceRepository.save(service);
        log.info("Serviço criado: {} para freelancer {}", service.getId(), freelancer.getId());

        return toResponse(service);
    }

    /**
     * Atualizar um serviço existente
     */
    @Transactional
    public ServiceResponse updateService(Long id, ServiceRequest request) {
        Freelancer freelancer = getCurrentFreelancer();

        Service service = serviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Serviço não encontrado"));

        // Verificar se o serviço pertence ao freelancer atual
        if (!service.getFreelancer().getId().equals(freelancer.getId())) {
            throw new RuntimeException("Você não tem permissão para editar este serviço");
        }

        // Atualizar campos
        if (request.getTitle() != null) {
            service.setTitle(request.getTitle());
        }
        if (request.getCategory() != null) {
            service.setCategory(request.getCategory());
        }
        if (request.getDescription() != null) {
            service.setDescription(request.getDescription());
        }
        if (request.getPrice() != null) {
            service.setPrice(request.getPrice());
        }
        if (request.getPriceType() != null) {
            service.setPriceType(parseEnum(Service.PriceType.class, request.getPriceType(), service.getPriceType()));
        }
        if (request.getDeliveryTime() != null) {
            service.setDeliveryTime(request.getDeliveryTime());
        }
        if (request.getDeliveryUnit() != null) {
            service.setDeliveryUnit(
                    parseEnum(Service.DeliveryUnit.class, request.getDeliveryUnit(), service.getDeliveryUnit()));
        }
        if (request.getWorkModel() != null) {
            service.setWorkModel(parseEnum(Service.WorkModel.class, request.getWorkModel(), service.getWorkModel()));
        }
        if (request.getTags() != null) {
            service.setTags(toJson(request.getTags()));
        }
        if (request.getImages() != null) {
            // Coletar URLs das imagens antigas para deletar do S3
            List<String> oldImagesUrls = fromJson(service.getImages());
            List<String> newImagesUrls = request.getImages();

            service.setImages(toJson(newImagesUrls));

            // Deletar as imagens que não estão mais na lista e são URLs do S3
            for (String oldUrl : oldImagesUrls) {
                if (!newImagesUrls.contains(oldUrl) && isS3Url(oldUrl)) {
                    try {
                        fileStorageService.deleteFile(oldUrl);
                    } catch (Exception e) {
                        log.warn("Falha ao deletar imagem de serviço antiga do S3: {}. Erro: {}", oldUrl,
                                e.getMessage());
                    }
                }
            }
        }
        if (request.getIsActive() != null) {
            service.setIsActive(request.getIsActive());
        }

        if (hasCategorySelection(request)) {
            CategoryCatalogService.CategorySelection categorySelection = resolveRequiredSelection(request);
            service.setCategory(categorySelection.getCategoryLabel());
            service.setServiceCategory(categorySelection.getCategory());
            service.setServiceSubcategory(categorySelection.getSubcategory());
        }

        service = serviceRepository.save(service);
        log.info("Serviço atualizado: {}", service.getId());

        return toResponse(service);
    }

    /**
     * Excluir um serviço
     */
    @Transactional
    public void deleteService(Long id) {
        Freelancer freelancer = getCurrentFreelancer();

        Service service = serviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Serviço não encontrado"));

        // Verificar se o serviço pertence ao freelancer atual
        if (!service.getFreelancer().getId().equals(freelancer.getId())) {
            throw new RuntimeException("Você não tem permissão para excluir este serviço");
        }

        // Coletar URLs das imagens para deletar do S3
        List<String> imageUrls = fromJson(service.getImages());

        serviceRepository.delete(service);
        log.info("Serviço excluído: {}", id);

        // Deletar imagens do S3
        for (String url : imageUrls) {
            if (isS3Url(url)) {
                try {
                    fileStorageService.deleteFile(url);
                } catch (Exception e) {
                    log.warn("Falha ao deletar imagem de serviço do S3 ao excluir: {}. Erro: {}", url, e.getMessage());
                }
            }
        }
    }

    private boolean isS3Url(String url) {
        return url != null && url.contains(".s3.") && url.contains("amazonaws.com");
    }

    /**
     * Alternar status do serviço (ativar/desativar)
     */
    @Transactional
    public ServiceResponse toggleServiceStatus(Long id) {
        Freelancer freelancer = getCurrentFreelancer();

        Service service = serviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Serviço não encontrado"));

        // Verificar se o serviço pertence ao freelancer atual
        if (!service.getFreelancer().getId().equals(freelancer.getId())) {
            throw new RuntimeException("Você não tem permissão para alterar este serviço");
        }

        service.setIsActive(!service.getIsActive());
        service = serviceRepository.save(service);
        log.info("Status do serviço {} alterado para: {}", id, service.getIsActive());

        return toResponse(service);
    }

    /**
     * Obter um serviço ativo específico por ID (público)
     */
    @Transactional(readOnly = true)
    public ServiceResponse getPublicServiceById(Long id) {
        Service service = serviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Serviço não encontrado"));
        
        if (!service.getIsActive()) {
            throw new RuntimeException("Serviço não está ativo");
        }
        
        return toResponse(service);
    }

    /**
     * Listar todos os serviços ativos de um freelancer específico (público)
     */
    @Transactional(readOnly = true)
    public List<ServiceResponse> getPublicServicesByFreelancer(Long freelancerId) {
        List<Service> services = serviceRepository.findByFreelancerIdAndIsActiveOrderByCreatedAtDesc(freelancerId,
                true);
        return toPublicResponses(services);
    }

    @Transactional(readOnly = true)
    public ServiceResponse getPublicServiceByIdWithPricing(Long id) {
        Service service = serviceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Serviço não encontrado"));

        if (!service.getIsActive()) {
            throw new RuntimeException("Serviço não está ativo");
        }

        return toPublicResponse(service);
    }

    /**
     * Listar todos os serviços ativos
     */
    @Transactional(readOnly = true)
    public List<ServiceResponse> getAllActiveServices() {
        List<Service> services = serviceRepository.findByIsActiveOrderByCreatedAtDesc(true);
        return toPublicResponses(services);
    }

    @Transactional(readOnly = true)
    public List<ServiceResponse> getActiveServices(String categorySlug, String subcategorySlug, String query) {
        List<Service> services;

        if (StringUtils.hasText(subcategorySlug)) {
            services = serviceRepository.findByServiceSubcategory_SlugAndIsActiveOrderByCreatedAtDesc(subcategorySlug,
                    true);
        } else if (StringUtils.hasText(categorySlug)) {
            services = serviceRepository.findByServiceCategory_SlugAndIsActiveOrderByCreatedAtDesc(categorySlug, true);
        } else if (StringUtils.hasText(query)) {
            services = serviceRepository.searchServices(query);
        } else {
            services = serviceRepository.findByIsActiveOrderByCreatedAtDesc(true);
        }

        if (StringUtils.hasText(query) && (StringUtils.hasText(categorySlug) || StringUtils.hasText(subcategorySlug))) {
            String normalizedQuery = query.toLowerCase();
            services = services.stream()
                    .filter(service -> (service.getTitle() != null
                            && service.getTitle().toLowerCase().contains(normalizedQuery))
                            || (service.getDescription() != null
                                    && service.getDescription().toLowerCase().contains(normalizedQuery)))
                    .toList();
        }

        return toPublicResponses(services);
    }

    /**
     * Buscar serviços públicos por categoria
     */
    @Transactional(readOnly = true)
    public List<ServiceResponse> getServicesByCategory(String category) {
        List<Service> services = serviceRepository.findByServiceCategory_SlugAndIsActiveOrderByCreatedAtDesc(category,
                true);
        if (services.isEmpty()) {
            services = serviceRepository.findByServiceSubcategory_SlugAndIsActiveOrderByCreatedAtDesc(category, true);
        }
        if (services.isEmpty()) {
            services = serviceRepository.findByCategoryAndIsActiveOrderByCreatedAtDesc(category, true);
        }
        return toPublicResponses(services);
    }

    /**
     * Buscar serviços públicos
     */
    @Transactional(readOnly = true)
    public List<ServiceResponse> searchPublicServices(String query) {
        List<Service> services = serviceRepository.searchServices(query);
        return toPublicResponses(services);
    }

    /**
     * Obter estatísticas dos serviços do freelancer
     */
    @Transactional(readOnly = true)
    public ServiceStats getServiceStats() {
        Freelancer freelancer = getCurrentFreelancer();
        Long total = serviceRepository.countByFreelancerId(freelancer.getId());
        Long active = serviceRepository.countByFreelancerIdAndIsActive(freelancer.getId(), true);
        Long inactive = total - active;

        return ServiceStats.builder()
                .total(total)
                .active(active)
                .inactive(inactive)
                .build();
    }

    // ==================== MÉTODOS AUXILIARES ====================

    public ServiceResponse toResponse(Service service) {
        return ServiceResponse.builder()
                .id(service.getId())
                .freelancerId(service.getFreelancer().getId())
                .freelancerName(service.getFreelancer().getName())
                .title(service.getTitle())
                .categoryId(service.getServiceCategory() != null ? service.getServiceCategory().getId() : null)
                .categorySlug(service.getServiceCategory() != null ? service.getServiceCategory().getSlug() : null)
                .categoryLabel(service.getServiceCategory() != null ? service.getServiceCategory().getName()
                        : service.getCategory())
                .category(service.getCategory())
                .subcategoryId(service.getServiceSubcategory() != null ? service.getServiceSubcategory().getId() : null)
                .subcategorySlug(
                        service.getServiceSubcategory() != null ? service.getServiceSubcategory().getSlug() : null)
                .subcategoryLabel(
                        service.getServiceSubcategory() != null ? service.getServiceSubcategory().getName() : null)
                .subcategory(
                        service.getServiceSubcategory() != null ? service.getServiceSubcategory().getName() : null)
                .description(service.getDescription())
                .price(service.getPrice())
                .priceType(service.getPriceType().name())
                .deliveryTime(service.getDeliveryTime())
                .deliveryUnit(service.getDeliveryUnit().name())
                .workModel(service.getWorkModel() != null ? service.getWorkModel().name() : null)
                .tags(fromJson(service.getTags()))
                .images(fromJson(service.getImages()))
                .isActive(service.getIsActive())
                .freelancerProfilePhoto(service.getFreelancer().getAvatarUrl())
                .createdAt(service.getCreatedAt())
                .updatedAt(service.getUpdatedAt())
                .build();
    }

    private boolean hasCategorySelection(ServiceRequest request) {
        return request.getCategoryId() != null
                || request.getSubcategoryId() != null
                || StringUtils.hasText(request.getCategory())
                || StringUtils.hasText(request.getSubcategory());
    }

    private CategoryCatalogService.CategorySelection resolveRequiredSelection(ServiceRequest request) {
        return categoryCatalogService.resolveSelection(
                request.getCategoryId(),
                request.getSubcategoryId(),
                request.getCategory(),
                request.getSubcategory(),
                true,
                true,
                false,
                false);
    }

    private ServiceResponse toPublicResponse(Service service) {
        ServiceResponse response = toResponse(service);
        populateServiceReviewSummary(service.getId(), response);

        long completedServicesCount = serviceRequestRepository.countByService_IdAndStatusAndResponseValueIsNotNull(
                service.getId(),
                com.servija.servija.model.ServiceRequest.Status.COMPLETED);

        response.setCompletedServicesCount(completedServicesCount);
        if (completedServicesCount == 0) {
            return response;
        }

        BigDecimal averageCompletedPrice = serviceRequestRepository.findAverageCompletedResponseValueByServiceId(
                service.getId(),
                com.servija.servija.model.ServiceRequest.Status.COMPLETED.name());

        if (averageCompletedPrice == null) {
            return response;
        }

        BigDecimal normalizedAverage = averageCompletedPrice.setScale(2, RoundingMode.HALF_UP);
        response.setAverageCompletedPrice(normalizedAverage);
        response.setAveragePriceMin(calculateAveragePriceBound(normalizedAverage, true));
        response.setAveragePriceMax(calculateAveragePriceBound(normalizedAverage, false));
        return response;
    }

    private List<ServiceResponse> toPublicResponses(List<Service> services) {
        return services.stream().map(this::toPublicResponse).collect(Collectors.toList());
    }

    private void populateServiceReviewSummary(Long serviceId, ServiceResponse response) {
        long serviceReviewCount = reviewRepository.countByServiceId(serviceId);
        response.setServiceReviewCount(Math.toIntExact(serviceReviewCount));

        if (serviceReviewCount == 0) {
            return;
        }

        Double averageRating = reviewRepository.findAverageRatingByServiceId(serviceId);
        if (averageRating == null) {
            return;
        }

        response.setServiceRating(BigDecimal.valueOf(averageRating).setScale(1, RoundingMode.HALF_UP));
    }

    private BigDecimal calculateAveragePriceBound(BigDecimal averagePrice, boolean lowerBound) {
        BigDecimal halfSpread = AVERAGE_PRICE_RANGE_SPREAD.divide(new BigDecimal("2"), 4, RoundingMode.HALF_UP);
        BigDecimal multiplier = lowerBound
                ? BigDecimal.ONE.subtract(halfSpread)
                : BigDecimal.ONE.add(halfSpread);
        return averagePrice.multiply(multiplier).setScale(2, RoundingMode.HALF_UP);
    }

    private String toJson(List<String> list) {
        if (list == null || list.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            log.error("Erro ao converter lista para JSON", e);
            return null;
        }
    }

    private List<String> fromJson(String json) {
        if (json == null || json.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException e) {
            log.error("Erro ao converter JSON para lista", e);
            return new ArrayList<>();
        }
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumClass, String value, E defaultValue) {
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Enum.valueOf(enumClass, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ServiceStats {
        private Long total;
        private Long active;
        private Long inactive;
    }
}
