package com.servija.servija.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PortfolioProjectRequest {
    
    @NotBlank(message = "Título é obrigatório")
    @Size(max = 200, message = "Título deve ter no máximo 200 caracteres")
    private String title;
    
    @Size(max = 5000, message = "Descrição deve ter no máximo 5000 caracteres")
    private String description;
    
    @Size(max = 100, message = "Categoria deve ter no máximo 100 caracteres")
    private String category;
    
    private List<String> tags;
    
    @Size(max = 500, message = "URL do vídeo deve ter no máximo 500 caracteres")
    private String videoUrl;
    
    @Size(max = 500, message = "URL do projeto deve ter no máximo 500 caracteres")
    private String projectUrl;
    
    private LocalDate completedDate;
    
    @Size(max = 200, message = "Nome do cliente deve ter no máximo 200 caracteres")
    private String client;
    
    private Boolean featured;
    
    private List<ProjectImageRequest> images;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProjectImageRequest {
        private String imageUrl;
        
        @Size(max = 255, message = "Legenda deve ter no máximo 255 caracteres")
        private String caption;
        
        private Integer displayOrder;
    }
}
