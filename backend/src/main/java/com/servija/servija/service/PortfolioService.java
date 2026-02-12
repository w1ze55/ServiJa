package com.servija.servija.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.servija.servija.dto.PortfolioProjectRequest;
import com.servija.servija.dto.PortfolioProjectResponse;
import com.servija.servija.model.Freelancer;
import com.servija.servija.model.PortfolioImage;
import com.servija.servija.model.PortfolioProject;
import com.servija.servija.repository.PortfolioImageRepository;
import com.servija.servija.repository.PortfolioProjectRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PortfolioService {

    private final PortfolioProjectRepository projectRepository;
    private final PortfolioImageRepository imageRepository;
    private final FreelancerService freelancerService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<PortfolioProjectResponse> getProjects() {
        Freelancer freelancer = freelancerService.getCurrentFreelancer();
        List<PortfolioProject> projects = projectRepository.findByFreelancerIdOrderByCreatedAtDesc(freelancer.getId());
        return projects.stream()
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

        PortfolioProject project = PortfolioProject.builder()
                .freelancer(freelancer)
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
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

        // Atualizar campos
        if (request.getTitle() != null) {
            project.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            project.setDescription(request.getDescription());
        }
        if (request.getCategory() != null) {
            project.setCategory(request.getCategory());
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

        // Atualizar imagens (remover antigas e adicionar novas)
        if (request.getImages() != null) {
            log.info("Atualizando {} imagens", request.getImages().size());
            
            // Limpar a coleção existente (não substituir a referência!)
            project.getImages().clear();
            
            // Adicionar novas imagens à mesma coleção
            final PortfolioProject finalProject = project;
            for (var imgReq : request.getImages()) {
                PortfolioImage newImage = PortfolioImage.builder()
                        .project(finalProject)
                        .imageUrl(imgReq.getImageUrl())
                        .caption(imgReq.getCaption())
                        .displayOrder(imgReq.getDisplayOrder() != null ? imgReq.getDisplayOrder() : 0)
                        .build();
                project.getImages().add(newImage);
            }
        }

        project = projectRepository.save(project);
        log.info("Projeto atualizado com sucesso: {}", project.getId());
        return mapToResponse(project);
    }

    @Transactional
    public void deleteProject(Long projectId) {
        Freelancer freelancer = freelancerService.getCurrentFreelancer();
        PortfolioProject project = projectRepository.findByIdAndFreelancerId(projectId, freelancer.getId())
                .orElseThrow(() -> new RuntimeException("Projeto não encontrado"));
        
        projectRepository.delete(project);
    }

    private PortfolioProjectResponse mapToResponse(PortfolioProject project) {
        List<String> tags = new ArrayList<>();
        if (project.getTags() != null && !project.getTags().isEmpty()) {
            try {
                tags = objectMapper.readValue(project.getTags(), List.class);
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
                .category(project.getCategory())
                .tags(tags)
                .videoUrl(project.getVideoUrl())
                .projectUrl(project.getProjectUrl())
                .completedDate(project.getCompletedDate())
                .client(project.getClient())
                .featured(project.getFeatured())
                .images(images)
                .build();
    }
}
