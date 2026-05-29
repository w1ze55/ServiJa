package com.servija.servija.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.servija.servija.dto.CategoryTreeResponse;
import com.servija.servija.dto.ServiceCategoryRequest;
import com.servija.servija.dto.ServiceCategoryResponse;
import com.servija.servija.dto.ServiceSubcategoryRequest;
import com.servija.servija.dto.ServiceSubcategoryResponse;
import com.servija.servija.model.Freelancer;
import com.servija.servija.model.PortfolioProject;
import com.servija.servija.model.ServiceCategory;
import com.servija.servija.model.ServiceRequest;
import com.servija.servija.model.ServiceSubcategory;
import com.servija.servija.repository.FreelancerRepository;
import com.servija.servija.repository.PortfolioProjectRepository;
import com.servija.servija.repository.ServiceCategoryRepository;
import com.servija.servija.repository.ServiceRepository;
import com.servija.servija.repository.ServiceRequestRepository;
import com.servija.servija.repository.ServiceSubcategoryRepository;
import com.servija.servija.util.SlugUtils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CategoryCatalogService {

    private static final String FALLBACK_CATEGORY_NAME = "Outros";
    private static final String FALLBACK_SUBCATEGORY_NAME = "Geral";

    private final ServiceCategoryRepository serviceCategoryRepository;
    private final ServiceSubcategoryRepository serviceSubcategoryRepository;
    private final ServiceRepository serviceRepository;
    private final FreelancerRepository freelancerRepository;
    private final PortfolioProjectRepository portfolioProjectRepository;
    private final ServiceRequestRepository serviceRequestRepository;
    private final ObjectMapper objectMapper;
    private final AuditLogger auditLogger;

    @Value("${servija.catalog.seed-resource:classpath:catalog/service-category-catalog.json}")
    private Resource seedResource;

    @Transactional(readOnly = true)
    public CategoryTreeResponse getPublicTree() {
        return CategoryTreeResponse.builder()
                .categories(serviceCategoryRepository.findActiveWithSubcategories().stream().map(this::toPublicCategoryNode).toList())
                .build();
    }

    @Transactional(readOnly = true)
    public List<ServiceCategoryResponse> getAdminCategories() {
        return serviceCategoryRepository.findAllWithSubcategories().stream().map(this::toCategoryResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ServiceSubcategoryResponse> getAdminSubcategories(Long categoryId) {
        List<ServiceSubcategory> subcategories = categoryId == null
                ? serviceSubcategoryRepository.findAll().stream()
                        .sorted(Comparator
                                .comparing((ServiceSubcategory subcategory) -> subcategory.getCategory().getSortOrder())
                                .thenComparing(subcategory -> subcategory.getCategory().getName())
                                .thenComparing(ServiceSubcategory::getSortOrder)
                                .thenComparing(ServiceSubcategory::getName))
                        .toList()
                : serviceSubcategoryRepository.findByCategoryOrderBySortOrderAscNameAsc(getCategoryOrThrow(categoryId));
        return subcategories.stream().map(this::toSubcategoryResponse).toList();
    }

    @Transactional
    public ServiceCategoryResponse createCategory(ServiceCategoryRequest request) {
        String normalizedName = normalizeName(request.getName());
        ServiceCategory category = serviceCategoryRepository.save(ServiceCategory.builder()
                .name(normalizedName)
                .slug(ensureUniqueCategorySlug(normalizedName, null))
                .sortOrder(request.getSortOrder())
                .active(true)
                .build());
        ServiceCategoryResponse response = toCategoryResponse(category);
        auditLogger.recordSemantic(
                "CATEGORY_CREATED",
                "CATEGORY",
                String.valueOf(category.getId()),
                category.getName(),
                com.servija.servija.model.AuditLog.Result.SUCCESS,
                request,
                response,
                null,
                null);
        return response;
    }

    @Transactional
    public ServiceCategoryResponse updateCategory(Long categoryId, ServiceCategoryRequest request) {
        ServiceCategory category = getCategoryOrThrow(categoryId);
        Map<String, Object> beforeState = buildCategoryAuditState(category);
        category.setName(normalizeName(request.getName()));
        category.setSortOrder(request.getSortOrder());
        ServiceCategory saved = serviceCategoryRepository.save(category);
        recordCategoryAudit("CATEGORY_UPDATED", saved, beforeState);
        return toCategoryResponse(saved);
    }

    @Transactional
    public ServiceCategoryResponse archiveCategory(Long categoryId) {
        ServiceCategory category = getCategoryOrThrow(categoryId);
        Map<String, Object> beforeState = buildCategoryAuditState(category);
        category.setActive(false);
        if (category.getSubcategories() != null) {
            category.getSubcategories().forEach(subcategory -> subcategory.setActive(false));
        }
        ServiceCategory saved = serviceCategoryRepository.save(category);
        Map<String, Object> afterState = buildCategoryAuditState(saved);
        List<String> changedFields = AuditStateDiffUtils.changedFields(beforeState, afterState);
        auditLogger.recordSemantic(
                "CATEGORY_ARCHIVED",
                "CATEGORY",
                String.valueOf(saved.getId()),
                saved.getName(),
                com.servija.servija.model.AuditLog.Result.SUCCESS,
                Map.of("fieldsChanged", changedFields),
                AuditStateDiffUtils.filterFields(beforeState, changedFields),
                AuditStateDiffUtils.filterFields(afterState, changedFields));
        return toCategoryResponse(saved);
    }

    @Transactional
    public ServiceCategoryResponse restoreCategory(Long categoryId) {
        ServiceCategory category = getCategoryOrThrow(categoryId);
        Map<String, Object> beforeState = buildCategoryAuditState(category);
        category.setActive(true);
        ServiceCategory saved = serviceCategoryRepository.save(category);
        Map<String, Object> afterState = buildCategoryAuditState(saved);
        List<String> changedFields = AuditStateDiffUtils.changedFields(beforeState, afterState);
        auditLogger.recordSemantic(
                "CATEGORY_RESTORED",
                "CATEGORY",
                String.valueOf(saved.getId()),
                saved.getName(),
                com.servija.servija.model.AuditLog.Result.SUCCESS,
                Map.of("fieldsChanged", changedFields),
                AuditStateDiffUtils.filterFields(beforeState, changedFields),
                AuditStateDiffUtils.filterFields(afterState, changedFields));
        return toCategoryResponse(saved);
    }

    @Transactional
    public ServiceSubcategoryResponse createSubcategory(ServiceSubcategoryRequest request) {
        ServiceCategory category = getCategoryOrThrow(request.getCategoryId());
        String normalizedName = normalizeName(request.getName());
        ServiceSubcategory subcategory = serviceSubcategoryRepository.save(ServiceSubcategory.builder()
                .category(category)
                .name(normalizedName)
                .slug(ensureUniqueSubcategorySlug(category, normalizedName, null))
                .sortOrder(request.getSortOrder())
                .active(true)
                .build());
        ServiceSubcategoryResponse response = toSubcategoryResponse(subcategory);
        auditLogger.recordSemantic(
                "SUBCATEGORY_CREATED",
                "SUBCATEGORY",
                String.valueOf(subcategory.getId()),
                subcategory.getName(),
                com.servija.servija.model.AuditLog.Result.SUCCESS,
                request,
                response,
                null,
                null);
        return response;
    }

    @Transactional
    public ServiceSubcategoryResponse updateSubcategory(Long subcategoryId, ServiceSubcategoryRequest request) {
        ServiceSubcategory subcategory = getSubcategoryOrThrow(subcategoryId);
        ServiceCategory category = getCategoryOrThrow(request.getCategoryId());
        Map<String, Object> beforeState = buildSubcategoryAuditState(subcategory);
        String normalizedName = normalizeName(request.getName());
        if (!subcategory.getCategory().getId().equals(category.getId()) || !subcategory.getName().equalsIgnoreCase(normalizedName)) {
            subcategory.setSlug(ensureUniqueSubcategorySlug(category, normalizedName, subcategory.getId()));
        }
        subcategory.setCategory(category);
        subcategory.setName(normalizedName);
        subcategory.setSortOrder(request.getSortOrder());
        ServiceSubcategory saved = serviceSubcategoryRepository.save(subcategory);
        recordSubcategoryAudit("SUBCATEGORY_UPDATED", saved, beforeState);
        return toSubcategoryResponse(saved);
    }

    @Transactional
    public ServiceSubcategoryResponse archiveSubcategory(Long subcategoryId) {
        ServiceSubcategory subcategory = getSubcategoryOrThrow(subcategoryId);
        Map<String, Object> beforeState = buildSubcategoryAuditState(subcategory);
        subcategory.setActive(false);
        ServiceSubcategory saved = serviceSubcategoryRepository.save(subcategory);
        Map<String, Object> afterState = buildSubcategoryAuditState(saved);
        List<String> changedFields = AuditStateDiffUtils.changedFields(beforeState, afterState);
        auditLogger.recordSemantic(
                "SUBCATEGORY_ARCHIVED",
                "SUBCATEGORY",
                String.valueOf(saved.getId()),
                saved.getName(),
                com.servija.servija.model.AuditLog.Result.SUCCESS,
                Map.of("fieldsChanged", changedFields),
                AuditStateDiffUtils.filterFields(beforeState, changedFields),
                AuditStateDiffUtils.filterFields(afterState, changedFields));
        return toSubcategoryResponse(saved);
    }

    @Transactional
    public ServiceSubcategoryResponse restoreSubcategory(Long subcategoryId) {
        ServiceSubcategory subcategory = getSubcategoryOrThrow(subcategoryId);
        if (!Boolean.TRUE.equals(subcategory.getCategory().getActive())) {
            throw new IllegalArgumentException("A categoria pai precisa estar ativa para restaurar a subcategoria");
        }
        Map<String, Object> beforeState = buildSubcategoryAuditState(subcategory);
        subcategory.setActive(true);
        ServiceSubcategory saved = serviceSubcategoryRepository.save(subcategory);
        Map<String, Object> afterState = buildSubcategoryAuditState(saved);
        List<String> changedFields = AuditStateDiffUtils.changedFields(beforeState, afterState);
        auditLogger.recordSemantic(
                "SUBCATEGORY_RESTORED",
                "SUBCATEGORY",
                String.valueOf(saved.getId()),
                saved.getName(),
                com.servija.servija.model.AuditLog.Result.SUCCESS,
                Map.of("fieldsChanged", changedFields),
                AuditStateDiffUtils.filterFields(beforeState, changedFields),
                AuditStateDiffUtils.filterFields(afterState, changedFields));
        return toSubcategoryResponse(saved);
    }

    @Transactional(readOnly = true)
    public CategorySelection resolveSelection(Long categoryId, Long subcategoryId, String categoryInput,
            String subcategoryInput, boolean requireActive, boolean requireSubcategoryWhenAvailable,
            boolean fallbackToOther, boolean allowEmpty) {
        boolean hasCategoryInput = categoryId != null || StringUtils.hasText(categoryInput);
        boolean hasSubcategoryInput = subcategoryId != null || StringUtils.hasText(subcategoryInput);
        if (!hasCategoryInput && !hasSubcategoryInput) {
            if (allowEmpty) {
                return CategorySelection.empty();
            }
            if (fallbackToOther) {
                return getFallbackSelection(requireActive);
            }
            throw new IllegalArgumentException("Selecione uma categoria");
        }

        ServiceCategory category = null;
        ServiceSubcategory subcategory = null;

        if (subcategoryId != null) {
            subcategory = getSubcategoryOrThrow(subcategoryId);
            validateEntityActive(subcategory.getActive(), requireActive, "subcategoria");
            category = subcategory.getCategory();
        }
        if (categoryId != null) {
            category = getCategoryOrThrow(categoryId);
            validateEntityActive(category.getActive(), requireActive, "categoria");
        }
        if (category == null && StringUtils.hasText(categoryInput)) {
            category = findCategoryByInput(categoryInput).orElse(null);
            if (category != null) {
                validateEntityActive(category.getActive(), requireActive, "categoria");
            }
        }
        if (subcategory == null && StringUtils.hasText(subcategoryInput)) {
            subcategory = findSubcategoryByInput(subcategoryInput).orElse(null);
            if (subcategory != null) {
                validateEntityActive(subcategory.getActive(), requireActive, "subcategoria");
                if (category == null) {
                    category = subcategory.getCategory();
                }
            }
        }
        if (category == null && StringUtils.hasText(categoryInput)) {
            subcategory = findSubcategoryByInput(categoryInput).orElse(null);
            if (subcategory != null) {
                validateEntityActive(subcategory.getActive(), requireActive, "subcategoria");
                category = subcategory.getCategory();
            }
        }
        if (category == null) {
            if (fallbackToOther) {
                return getFallbackSelection(requireActive);
            }
            if (allowEmpty) {
                return CategorySelection.empty();
            }
            throw new IllegalArgumentException("Categoria invalida");
        }

        validateEntityActive(category.getActive(), requireActive, "categoria");
        if (subcategory != null && !subcategory.getCategory().getId().equals(category.getId())) {
            throw new IllegalArgumentException("A subcategoria nao pertence a categoria informada");
        }
        if (subcategory == null && StringUtils.hasText(subcategoryInput)) {
            subcategory = serviceSubcategoryRepository.findByNameIgnoreCaseAndCategoryId(normalizeName(subcategoryInput), category.getId())
                    .orElse(null);
            if (subcategory != null) {
                validateEntityActive(subcategory.getActive(), requireActive, "subcategoria");
            }
        }
        long activeSubcategories = serviceSubcategoryRepository.countByCategoryIdAndActiveTrue(category.getId());
        if (requireSubcategoryWhenAvailable && activeSubcategories > 0 && subcategory == null) {
            throw new IllegalArgumentException("Selecione uma subcategoria");
        }
        return CategorySelection.of(category, subcategory);
    }

    @Transactional(readOnly = true)
    public CategorySelection getFallbackSelection(boolean requireActive) {
        ServiceCategory fallbackCategory = serviceCategoryRepository.findByNameIgnoreCase(FALLBACK_CATEGORY_NAME)
                .orElseThrow(() -> new IllegalStateException("Categoria fallback nao encontrada"));
        ServiceSubcategory fallbackSubcategory = serviceSubcategoryRepository
                .findByNameIgnoreCaseAndCategoryId(FALLBACK_SUBCATEGORY_NAME, fallbackCategory.getId())
                .orElseThrow(() -> new IllegalStateException("Subcategoria fallback nao encontrada"));
        validateEntityActive(fallbackCategory.getActive(), requireActive, "categoria");
        validateEntityActive(fallbackSubcategory.getActive(), requireActive, "subcategoria");
        return CategorySelection.of(fallbackCategory, fallbackSubcategory);
    }

    @Transactional
    public void seedCatalogIfNeeded() {
        for (SeedCategory seedCategory : readSeedCategories()) {
            String categoryName = normalizeName(seedCategory.getName());
            String categorySlug = SlugUtils.toSlug(categoryName);
            ServiceCategory category = serviceCategoryRepository.findBySlug(categorySlug)
                    .orElseGet(() -> serviceCategoryRepository.save(ServiceCategory.builder()
                            .name(categoryName)
                            .slug(categorySlug)
                            .sortOrder(seedCategory.getSortOrder())
                            .active(true)
                            .build()));

            List<SeedSubcategory> subcategories = seedCategory.getSubcategories() != null ? seedCategory.getSubcategories() : List.of();
            for (SeedSubcategory seedSubcategory : subcategories) {
                String subcategoryName = normalizeName(seedSubcategory.getName());
                String subcategorySlug = buildSubcategorySlug(category.getSlug(), subcategoryName);
                serviceSubcategoryRepository.findBySlug(subcategorySlug).orElseGet(() -> serviceSubcategoryRepository.save(
                        ServiceSubcategory.builder()
                                .category(category)
                                .name(subcategoryName)
                                .slug(subcategorySlug)
                                .sortOrder(seedSubcategory.getSortOrder())
                                .active(true)
                                .build()));
            }
        }
    }

    @Transactional
    public void backfillLegacyAssignments() {
        int updatedServices = 0;
        List<com.servija.servija.model.Service> services = serviceRepository.findAll();
        for (com.servija.servija.model.Service service : services) {
            if (service.getServiceCategory() != null || service.getServiceSubcategory() != null) {
                continue;
            }
            CategorySelection selection = resolveSelection(null, null, service.getCategory(), null, false, false, true, false);
            service.setServiceCategory(selection.getCategory());
            service.setServiceSubcategory(selection.getSubcategory());
            if (!StringUtils.hasText(service.getCategory()) && selection.getCategoryLabel() != null) {
                service.setCategory(selection.getCategoryLabel());
            }
            updatedServices++;
        }
        if (updatedServices > 0) {
            serviceRepository.saveAll(services);
        }

        int updatedFreelancers = 0;
        List<Freelancer> freelancers = freelancerRepository.findAll();
        for (Freelancer freelancer : freelancers) {
            if (freelancer.getServiceCategory() != null || freelancer.getServiceSubcategory() != null) {
                continue;
            }
            CategorySelection selection = resolveSelection(null, null, firstNonBlank(freelancer.getCategory(), freelancer.getServiceType()), null, false, false, true, false);
            freelancer.setServiceCategory(selection.getCategory());
            freelancer.setServiceSubcategory(selection.getSubcategory());
            if (!StringUtils.hasText(freelancer.getCategory())) {
                freelancer.setCategory(selection.getCategoryLabel());
            }
            if (!StringUtils.hasText(freelancer.getServiceType()) && selection.getSubcategoryLabel() != null) {
                freelancer.setServiceType(selection.getSubcategoryLabel());
            }
            updatedFreelancers++;
        }
        if (updatedFreelancers > 0) {
            freelancerRepository.saveAll(freelancers);
        }

        int updatedProjects = 0;
        List<PortfolioProject> projects = portfolioProjectRepository.findAll();
        for (PortfolioProject project : projects) {
            if (project.getServiceCategory() != null || project.getServiceSubcategory() != null) {
                continue;
            }
            CategorySelection selection = resolveSelection(null, null, project.getCategory(), null, false, false, true, false);
            project.setServiceCategory(selection.getCategory());
            project.setServiceSubcategory(selection.getSubcategory());
            if (!StringUtils.hasText(project.getCategory())) {
                project.setCategory(selection.getCategoryLabel());
            }
            updatedProjects++;
        }
        if (updatedProjects > 0) {
            portfolioProjectRepository.saveAll(projects);
        }

        int updatedRequests = 0;
        List<ServiceRequest> requests = serviceRequestRepository.findAll();
        for (ServiceRequest request : requests) {
            if (request.getServiceCategory() != null || request.getServiceSubcategory() != null) {
                continue;
            }
            CategorySelection selection = resolveSelection(null, null, request.getCategory(), request.getSubcategory(), false, false, true, false);
            serviceRequestRepository.backfillCatalogAssignment(
                    request.getId(),
                    selection.getCategoryId(),
                    selection.getSubcategoryId());
            updatedRequests++;
        }

        if (updatedServices + updatedFreelancers + updatedProjects + updatedRequests > 0) {
            log.info("Backfill de catalogo concluido. services={}, freelancers={}, portfolioProjects={}, serviceRequests={}",
                    updatedServices, updatedFreelancers, updatedProjects, updatedRequests);
        }
    }

    private List<SeedCategory> readSeedCategories() {
        try (InputStream inputStream = seedResource.getInputStream()) {
            SeedCatalog seedCatalog = objectMapper.readValue(inputStream, new TypeReference<SeedCatalog>() {});
            return seedCatalog == null || seedCatalog.getCategories() == null ? List.of() : seedCatalog.getCategories();
        } catch (IOException exception) {
            throw new IllegalStateException("Nao foi possivel carregar o seed de categorias", exception);
        }
    }

    private String ensureUniqueCategorySlug(String name, Long currentId) {
        String slug = SlugUtils.toSlug(name);
        Optional<ServiceCategory> existing = serviceCategoryRepository.findBySlug(slug);
        if (existing.isPresent() && !Objects.equals(existing.get().getId(), currentId)) {
            throw new IllegalArgumentException("Ja existe uma categoria com esse nome");
        }
        return slug;
    }

    private String ensureUniqueSubcategorySlug(ServiceCategory category, String name, Long currentId) {
        String slug = buildSubcategorySlug(category.getSlug(), name);
        Optional<ServiceSubcategory> existing = serviceSubcategoryRepository.findBySlug(slug);
        if (existing.isPresent() && !Objects.equals(existing.get().getId(), currentId)) {
            throw new IllegalArgumentException("Ja existe uma subcategoria com esse nome nessa categoria");
        }
        return slug;
    }

    private String buildSubcategorySlug(String categorySlug, String subcategoryName) {
        return categorySlug + "-" + SlugUtils.toSlug(subcategoryName);
    }

    private Optional<ServiceCategory> findCategoryByInput(String input) {
        if (!StringUtils.hasText(input)) {
            return Optional.empty();
        }
        String normalizedInput = normalizeName(input);
        return serviceCategoryRepository.findBySlug(SlugUtils.toSlug(normalizedInput))
                .or(() -> serviceCategoryRepository.findByNameIgnoreCase(normalizedInput));
    }

    private Optional<ServiceSubcategory> findSubcategoryByInput(String input) {
        if (!StringUtils.hasText(input)) {
            return Optional.empty();
        }
        String normalizedInput = normalizeName(input);
        return serviceSubcategoryRepository.findBySlug(SlugUtils.toSlug(normalizedInput))
                .or(() -> serviceSubcategoryRepository.findByNameIgnoreCase(normalizedInput));
    }

    private String normalizeName(String value) {
        return StringUtils.hasText(value) ? value.trim().replaceAll("\\s+", " ") : null;
    }

    private void validateEntityActive(Boolean active, boolean requireActive, String entityName) {
        if (requireActive && !Boolean.TRUE.equals(active)) {
            throw new IllegalArgumentException("A " + entityName + " informada esta arquivada");
        }
    }

    private String firstNonBlank(String first, String second) {
        return StringUtils.hasText(first) ? first : (StringUtils.hasText(second) ? second : null);
    }

    private ServiceCategory getCategoryOrThrow(Long categoryId) {
        return serviceCategoryRepository.findById(categoryId).orElseThrow(() -> new IllegalArgumentException("Categoria nao encontrada"));
    }

    private ServiceSubcategory getSubcategoryOrThrow(Long subcategoryId) {
        return serviceSubcategoryRepository.findById(subcategoryId).orElseThrow(() -> new IllegalArgumentException("Subcategoria nao encontrada"));
    }

    private CategoryTreeResponse.CategoryNode toPublicCategoryNode(ServiceCategory category) {
        List<CategoryTreeResponse.SubcategoryNode> subcategories = category.getSubcategories().stream()
                .filter(subcategory -> Boolean.TRUE.equals(subcategory.getActive()))
                .sorted(Comparator.comparing(ServiceSubcategory::getSortOrder).thenComparing(ServiceSubcategory::getName))
                .map(subcategory -> CategoryTreeResponse.SubcategoryNode.builder()
                        .id(subcategory.getId())
                        .categoryId(category.getId())
                        .slug(subcategory.getSlug())
                        .name(subcategory.getName())
                        .sortOrder(subcategory.getSortOrder())
                        .active(subcategory.getActive())
                        .build())
                .toList();
        return CategoryTreeResponse.CategoryNode.builder()
                .id(category.getId())
                .slug(category.getSlug())
                .name(category.getName())
                .sortOrder(category.getSortOrder())
                .active(category.getActive())
                .subcategories(subcategories)
                .build();
    }

    private ServiceCategoryResponse toCategoryResponse(ServiceCategory category) {
        List<ServiceSubcategoryResponse> subcategories = category.getSubcategories() == null ? new ArrayList<>()
                : category.getSubcategories().stream()
                        .sorted(Comparator.comparing(ServiceSubcategory::getSortOrder).thenComparing(ServiceSubcategory::getName))
                        .map(this::toSubcategoryResponse)
                        .toList();
        return ServiceCategoryResponse.builder()
                .id(category.getId())
                .slug(category.getSlug())
                .name(category.getName())
                .sortOrder(category.getSortOrder())
                .active(category.getActive())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .subcategories(subcategories)
                .build();
    }

    private ServiceSubcategoryResponse toSubcategoryResponse(ServiceSubcategory subcategory) {
        return ServiceSubcategoryResponse.builder()
                .id(subcategory.getId())
                .categoryId(subcategory.getCategory().getId())
                .categorySlug(subcategory.getCategory().getSlug())
                .categoryName(subcategory.getCategory().getName())
                .slug(subcategory.getSlug())
                .name(subcategory.getName())
                .sortOrder(subcategory.getSortOrder())
                .active(subcategory.getActive())
                .createdAt(subcategory.getCreatedAt())
                .updatedAt(subcategory.getUpdatedAt())
                .build();
    }

    private Map<String, Object> buildCategoryAuditState(ServiceCategory category) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("name", category.getName());
        state.put("sortOrder", category.getSortOrder());
        state.put("active", category.getActive());
        return state;
    }

    private Map<String, Object> buildSubcategoryAuditState(ServiceSubcategory subcategory) {
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("categoryId", subcategory.getCategory() != null ? subcategory.getCategory().getId() : null);
        state.put("categoryName", subcategory.getCategory() != null ? subcategory.getCategory().getName() : null);
        state.put("name", subcategory.getName());
        state.put("sortOrder", subcategory.getSortOrder());
        state.put("active", subcategory.getActive());
        return state;
    }

    private void recordCategoryAudit(String action, ServiceCategory category, Map<String, Object> beforeState) {
        Map<String, Object> afterState = buildCategoryAuditState(category);
        List<String> changedFields = AuditStateDiffUtils.changedFields(beforeState, afterState);
        if (changedFields.isEmpty()) {
            return;
        }
        auditLogger.recordSemantic(
                action,
                "CATEGORY",
                String.valueOf(category.getId()),
                category.getName(),
                com.servija.servija.model.AuditLog.Result.SUCCESS,
                Map.of("fieldsChanged", changedFields),
                AuditStateDiffUtils.filterFields(beforeState, changedFields),
                AuditStateDiffUtils.filterFields(afterState, changedFields));
    }

    private void recordSubcategoryAudit(String action, ServiceSubcategory subcategory, Map<String, Object> beforeState) {
        Map<String, Object> afterState = buildSubcategoryAuditState(subcategory);
        List<String> changedFields = AuditStateDiffUtils.changedFields(beforeState, afterState);
        if (changedFields.isEmpty()) {
            return;
        }
        auditLogger.recordSemantic(
                action,
                "SUBCATEGORY",
                String.valueOf(subcategory.getId()),
                subcategory.getName(),
                com.servija.servija.model.AuditLog.Result.SUCCESS,
                Map.of("fieldsChanged", changedFields),
                AuditStateDiffUtils.filterFields(beforeState, changedFields),
                AuditStateDiffUtils.filterFields(afterState, changedFields));
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategorySelection {
        private ServiceCategory category;
        private ServiceSubcategory subcategory;
        public static CategorySelection of(ServiceCategory category, ServiceSubcategory subcategory) {
            return CategorySelection.builder().category(category).subcategory(subcategory).build();
        }
        public static CategorySelection empty() {
            return CategorySelection.builder().build();
        }
        public Long getCategoryId() { return category != null ? category.getId() : null; }
        public String getCategorySlug() { return category != null ? category.getSlug() : null; }
        public String getCategoryLabel() { return category != null ? category.getName() : null; }
        public Long getSubcategoryId() { return subcategory != null ? subcategory.getId() : null; }
        public String getSubcategorySlug() { return subcategory != null ? subcategory.getSlug() : null; }
        public String getSubcategoryLabel() { return subcategory != null ? subcategory.getName() : null; }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class SeedCatalog {
        private List<SeedCategory> categories;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class SeedCategory {
        private String name;
        private Integer sortOrder;
        private List<SeedSubcategory> subcategories;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class SeedSubcategory {
        private String name;
        private Integer sortOrder;
    }
}
