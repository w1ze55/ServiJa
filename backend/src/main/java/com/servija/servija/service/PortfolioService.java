package com.servija.servija.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.servija.servija.dto.PortfolioProjectRequest;
import com.servija.servija.dto.PortfolioProjectResponse;
import com.servija.servija.model.Freelancer;
import com.servija.servija.model.PortfolioImage;
import com.servija.servija.model.PortfolioProject;
import com.servija.servija.repository.PortfolioImageRepository;
import com.servija.servija.repository.PortfolioProjectRepository;
import com.servija.servija.util.PublicVisibilityRules;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PortfolioService {

    private final PortfolioProjectRepository projectRepository;
    private final PortfolioImageRepository imageRepository;
    private final FreelancerService freelancerService;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;
    private final CategoryCatalogService categoryCatalogService;
    private final AuditLogger auditLogger;

    @Transactional(readOnly = true)
    public List<PortfolioProjectResponse> getProjects() {
        Freelancer freelancer = freelancerService.getCurrentFreelancer();
        List<PortfolioProject> projects = projectRepository.findByFreelancerIdOrderByCreatedAtDesc(freelancer.getId());
        return projects.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<PortfolioProjectResponse> getPublicProjects(Long freelancerId) {
        List<PortfolioProject> projects = projectRepository.findByFreelancerIdOrderByCreatedAtDesc(freelancerId);
        return projects.stream()
                .filter(project -> PublicVisibilityRules.isFreelancerPublic(project.getFreelancer()))
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PortfolioProjectResponse getProject(Long projectId) {
        Freelancer freelancer = freelancerService.getCurrentFreelancer();
        PortfolioProject project = projectRepository.findByIdAndFreelancerId(projectId, freelancer.getId())
                .orElseThrow(() -> new RuntimeException("Projeto não encontrado"));
        return mapToResponse(project);
    }

    @Transactional
    public PortfolioProjectResponse createProject(PortfolioProjectRequest request) {
        Freelancer freelancer = freelancerService.getCurrentFreelancer();
        CategoryCatalogService.CategorySelection categorySelection = resolveRequiredSelection(request);

        PortfolioProject project = PortfolioProject.builder()
                .freelancer(freelancer)
                .title(request.getTitle())
                .description(request.getDescription())
                .category(categorySelection.getCategoryLabel())
                .serviceCategory(categorySelection.getCategory())
                .serviceSubcategory(categorySelection.getSubcategory())
                .videoUrl(request.getVideoUrl())
                .projectUrl(request.getProjectUrl())
                .completedDate(request.getCompletedDate())
                .client(request.getClient())
                .featured(request.getFeatured() != null ? request.getFeatured() : false)
                .build();

        // Converter tags para JSON
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            try {
                project.setTags(objectMapper.writeValueAsString(request.getTags()));
            } catch (JsonProcessingException e) {
                log.error("Erro ao serializar tags", e);
            }
        }

        project = projectRepository.save(project);

        // Adicionar imagens
        if (request.getImages() != null && !request.getImages().isEmpty()) {
            PortfolioProject finalProject = project;
            List<PortfolioImage> images = request.getImages().stream()
                    .map(imgReq -> PortfolioImage.builder()
                            .project(finalProject)
                            .imageUrl(imgReq.getImageUrl())
                            .caption(imgReq.getCaption())
                            .displayOrder(imgReq.getDisplayOrder() != null ? imgReq.getDisplayOrder() : 0)
                            .build())
                    .collect(Collectors.toList());

            imageRepository.saveAll(images);
            project.setImages(images);
        }

        return mapToResponse(project);
    }

    @Transactional
    public PortfolioProjectResponse updateProject(Long projectId, PortfolioProjectRequest request) {
        Freelancer freelancer = freelancerService.getCurrentFreelancer();
        log.info("Tentando atualizar projeto {} do freelancer {}", projectId, freelancer.getId());

        PortfolioProject project = projectRepository.findByIdAndFreelancerId(projectId, freelancer.getId())
                .orElseThrow(() -> {
                    log.error("Projeto {} não encontrado para o freelancer {}", projectId, freelancer.getId());
                    return new RuntimeException("Projeto não encontrado");
                });

        log.info("Projeto encontrado: {}", project.getTitle());
        Map<String, Object> beforeState = buildPortfolioAuditState(project);

        // Atualizar campos
        if (request.getTitle() != null) {
            project.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            project.setDescription(request.getDescription());
        }
        if (request.getTags() != null) {
            try {
                project.setTags(objectMapper.writeValueAsString(request.getTags()));
            } catch (JsonProcessingException e) {
                log.error("Erro ao serializar tags", e);
            }
        }
        if (request.getVideoUrl() != null) {
            project.setVideoUrl(request.getVideoUrl());
        }
        if (request.getProjectUrl() != null) {
            project.setProjectUrl(request.getProjectUrl());
        }
        if (request.getCompletedDate() != null) {
            project.setCompletedDate(request.getCompletedDate());
        }
        if (request.getClient() != null) {
            project.setClient(request.getClient());
        }
        if (request.getFeatured() != null) {
            project.setFeatured(request.getFeatured());
        }

        if (hasCategorySelection(request)) {
            CategoryCatalogService.CategorySelection categorySelection = resolveRequiredSelection(request);
            project.setCategory(categorySelection.getCategoryLabel());
            project.setServiceCategory(categorySelection.getCategory());
            project.setServiceSubcategory(categorySelection.getSubcategory());
        }

        // Atualizar imagens (remover antigas e adicionar novas)
        if (request.getImages() != null) {
            log.info("Atualizando {} imagens", request.getImages().size());

            // Coletar URLs das imagens antigas para deletar do S3
            List<String> oldImagesUrls = project.getImages().stream()
                    .map(PortfolioImage::getImageUrl)
                    .collect(Collectors.toList());

            // Limpar a coleção existente (não substituir a referência!)
            project.getImages().clear();

            // Adicionar novas imagens à mesma coleção
            final PortfolioProject finalProject = project;
            List<String> newImagesUrls = new ArrayList<>();
            for (var imgReq : request.getImages()) {
                PortfolioImage newImage = PortfolioImage.builder()
                        .project(finalProject)
                        .imageUrl(imgReq.getImageUrl())
                        .caption(imgReq.getCaption())
                        .displayOrder(imgReq.getDisplayOrder() != null ? imgReq.getDisplayOrder() : 0)
                        .build();
                project.getImages().add(newImage);
                newImagesUrls.add(imgReq.getImageUrl());
            }

            // Deletar as imagens que não estão mais na lista e são URLs do S3
            for (String oldUrl : oldImagesUrls) {
                if (!newImagesUrls.contains(oldUrl) && isS3Url(oldUrl)) {
                    try {
                        fileStorageService.deleteFile(oldUrl);
                    } catch (Exception e) {
                        log.warn("Falha ao deletar imagem antiga do S3: {}. Erro: {}", oldUrl, e.getMessage());
                    }
                }
            }
        }

        project = projectRepository.save(project);
        recordPortfolioAudit(project, beforeState);
        log.info("Projeto atualizado com sucesso: {}", project.getId());
        return mapToResponse(project);
    }

    @Transactional
    public void deleteProject(Long projectId) {
        Freelancer freelancer = freelancerService.getCurrentFreelancer();
        PortfolioProject project = projectRepository.findByIdAndFreelancerId(projectId, freelancer.getId())
                .orElseThrow(() -> new RuntimeException("Projeto não encontrado"));

        // Coletar URLs para deletar do S3
        List<String> imageUrls = project.getImages().stream()
                .map(PortfolioImage::getImageUrl)
                .collect(Collectors.toList());
        PortfolioProjectResponse deletedProject = mapToResponse(project);

        projectRepository.delete(project);

        // Deletar imagens do S3
        for (String url : imageUrls) {
            if (isS3Url(url)) {
                try {
                    fileStorageService.deleteFile(url);
                } catch (Exception e) {
                    log.warn("Falha ao deletar imagem do S3 ao excluir projeto: {}. Erro: {}", url, e.getMessage());
                }
            }
        }

        auditLogger.recordSemantic(
                "PORTFOLIO_DELETED",
                "PORTFOLIO",
                String.valueOf(projectId),
                deletedProject.getTitle(),
                com.servija.servija.model.AuditLog.Result.SUCCESS,
                null,
                deletedProject,
                null,
                null);
    }

    private boolean isS3Url(String url) {
        return url != null && url.contains(".s3.") && url.contains("amazonaws.com");
    }

    private PortfolioProjectResponse mapToResponse(PortfolioProject project) {
        List<String> tags = new ArrayList<>();
        if (project.getTags() != null && !project.getTags().isEmpty()) {
            try {
                tags = objectMapper.readValue(project.getTags(), new TypeReference<List<String>>() {});
            } catch (JsonProcessingException e) {
                log.error("Erro ao desserializar tags", e);
            }
        }

        List<PortfolioProjectResponse.ProjectImageResponse> images = project.getImages().stream()
                .map(img -> PortfolioProjectResponse.ProjectImageResponse.builder()
                        .id(img.getId())
                        .imageUrl(img.getImageUrl())
                        .caption(img.getCaption())
                        .displayOrder(img.getDisplayOrder())
                        .build())
                .collect(Collectors.toList());

        return PortfolioProjectResponse.builder()
                .id(project.getId())
                .title(project.getTitle())
                .description(project.getDescription())
                .categoryId(project.getServiceCategory() != null ? project.getServiceCategory().getId() : null)
                .categorySlug(project.getServiceCategory() != null ? project.getServiceCategory().getSlug() : null)
                .categoryLabel(project.getServiceCategory() != null ? project.getServiceCategory().getName()
                        : project.getCategory())
                .category(project.getCategory())
                .subcategoryId(
                        project.getServiceSubcategory() != null ? project.getServiceSubcategory().getId() : null)
                .subcategorySlug(
                        project.getServiceSubcategory() != null ? project.getServiceSubcategory().getSlug() : null)
                .subcategoryLabel(
                        project.getServiceSubcategory() != null ? project.getServiceSubcategory().getName() : null)
                .subcategory(
                        project.getServiceSubcategory() != null ? project.getServiceSubcategory().getName() : null)
                .tags(tags)
                .videoUrl(project.getVideoUrl())
                .projectUrl(project.getProjectUrl())
                .completedDate(project.getCompletedDate())
                .client(project.getClient())
                .featured(project.getFeatured())
                .images(images)
                .build();
    }

    private boolean hasCategorySelection(PortfolioProjectRequest request) {
        return request.getCategoryId() != null
                || request.getSubcategoryId() != null
                || StringUtils.hasText(request.getCategory())
                || StringUtils.hasText(request.getSubcategory());
    }

    private CategoryCatalogService.CategorySelection resolveRequiredSelection(PortfolioProjectRequest request) {
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

    private void recordPortfolioAudit(PortfolioProject project, Map<String, Object> beforeState) {
        Map<String, Object> afterState = buildPortfolioAuditState(project);
        List<String> changedFields = AuditStateDiffUtils.changedFields(beforeState, afterState);
        if (changedFields.isEmpty()) {
            return;
        }
        auditLogger.recordSemantic(
                "PORTFOLIO_UPDATED",
                "PORTFOLIO",
                String.valueOf(project.getId()),
                project.getTitle(),
                com.servija.servija.model.AuditLog.Result.SUCCESS,
                Map.of("fieldsChanged", changedFields),
                AuditStateDiffUtils.filterFields(beforeState, changedFields),
                AuditStateDiffUtils.filterFields(afterState, changedFields));
    }

    private Map<String, Object> buildPortfolioAuditState(PortfolioProject project) {
        PortfolioProjectResponse response = mapToResponse(project);
        Map<String, Object> state = new LinkedHashMap<>();
        state.put("title", response.getTitle());
        state.put("description", response.getDescription());
        state.put("category", response.getCategoryLabel());
        state.put("subcategory", response.getSubcategoryLabel());
        state.put("tags", response.getTags());
        state.put("videoUrl", response.getVideoUrl());
        state.put("projectUrl", response.getProjectUrl());
        state.put("completedDate", response.getCompletedDate());
        state.put("client", response.getClient());
        state.put("featured", response.getFeatured());
        state.put("images", response.getImages() == null
                ? List.of()
                : response.getImages().stream()
                .map(image -> {
                    Map<String, Object> imageState = new LinkedHashMap<>();
                    imageState.put("imageUrl", image.getImageUrl());
                    imageState.put("caption", image.getCaption());
                    imageState.put("displayOrder", image.getDisplayOrder());
                    return imageState;
                })
                .toList());
        return state;
    }
}
